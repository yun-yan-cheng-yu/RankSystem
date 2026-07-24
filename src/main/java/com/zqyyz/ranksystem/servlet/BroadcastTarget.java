package com.zqyyz.ranksystem.servlet;

public record BroadcastTarget(BroadcastScope scope, int tableId) {
    public static BroadcastTarget globalLobby() {
        return new BroadcastTarget(BroadcastScope.GLOBAL_LOBBY, 0);
    }

    public static BroadcastTarget pokerLobby() {
        return new BroadcastTarget(BroadcastScope.POKER_LOBBY, 0);
    }

    public static BroadcastTarget pokerTable(int tableId) {
        return new BroadcastTarget(BroadcastScope.POKER_TABLE, tableId);
    }
}
