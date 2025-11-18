package it.vasilepersonalsite.constans;

public enum Stato {

    CONFERMATA("Confermata"),
    IN_APPROVAZIONE("In aprrovazione"),
    ANNULLATA("Annullata");


    private final String label;

    Stato(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
