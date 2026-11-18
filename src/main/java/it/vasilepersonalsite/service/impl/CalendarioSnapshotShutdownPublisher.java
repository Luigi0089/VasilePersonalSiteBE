package it.vasilepersonalsite.service.impl;

import it.vasilepersonalsite.DTO.CalendarioSnapshotDto;
import it.vasilepersonalsite.DTO.CalendarioSnapshotEsitoDto;
import it.vasilepersonalsite.config.CalendarioSnapshotProperties;
import it.vasilepersonalsite.util.CalendarioModificatoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pubblica su Google Drive lo snapshot del calendario quando App Engine spegne l'istanza.
 *
 * <h2>Perché nessuna query allo shutdown</h2>
 * Lo shutdown concede circa 2 s (SIGTERM → SIGKILL) e Neon può essere sospeso dopo 5 minuti
 * di inattività, mentre App Engine spegne l'istanza dopo circa 15: una query in quel momento
 * rischierebbe di attendere il risveglio di Neon e di sforare il tempo. Per questo lo snapshot
 * viene RICALCOLATO SUBITO dopo ogni modifica del calendario (in background, dopo il commit,
 * con il database sicuramente attivo) e tenuto IN MEMORIA. Allo shutdown resta solo l'upload.
 * Nessuna scrittura su Drive avviene durante la vita dell'istanza.
 *
 * <h2>Perché ContextClosedEvent</h2>
 * SIGTERM attiva lo shutdown hook di Spring Boot; AbstractApplicationContext.doClose()
 * pubblica ContextClosedEvent PRIMA di fermare Tomcat e PRIMA di distruggere i singleton.
 * @PreDestroy arriverebbe durante la distruzione dei bean (ordine non garantito rispetto
 * ad altri bean), e uno shutdown hook JVM separato correrebbe in parallelo a quello di Spring.
 *
 * <h2>Garanzie</h2>
 * - L'upload ha un budget unico (default 1,5 s) e gira su un thread daemon: allo scadere lo
 *   shutdown prosegue e su Drive resta lo snapshot precedente.
 * - Il JSON è serializzato interamente in memoria prima dell'invio: mai contenuti parziali.
 * - Se durante la vita dell'istanza nulla è cambiato, non si pubblica niente.
 */
@Slf4j
@Component
public class CalendarioSnapshotShutdownPublisher {

    private final CalendarioSnapshotServiceImpl snapshotService;
    private final CalendarioSnapshotProperties props;

    /** Ultima fotografia calcolata dopo una modifica, non ancora pubblicata. */
    private final AtomicReference<CalendarioSnapshotDto> daPubblicare = new AtomicReference<>();
    private final AtomicBoolean eseguito = new AtomicBoolean(false);

    public CalendarioSnapshotShutdownPublisher(CalendarioSnapshotServiceImpl snapshotService,
                                               CalendarioSnapshotProperties props) {
        this.snapshotService = snapshotService;
        this.props = props;
    }

    /**
     * Dopo il commit di una modifica: ricalcola la fotografia (query di sola lettura, in
     * background sul pool @Async esistente) e la conserva in memoria. Non tocca Google Drive.
     *
     * Le operazioni di LezioneServiceImpl salvano con save() senza transazione esterna (il
     * commit è già avvenuto quando l'evento viene pubblicato): fallbackExecution le gestisce.
     * La pulizia mensile è @Transactional: lì l'evento arriva solo dopo il commit.
     * Poiché è @Async, la query gira su un thread separato e vede sempre i dati già committati.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCalendarioModificato(CalendarioModificatoEvent evento) {
        if (!props.enabled() || !props.publishOnShutdown()) {
            return;
        }
        try {
            CalendarioSnapshotDto nuovo = snapshotService.genera();
            // con modifiche ravvicinate vince sempre la fotografia più recente
            daPubblicare.accumulateAndGet(nuovo, (vecchio, n) ->
                    vecchio == null || !n.generatedAt().isBefore(vecchio.generatedAt()) ? n : vecchio);
        } catch (Exception e) {
            // non blocca nulla: al massimo lo snapshot su Drive resterà più vecchio
            log.warn("SNAPSHOT CALENDARIO → ricalcolo dopo '{}' non riuscito: {}", evento.motivo(), e.getMessage());
        }
    }

    /** Dopo una pubblicazione manuale riuscita: la fotografia in memoria non serve più. */
    public void segnalaPubblicato(CalendarioSnapshotDto pubblicato) {
        daPubblicare.updateAndGet(c -> c != null && c.generatedAt().isAfter(pubblicato.generatedAt()) ? c : null);
    }

    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
        ApplicationContext ctx = event.getApplicationContext();
        // solo il contesto principale e una sola volta
        if (ctx.getParent() != null || !eseguito.compareAndSet(false, true)) {
            return;
        }
        if (!props.enabled() || !props.publishOnShutdown()) {
            return;
        }
        CalendarioSnapshotDto snapshot = daPubblicare.get();
        if (snapshot == null) {
            log.info("SNAPSHOT CALENDARIO → shutdown: nessuna modifica in questa istanza, nessuna pubblicazione");
            return;
        }
        if (!props.pubblicazioneConfigurata()) {
            log.warn("SNAPSHOT CALENDARIO → shutdown: configurazione Drive incompleta, nessuna pubblicazione");
            return;
        }

        Instant inizio = Instant.now();
        Instant scadenza = inizio.plusMillis(props.shutdownBudgetMs());

        ExecutorService ex = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "snapshot-shutdown");
            t.setDaemon(true); // non deve mai trattenere la JVM
            return t;
        });
        try {
            Future<CalendarioSnapshotEsitoDto> f = ex.submit(() -> snapshotService.pubblicaEntro(snapshot, scadenza));
            long attesa = Math.max(Duration.between(Instant.now(), scadenza).toMillis(), 0) + 50;
            CalendarioSnapshotEsitoDto r = f.get(attesa, TimeUnit.MILLISECONDS);
            log.info("SNAPSHOT CALENDARIO → shutdown: {} in {} ms ({})", r.esito(),
                    Duration.between(inizio, Instant.now()).toMillis(), r.messaggio());
        } catch (TimeoutException e) {
            log.warn("SNAPSHOT CALENDARIO → shutdown: budget di {} ms esaurito, resta lo snapshot precedente",
                    props.shutdownBudgetMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("SNAPSHOT CALENDARIO → shutdown interrotto, resta lo snapshot precedente");
        } catch (Exception e) {
            Throwable c = e.getCause() != null ? e.getCause() : e;
            log.warn("SNAPSHOT CALENDARIO → shutdown: pubblicazione non riuscita ({}: {}), resta lo snapshot precedente",
                    c.getClass().getSimpleName(), c.getMessage());
        } finally {
            ex.shutdownNow();
        }
    }
}
