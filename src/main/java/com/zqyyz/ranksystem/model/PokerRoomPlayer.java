package com.zqyyz.ranksystem.model;

import java.util.List;

public record PokerRoomPlayer(
        String playerId,
        boolean ready,
        List<String> holeCards,
        boolean folded,
        int chipsCommitted,
        int roundBet,
        boolean acted,
        int score
) {
    public PokerRoomPlayer(String playerId, boolean ready) {
        this(playerId, ready, List.of(), false, 0, 0, false, 0);
    }

    public PokerRoomPlayer(String playerId, boolean ready, List<String> holeCards, boolean folded, int chipsCommitted) {
        this(playerId, ready, holeCards, folded, chipsCommitted, 0, false, 0);
    }

    public PokerRoomPlayer(
            String playerId,
            boolean ready,
            List<String> holeCards,
            boolean folded,
            int chipsCommitted,
            int roundBet,
            boolean acted
    ) {
        this(playerId, ready, holeCards, folded, chipsCommitted, roundBet, acted, 0);
    }
}
