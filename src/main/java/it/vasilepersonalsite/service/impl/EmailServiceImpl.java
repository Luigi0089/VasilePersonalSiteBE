package it.vasilepersonalsite.service.impl;

import it.vasilepersonalsite.DTO.LezioneResponseDto;
import it.vasilepersonalsite.entity.PrenotazioneLezione;
import it.vasilepersonalsite.service.EmailService;
import it.vasilepersonalsite.util.SimpleAES;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    // ==========================
    //  CONFIGURAZIONE
    // ==========================

    // a chi mandare la mail di notifica admin
    @Value("${lezioni.notification.to}")
    private String notificationTo;

    // chiavi Mailjet (API HTTP)
    @Value("${mailjet.api.key}")
    private String apiKey;

    @Value("${mailjet.api.secret}")
    private String apiSecret;

    // da chi arriva (deve essere un sender/verificato su Mailjet)
    @Value("${mailjet.sender.email}")
    private String fromEmail;

    @Value("${mailjet.sender.name:Luigi}")
    private String fromName;

    @Value("${personal.domain}")
    private String adminUrl;

    @Value("${chiave.SimpleAES}")
    private String chiave;

    private final RestTemplate restTemplate = new RestTemplate();

    // ==========================
    //  URL AZIONI ADMIN
    // ==========================

    private String confermaUrl() {
        return adminUrl + "/conferma";
    }

    private String rifiutaUrl() {
        return adminUrl + "/rifiuta";
    }

    private String posticipaUrl() {
        return adminUrl + "/posticipa";
    }

    // ==========================
    //  METODI PUBBLICI
    // ==========================

    @Override
    @Async
    public void sendNotificaLezione(LezioneResponseDto lezione, String mailStudente, boolean isModifica) {

        try {
            notificaStudente(lezione, mailStudente, isModifica);
        } catch (Exception e) {
            log.error("Errore durante l'invio dell'email allo studente per la lezione {}",
                    lezione.getId(), e);
        }

        try {
            notificaAdmin(lezione, isModifica);
        } catch (Exception e) {
            log.error("Errore durante l'invio dell'email all'admin per la lezione {}",
                    lezione.getId(), e);
        }
    }

    @Override
    @Async
    public void confermaLezione(PrenotazioneLezione lezione) {

        if (lezione == null) {
            log.warn("Nessuna lezione trovata");
            return;
        }

        String mailStudente = lezione.getEmail();
        if (mailStudente == null || mailStudente.isBlank()) {
            log.warn("Nessuna email studente presente per la lezione {}", lezione.getId());
            return;
        }

        try {
            String html = """
        <html>
          <body style="font-family: Arial, sans-serif; background-color:#f5f5f5; padding:20px;">
            <div style="max-width:600px; margin:0 auto; background-color:#ffffff; border-radius:8px; padding:20px; box-shadow:0 2px 6px rgba(0,0,0,0.1);">
              <div style="text-align:center; margin-bottom:20px;">
                <img src="cid:logoImage" alt="Logo" style="max-height:60px; margin-bottom:10px;">
                <h2 style="margin:0; color:#333;">Prenotazione confermata</h2>
              </div>
              
              <p>Gentile %s,</p>
              <p>
                la informiamo che la sua richiesta è stata 
                <strong>confermata</strong>.
              </p>
              <p>
                Di seguito le riportiamo i dettagli della lezione:
              </p>
              
              <h3 style="color:#444;">Dettagli della lezione</h3>
              <ul style="list-style:none; padding:0;">
                <li><strong>Materia:</strong> %s</li>
                <li><strong>Livello:</strong> %s</li>
                <li><strong>Data:</strong> %s</li>
                <li><strong>Orario:</strong> %s - %s</li>
                <li><strong>Note:</strong> %s</li>
                <li><strong>Stato:</strong> %s</li>
                    <li>
                        <p>
                            <strong>Codice di modifica:</strong>
                            <span style="font-family: monospace;">%s</span>
                        </p>
                    </li>
              </ul>
              

              <p style="font-size:13px; color:#777;">
                La preghiamo di conservare questo codice: potrà utilizzarlo per modificare o
                annullare la lezione, secondo le modalità indicate sul sito.
              </p>
              
              <p style="margin-top:25px; font-size:13px; color:#999; text-align:center;">
                La ringrazio per la fiducia e per aver scelto di svolgere una lezione con me.
              </p>
            </div>
          </body>
        </html>
        """.formatted(
                    lezione.getNomeStudente(),
                    lezione.getMateria(),
                    lezione.getLivello(),
                    lezione.getDataLezione(),
                    lezione.getOrarioInizio(),
                    lezione.getOrarioFine(),
                    Optional.ofNullable(lezione.getNote()).orElse("Nessuna nota"),
                    lezione.getStato(),
                    SimpleAES.decripta(lezione.getCodiceModifica(), chiave)
            );

            String subject = "Conferma prenotazione lezione - " + lezione.getMateria();

            log.info("INVIO EMAIL CONFERMA LEZIONE A STUDENTE : {}", mailStudente);
            inviaEmailConLogo(mailStudente, subject, html);
            log.info("EMAIL CONFERMA LEZIONE INVIATA a {}", mailStudente);

        } catch (Exception e) {
            log.error("ERRORE INVIO EMAIL STUDENTE PER CONFERMA PRENOTAZIONE LEZIONE {}", lezione.getId(), e);
        }
    }

    @Override
    @Async
    public void annullaLezione(PrenotazioneLezione lezione) {

        if (lezione == null) {
            log.warn("annullaLezione chiamato con lezione nulla");
            return;
        }

        String mailStudente = lezione.getEmail();
        if (mailStudente == null || mailStudente.isBlank()) {
            log.warn("Nessuna email studente presente per la lezione da annullare");
            return;
        }

        try {
            String html = """
        <html>
          <body style="font-family: Arial, sans-serif; background-color:#f5f5f5; padding:20px;">
            <div style="max-width:600px; margin:0 auto; background-color:#ffffff; border-radius:8px; padding:20px; box-shadow:0 2px 6px rgba(0,0,0,0.1);">
              
              <div style="text-align:center; margin-bottom:20px;">
                <img src="cid:logoImage" alt="Logo" style="max-height:60px; margin-bottom:10px;">
                <h2 style="margin:0; color:#333;">Lezione annullata</h2>
              </div>
              
              <p>Gentile %s,</p>
              <p>
                la informiamo che la prenotazione della seguente lezione è stata
                <strong>annullata</strong>.
              </p>
              
              <h3 style="color:#444;">Dettagli della lezione annullata</h3>
              <ul style="list-style:none; padding:0;">
                <li><strong>Materia:</strong> %s</li>
                <li><strong>Livello:</strong> %s</li>
                <li><strong>Data:</strong> %s</li>
                <li><strong>Orario:</strong> %s - %s</li>
                <li><strong>Note:</strong> %s</li>
                <li><strong>Stato:</strong> %s</li>
              </ul>
              
              <p style="font-size:13px; color:#777;">
                Se desidera, potrà effettuare una nuova prenotazione direttamente dal sito,
                scegliendo una data e un orario tra quelli disponibili.
              </p>
              
              <p style="margin-top:25px; font-size:13px; color:#999; text-align:center;">
                La ringrazio per l'attenzione e resto a disposizione per eventuali chiarimenti.
              </p>
            </div>
          </body>
        </html>
        """.formatted(
                    lezione.getNomeStudente(),
                    lezione.getMateria(),
                    lezione.getLivello(),
                    lezione.getDataLezione(),
                    lezione.getOrarioInizio(),
                    lezione.getOrarioFine(),
                    Optional.ofNullable(lezione.getNote()).orElse("Nessuna nota"),
                    lezione.getStato()
            );

            String subject = "Annullamento prenotazione lezione - " + lezione.getMateria();

            log.info("INVIO EMAIL ANNULLAMENTO LEZIONE A STUDENTE : {}", mailStudente);
            inviaEmailConLogo(mailStudente, subject, html);
            log.info("EMAIL ANNULLAMENTO LEZIONE INVIATA a {}", mailStudente);

        } catch (Exception e) {
            log.error("ERRORE INVIO EMAIL STUDENTE PER ANNULLAMENTO PRENOTAZIONE LEZIONE {}", lezione.getId(), e);
        }
    }

    // ==========================
    //  METODI PRIVATI
    // ==========================

    private void notificaAdmin(LezioneResponseDto lezione, boolean isModifica) {

        try {
            String adminHtml = """
            <html>
              <body style="font-family: Arial, sans-serif; background-color:#f5f5f5; padding:20px;">
                <div style="max-width:600px; margin:0 auto; background-color:#ffffff; border-radius:8px; padding:20px; box-shadow:0 2px 6px rgba(0,0,0,0.1);">
            
                  <div style="text-align:center; margin-bottom:20px;">
                    <img src="cid:logoImage" alt="Logo" style="max-height:60px; margin-bottom:10px;">
                    <h2 style="margin:0; color:#333;">Nuova lezione prenotata</h2>
                  </div>
            
                  <p>Ciao Luigi, è stata appena %s lezione.</p>
            
                  <h3 style="color:#444;">Dettagli lezione</h3>
                  <ul style="list-style:none; padding:0;">
                    <li><strong>Studente:</strong> %s</li>
                    <li><strong>Materia:</strong> %s</li>
                    <li><strong>Livello:</strong> %s</li>
                    <li><strong>Data:</strong> %s</li>
                    <li><strong>Orario:</strong> %s - %s</li>
                    <li><strong>Note:</strong> %s</li>
                    <li><strong>Stato:</strong> %s</li>
                    <li>
                        <p>
                            <strong>Codice di modifica:</strong>
                            <span style="font-family: monospace;">%s</span>
                        </p>
                    </li>
                  </ul>
            
                  <p style="font-size:13px; color:#777;">
                    Conserva questo codice: ti servirà per modificare o annullare la lezione.
                  </p>
            
                  <div style="text-align:center; margin-top:25px;">
                    <a href="%s"
                       style="display:inline-block; padding:10px 16px; margin:4px;
                              background-color:#28a745; color:white; text-decoration:none;
                              border-radius:4px; font-size:14px;">
                      Conferma
                    </a>
                    <a href="%s"
                       style="display:inline-block; padding:10px 16px; margin:4px;
                              background-color:#dc3545; color:white; text-decoration:none;
                              border-radius:4px; font-size:14px;">
                      Annulla
                    </a>
                    <a href="%s"
                       style="display:inline-block; padding:10px 16px; margin:4px;
                              background-color:#ffc107; color:#000; text-decoration:none;
                              border-radius:4px; font-size:14px;">
                      Posticipa
                    </a>
                  </div>
            
                </div>
              </body>
            </html>
            """.formatted(
                    isModifica ? "modificata una" : " prenotata una nuova",
                    lezione.getNomeStudente(),
                    lezione.getMateria(),
                    lezione.getLivello(),
                    lezione.getDataLezione(),
                    lezione.getOrarioInizio(),
                    lezione.getOrarioFine(),
                    Optional.ofNullable(lezione.getNote()).orElse("Nessuna nota"),
                    lezione.getStato(),
                    lezione.getCodiceModifica(),
                    confermaUrl() + "?id=" + lezione.getId(),
                    rifiutaUrl() + "?id=" + lezione.getId(),
                    posticipaUrl() + "?id=" + lezione.getId()
            );

            String subject = "Nuova lezione prenotata - " + lezione.getMateria();

            log.info("INVIO EMAIL (ADMIN) : {}", notificationTo);
            inviaEmailConLogo(notificationTo, subject, adminHtml);
            log.info("EMAIL ADMIN INVIATA a {}", notificationTo);

        } catch (Exception e) {
            log.error("ERRORE INVIO EMAIL ADMIN PER PRENOTAZIONE LEZIONE", e);
        }
    }

    private void notificaStudente(LezioneResponseDto lezione, String mailStudente, boolean isModifica) {

        try {
            if (mailStudente != null && !mailStudente.isBlank()) {

                String studentHtml = """
            <html>
              <body style="font-family: Arial, sans-serif; background-color:#f5f5f5; padding:20px;">
                <div style="max-width:600px; margin:0 auto; background-color:#ffffff; border-radius:8px; padding:20px; box-shadow:0 2px 6px rgba(0,0,0,0.1);">
                  
                  <div style="text-align:center; margin-bottom:20px;">
                    <img src="cid:logoImage" alt="Logo" style="max-height:60px; margin-bottom:10px;">
                    <h2 style="margin:0; color:#333;">Richiesta di %s ricevuta</h2>
                  </div>
                  
                  <p>Gentile %s,</p>
                  <p>
                    %sLa sua richiesta è stata registrata
                    correttamente nel sistema.
                  </p>
                  <p>
                    Di seguito trova un riepilogo dei dati inseriti:
                  </p>
                  
                  <h3 style="color:#444;">Dettagli della lezione</h3>
                  <ul style="list-style:none; padding:0;">
                    <li><strong>Materia:</strong> %s</li>
                    <li><strong>Livello:</strong> %s</li>
                    <li><strong>Data:</strong> %s</li>
                    <li><strong>Orario:</strong> %s - %s</li>
                    <li><strong>Note:</strong> %s</li>
                    <li><strong>Stato:</strong> %s</li>
                    <li>
                        <p>
                            <strong>Codice di modifica:</strong>
                            <span style="font-family: monospace;">%s</span>
                        </p>
                    </li>
                  </ul>
                  

                  <p style="font-size:13px; color:#777;">
                    La preghiamo di conservare questo codice: sarà necessario per modificare o annullare
                    la lezione, secondo le modalità indicate sul sito.
                  </p>

                  <p style="font-size:13px; color:#777; margin-top:15px;">
                    Riceverà una successiva email di conferma nel momento in cui la prenotazione verrà approvata.
                  </p>
                  
                  <p style="margin-top:25px; font-size:13px; color:#999; text-align:center;">
                    Grazie per la fiducia e per aver scelto di prenotare una lezione.
                  </p>
                </div>
              </body>
            </html>
            """.formatted(
                        isModifica ? "modifica" : "prenotazione",
                        lezione.getNomeStudente(),
                        isModifica ? "" : "la ringrazio per aver prenotato una lezione. ",
                        lezione.getMateria(),
                        lezione.getLivello(),
                        lezione.getDataLezione(),
                        lezione.getOrarioInizio(),
                        lezione.getOrarioFine(),
                        Optional.ofNullable(lezione.getNote()).orElse("Nessuna nota"),
                        lezione.getStato(),
                        lezione.getCodiceModifica()
                );

                String subject = "Richiesta di prenotazione lezione ricevuta - " + lezione.getMateria();

                log.info("INVIO EMAIL STUDENTE : {}", mailStudente);
                inviaEmailConLogo(mailStudente, subject, studentHtml);
                log.info("EMAIL STUDENTE INVIATA a {}", mailStudente);

            } else {
                log.warn("Nessuna email studente fornita, inviata solo email admin.");
            }
        } catch (Exception e) {
            log.error("ERRORE INVIO EMAIL STUDENTE PER PRENOTAZIONE LEZIONE", e);
        }
    }

    // ==========================
    //  INVIO VIA MAILJET API
    // ==========================

    private void inviaEmailConLogo(String to, String subject, String htmlContent) {
        try {
            // Carico il logo da src/main/resources/static/Logo.png
            ClassPathResource logo = new ClassPathResource("static/Logo.png");
            byte[] logoBytes = logo.getInputStream().readAllBytes();
            String base64Logo = Base64.getEncoder().encodeToString(logoBytes);

            String url = "https://api.mailjet.com/v3.1/send";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(apiKey, apiSecret); // Mailjet Basic Auth (API key / secret)

            Map<String, Object> from = Map.of(
                    "Email", fromEmail,
                    "Name", fromName
            );

            Map<String, String> toMap = Map.of(
                    "Email", to,
                    "Name", to
            );

            Map<String, Object> inlineAttachment = new HashMap<>();
            inlineAttachment.put("ContentType", "image/png");
            inlineAttachment.put("Filename", "Logo.png");
            inlineAttachment.put("ContentID", "logoImage");      // deve combaciare con cid:logoImage
            inlineAttachment.put("Base64Content", base64Logo);

            Map<String, Object> message = new HashMap<>();
            message.put("From", from);
            message.put("To", List.of(toMap));
            message.put("Subject", subject);
            message.put("HTMLPart", htmlContent);
            message.put("InlinedAttachments", List.of(inlineAttachment));

            Map<String, Object> body = Map.of(
                    "Messages", List.of(message)
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            log.info("Mailjet response per {}: status={}, body={}",
                    to, response.getStatusCodeValue(), response.getBody());

        } catch (Exception e) {
            log.error("ERRORE INVIO EMAIL tramite Mailjet verso {}", to, e);
        }
    }

}
