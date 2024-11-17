package dev.crashteam.charon.model;

import lombok.Getter;

@Getter
public enum Currency {
    USD("USD"),
    RUB("RUB");


    private final String title;

    Currency(String title) {
        this.title = title;
    }
}
