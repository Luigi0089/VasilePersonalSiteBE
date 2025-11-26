package it.vasilepersonalsite.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.text.Normalizer;
import java.util.Set;

public class NoProfanityValidator implements ConstraintValidator<NoProfanity, String> {

    private static final Set<String> BANNED_WORDS = Set.of(
            "porco",
            "suca",
            "gay",
            "frocio",
            "froci",
            "ricchione",
            "ricchioni",
            "ritardato",
            "pompini",
            "pompino",
            "sesso",
            "cazzo",
            "minchia",
            "ammazzati",
            "culo",
            "tette",
            "pene",
            "puttana",
            "buttana",
            "zoccola",
            "bracialet",
            "troia",
            "pulla",
            "bastarda",
            "impiccati",
            "droga",
            "finocchio",
            "ciolla",
            "ciolle",
            "minchione",
            "indegno",
            "coglione"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // se è null o vuoto, lascia passare: ci penseranno @NotBlank / @NotNull
        if (value == null || value.isBlank()) {
            return true;
        }

        // normalizza: minuscole + rimozione accenti
        String normalized = normalize(value);

        // controllo molto semplice: "contiene" una parolaccia come substring
        // (tu qui puoi raffinare la logica come preferisci)
        for (String bad : BANNED_WORDS) {
            if (normalized.contains(bad)) {
                return false;
            }
        }

        return true;
    }

    private String normalize(String input) {
        String lower = input.toLowerCase();
        String noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");   // rimuove i segni diacritici
        return noAccents;
    }
}
