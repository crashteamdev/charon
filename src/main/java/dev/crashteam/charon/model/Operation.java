package dev.crashteam.charon.model;

public enum Operation {

    DEPOSIT_BALANCE("DEPOSIT_BALANCE"),
    GENERIC_PURCHASE("GENERIC_PURCHASE"),
    PURCHASE_SERVICE("PURCHASE_SERVICE");

    private final String title;

    Operation(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
