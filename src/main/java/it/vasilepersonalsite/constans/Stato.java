package it.vasilepersonalsite.constans;

import lombok.Getter;

@Getter
public enum Stato {

    CONFERMATA("Confermata"),
    IN_APPROVAZIONE("In approvazione"),
    ANNULLATA("Annullata");


    private final String label;

    Stato(String label) {
        this.label = label;
    }


}
