package com.example.ranksystem;

import java.util.List;

public record PokerTableSummary(
        int tableId,
        List<String> playerIds,
        String hostId,
        List<PokerRoomPlayer> players,
        boolean started,
        boolean finished
) {
}
