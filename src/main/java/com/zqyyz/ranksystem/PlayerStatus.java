package com.zqyyz.ranksystem;

public final class PlayerStatus {
    public static final String LOBBY = "在总大厅";
    public static final String GAME_A_ROOM = "在德州扑克房间";
    public static final String GAME_A_PLAYING = "在德州扑克中";
    public static final String GAME_B_ROOM = "在游戏B房间";
    public static final String GAME_B_PLAYING = "在游戏B中";

    private PlayerStatus() {
    }

    public static boolean isGameAStatus(String status) {
        return GAME_A_ROOM.equals(status) || GAME_A_PLAYING.equals(status);
    }
}
