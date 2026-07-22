package com.zqyyz.ranksystem.model;

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
