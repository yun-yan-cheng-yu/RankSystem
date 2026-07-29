package com.zqyyz.ranksystem.model;

public enum HandCategory {
    HIGH_CARD(0, "高牌"),
    ONE_PAIR(1, "一对"),
    TWO_PAIR(2, "两对"),
    THREE_OF_KIND(3, "三条"),
    STRAIGHT(4, "顺子"),
    FLUSH(5, "同花"),
    FULL_HOUSE(6, "葫芦"),
    FOUR_OF_KIND(7, "四条"),
    STRAIGHT_FLUSH(8, "同花顺"),
    ROYAL_FLUSH(9, "皇家同花顺");

    private final int strength;
    private final String displayName;

    HandCategory(int strength, String displayName) {
        this.strength = strength;
        this.displayName = displayName;
    }

    public int strength() {
        return strength;
    }

    public String displayName() {
        return displayName;
    }
}
