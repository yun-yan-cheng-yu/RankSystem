package com.zqyyz.ranksystem.model;

import java.util.Arrays;

public enum CardRank {
    ACE(14, "A"),
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K");

    private final int value;
    private final String label;

    CardRank(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int value() {
        return value;
    }

    public String label() {
        return label;
    }

    public static CardRank fromLabel(String label) {
        return Arrays.stream(values())
                .filter(rank -> rank.label.equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown card rank: " + label));
    }
}
