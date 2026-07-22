package com.example.ranksystem;

import java.util.List;

public final class AppState {
    public static final long IDLE_TIMEOUT_MILLIS = 30L * 60L * 1000L;
    public static final LoginService LOGIN_SERVICE = new LoginService();
    public static final PokerRoomService POKER_ROOM_SERVICE = new PokerRoomService(LOGIN_SERVICE);

    private AppState() {
    }

    public static String snapshotJson() {
        expireIdlePlayers();
        return "{\"type\":\"snapshot\",\"players\":"
                + playersArrayJson(LOGIN_SERVICE.getOnlinePlayers())
                + ",\"pokerTables\":"
                + pokerTablesJson(POKER_ROOM_SERVICE.tableSummaries())
                + ",\"pokerRoom\":"
                + pokerRoomJson(POKER_ROOM_SERVICE.snapshot())
                + "}";
    }

    public static String playersJson(List<PlayerSession> players) {
        return "{\"players\":" + playersArrayJson(players) + "}";
    }

    public static boolean expireIdlePlayers() {
        return !LOGIN_SERVICE.expireIdlePlayers(
                IDLE_TIMEOUT_MILLIS,
                POKER_ROOM_SERVICE::isPlayerInAnyTable
        ).isEmpty();
    }

    public static String pokerRoomJson(PokerRoomSnapshot room) {
        return pokerRoomJson(room, "");
    }

    public static String pokerRoomJson(PokerRoomSnapshot room, String viewerId) {
        String normalizedViewerId = viewerId == null ? "" : viewerId.trim();
        boolean revealAllHoleCards = room.finished() && room.communityCards().size() >= 5;
        boolean hideOtherChoices = shouldHideOtherChoices(room, normalizedViewerId);
        StringBuilder json = new StringBuilder("{\"tableId\":")
                .append(room.tableId())
                .append(",\"started\":")
                .append(room.started())
                .append(",\"canStart\":")
                .append(room.canStart())
                .append(",\"hostId\":\"")
                .append(escapeJson(room.hostId()))
                .append("\"")
                .append(",\"dealerId\":\"")
                .append(escapeJson(room.dealerId()))
                .append("\"")
                .append(",\"currentAggressorId\":\"")
                .append(escapeJson(room.currentAggressorId()))
                .append("\"")
                .append(",\"currentTurnId\":\"")
                .append(escapeJson(room.currentTurnId()))
                .append("\"")
                .append(",\"pot\":")
                .append(room.pot())
                .append(",\"currentBet\":")
                .append(room.currentBet())
                .append(",\"finished\":")
                .append(room.finished())
                .append(",\"winnerId\":\"")
                .append(escapeJson(room.winnerId()))
                .append("\"")
                .append(",\"message\":\"")
                .append(escapeJson(room.message()))
                .append("\"")
                .append(",\"aggressorOrder\":")
                .append(stringArrayJson(room.aggressorOrder()))
                .append(",\"completedAggressorIds\":")
                .append(stringArrayJson(room.completedAggressorIds()))
                .append(",\"communityCards\":")
                .append(stringArrayJson(room.communityCards()))
                .append(",\"rules\":")
                .append(stringArrayJson(room.rules()))
                .append(",\"players\":[");
        for (int i = 0; i < room.players().size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            PokerRoomPlayer player = room.players().get(i);
            PokerRoomPlayer visiblePlayer = visiblePlayer(room, player, normalizedViewerId, hideOtherChoices);
            String handName = "";
            List<String> bestCards = List.of();
            if (revealAllHoleCards) {
                handName = player.folded() ? "已弃牌" : PokerRoomService.bestHandName(player.holeCards(), room.communityCards());
                bestCards = player.folded() ? List.of() : PokerRoomService.bestHandCards(player.holeCards(), room.communityCards());
            }
            json.append("{\"id\":\"")
                    .append(escapeJson(visiblePlayer.playerId()))
                    .append("\",\"ready\":")
                    .append(visiblePlayer.ready())
                    .append(",\"folded\":")
                    .append(visiblePlayer.folded())
                    .append(",\"chipsCommitted\":")
                    .append(visiblePlayer.chipsCommitted())
                    .append(",\"roundBet\":")
                    .append(visiblePlayer.roundBet())
                    .append(",\"acted\":")
                    .append(visiblePlayer.acted())
                    .append(",\"score\":")
                    .append(visiblePlayer.score())
                    .append(",\"handName\":\"")
                    .append(escapeJson(handName))
                    .append("\"")
                    .append(",\"bestCards\":")
                    .append(stringArrayJson(bestCards))
                    .append(",\"holeCards\":")
                    .append(stringArrayJson(revealAllHoleCards || visiblePlayer.playerId().equals(normalizedViewerId) ? visiblePlayer.holeCards() : List.of()))
                    .append("}");
        }
        json.append("]}");
        return json.toString();
    }

    private static boolean shouldHideOtherChoices(PokerRoomSnapshot room, String viewerId) {
        if (room.finished() || room.currentBet() <= 0) {
            return false;
        }
        if (viewerId.isBlank()) {
            return true;
        }
        PokerRoomPlayer viewer = room.players().stream()
                .filter(player -> viewerId.equals(player.playerId()))
                .findFirst()
                .orElse(null);
        return viewer != null
                && !viewer.folded()
                && !viewer.playerId().equals(room.currentAggressorId())
                && viewer.roundBet() != room.currentBet();
    }

    private static PokerRoomPlayer visiblePlayer(PokerRoomSnapshot room, PokerRoomPlayer player, String viewerId, boolean hideOtherChoices) {
        if (!hideOtherChoices
                || player.playerId().equals(viewerId)
                || player.playerId().equals(room.currentAggressorId())) {
            return player;
        }
        return new PokerRoomPlayer(
                player.playerId(),
                player.ready(),
                player.holeCards(),
                false,
                player.chipsCommitted() - player.roundBet(),
                0,
                false,
                player.score() + player.roundBet()
        );
    }

    public static String pokerTablesJson(List<PokerTableSummary> tables) {
        StringBuilder json = new StringBuilder("{\"tables\":[");
        for (int i = 0; i < tables.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            PokerTableSummary table = tables.get(i);
            json.append("{\"id\":")
                    .append(table.tableId())
                    .append(",\"started\":")
                    .append(table.started())
                    .append(",\"finished\":")
                    .append(table.finished())
                    .append(",\"hostId\":\"")
                    .append(escapeJson(table.hostId()))
                    .append("\"")
                    .append(",\"players\":")
                    .append(stringArrayJson(table.playerIds()))
                    .append(",\"playerDetails\":")
                    .append(tablePlayerDetailsJson(table.players()))
                    .append("}");
        }
        json.append("]}");
        return json.toString();
    }

    private static String tablePlayerDetailsJson(List<PokerRoomPlayer> players) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            PokerRoomPlayer player = players.get(i);
            json.append("{\"id\":\"")
                    .append(escapeJson(player.playerId()))
                    .append("\",\"ready\":")
                    .append(player.ready())
                    .append("}");
        }
        json.append("]");
        return json.toString();
    }

    public static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String playersArrayJson(List<PlayerSession> players) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            PlayerSession player = players.get(i);
            json.append("{\"id\":\"")
                    .append(escapeJson(player.playerId()))
                    .append("\",\"status\":\"")
                    .append(escapeJson(player.status()))
                    .append("\",\"tableId\":")
                    .append(POKER_ROOM_SERVICE.tableIdForPlayer(player.playerId()))
                    .append("}");
        }
        json.append("]");
        return json.toString();
    }

    private static String stringArrayJson(List<String> values) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("\"").append(escapeJson(values.get(i))).append("\"");
        }
        json.append("]");
        return json.toString();
    }
}
