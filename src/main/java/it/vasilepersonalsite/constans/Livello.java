package it.vasilepersonalsite.constans;

public enum Livello {

    ELEMENTARE("Elementare"),
    MEDIE("Medie"),
    SUPERIORI("Superiori"),
    UNIVERSITA("Università");

    private final String label;

    Livello(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

