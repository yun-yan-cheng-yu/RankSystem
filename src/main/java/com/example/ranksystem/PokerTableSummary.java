package com.example.ranksystem;

import java.util.List;

public record PokerTableSummary(int tableId, List<String> playerIds, boolean started, boolean finished) {
}
