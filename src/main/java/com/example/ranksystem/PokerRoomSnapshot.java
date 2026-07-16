package com.example.ranksystem;

import java.util.List;

public record PokerRoomSnapshot(
        int tableId,
        List<PokerRoomPlayer> players,
        boolean started,
        boolean canStart,
        String hostId,
        String dealerId,
        String currentAggressorId,
        String currentTurnId,
        List<String> communityCards,
        int pot,
        int currentBet,
        boolean finished,
        String winnerId,
        String message,
        List<String> aggressorOrder,
        List<String> completedAggressorIds,
        List<String> rules
) {
}
