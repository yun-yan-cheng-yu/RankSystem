package com.zqyyz.ranksystem.model;

import java.util.Arrays;

public enum CardSuit {
    SPADE("♠"),
    HEART("♥"),
    DIAMOND("♦"),
    CLUB("♣");

    private final String symbol;

    CardSuit(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    public static CardSuit fromSymbol(String symbol) {
        return Arrays.stream(values())
                .filter(suit -> suit.symbol.equals(symbol))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown card suit: " + symbol));
    }
}
