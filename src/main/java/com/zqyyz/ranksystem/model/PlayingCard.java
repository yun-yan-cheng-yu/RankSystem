package com.zqyyz.ranksystem.model;

import java.util.Arrays;
import java.util.List;

public record PlayingCard(CardRank rank, CardSuit suit) {
    public static PlayingCard parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("card is required");
        }

        return Arrays.stream(CardSuit.values())
                .filter(suit -> text.endsWith(suit.symbol()))
                .findFirst()
                .map(suit -> new PlayingCard(
                        CardRank.fromLabel(text.substring(0, text.length() - suit.symbol().length())),
                        suit
                ))
                .orElseThrow(() -> new IllegalArgumentException("unknown card suit in: " + text));
    }

    public static List<PlayingCard> fullDeck() {
        return Arrays.stream(CardSuit.values())
                .flatMap(suit -> Arrays.stream(CardRank.values())
                        .map(rank -> new PlayingCard(rank, suit)))
                .toList();
    }

    @Override
    public String toString() {
        return rank.label() + suit.symbol();
    }
}
