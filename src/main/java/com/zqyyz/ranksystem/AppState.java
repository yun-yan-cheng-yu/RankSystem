package com.zqyyz.ranksystem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqyyz.ranksystem.model.PlayerSession;
import com.zqyyz.ranksystem.model.PokerRoomPlayer;
import com.zqyyz.ranksystem.model.PokerRoomSnapshot;
import com.zqyyz.ranksystem.model.PokerTableSummary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AppState {
    public static final long IDLE_TIMEOUT_MILLIS = 5L * 60L * 1000L;
    public static final LoginService LOGIN_SERVICE = new LoginService();
    public static final PokerRoomService POKER_ROOM_SERVICE = new PokerRoomService(LOGIN_SERVICE);
    private static final ObjectMapper JSON = new ObjectMapper();

    private AppState() {
    }

    public static String snapshotJson() {
        expireIdlePlayers();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("type", "snapshot");
        snapshot.put("players", playersView(LOGIN_SERVICE.getOnlinePlayers()));
        snapshot.put("pokerTables", Map.of("tables", pokerTablesView(POKER_ROOM_SERVICE.tableSummaries())));
        snapshot.put("pokerRoom", pokerRoomView(POKER_ROOM_SERVICE.snapshot(), ""));
        return json(snapshot);
    }

    public static String playersJson(List<PlayerSession> players) {
        return json(Map.of("players", playersView(players)));
    }

    public static String successJson() {
        return json(Map.of("success", true));
    }

    public static String loginSuccessJson(String token) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("token", token);
        return json(response);
    }

    public static String errorJson(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("message", message);
        return json(response);
    }

    public static String json(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize json", exception);
        }
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
        return json(pokerRoomView(room, viewerId));
    }

    private static Map<String, Object> pokerRoomView(PokerRoomSnapshot room, String viewerId) {
        String normalizedViewerId = viewerId == null ? "" : viewerId.trim();
        boolean revealAllHoleCards = room.finished() && room.communityCards().size() >= 5;
        boolean hideOtherChoices = shouldHideOtherChoices(room, normalizedViewerId);

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("tableId", room.tableId());
        view.put("started", room.started());
        view.put("canStart", room.canStart());
        view.put("hostId", room.hostId());
        view.put("dealerId", room.dealerId());
        view.put("currentAggressorId", room.currentAggressorId());
        view.put("currentTurnId", room.currentTurnId());
        view.put("pot", room.pot());
        view.put("currentBet", room.currentBet());
        view.put("finished", room.finished());
        view.put("winnerId", room.winnerId());
        view.put("message", room.message());
        view.put("aggressorOrder", room.aggressorOrder());
        view.put("completedAggressorIds", room.completedAggressorIds());
        view.put("communityCards", room.communityCards());
        view.put("rules", room.rules());

        List<Map<String, Object>> players = new ArrayList<>();
        for (PokerRoomPlayer player : room.players()) {
            PokerRoomPlayer visiblePlayer = visiblePlayer(room, player, normalizedViewerId, hideOtherChoices);
            String handName = "";
            List<String> bestCards = List.of();
            if (revealAllHoleCards) {
                handName = player.folded() ? "已弃牌" : PokerRoomService.bestHandName(player.holeCards(), room.communityCards());
                bestCards = player.folded() ? List.of() : PokerRoomService.bestHandCards(player.holeCards(), room.communityCards());
            }

            Map<String, Object> playerView = new LinkedHashMap<>();
            playerView.put("id", visiblePlayer.playerId());
            playerView.put("ready", visiblePlayer.ready());
            playerView.put("folded", visiblePlayer.folded());
            playerView.put("chipsCommitted", visiblePlayer.chipsCommitted());
            playerView.put("roundBet", visiblePlayer.roundBet());
            playerView.put("acted", visiblePlayer.acted());
            playerView.put("score", visiblePlayer.score());
            playerView.put("handName", handName);
            playerView.put("bestCards", bestCards);
            playerView.put("holeCards", revealAllHoleCards || visiblePlayer.playerId().equals(normalizedViewerId)
                    ? visiblePlayer.holeCards()
                    : List.of());
            players.add(playerView);
        }
        view.put("players", players);
        return view;
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
        return json(Map.of("tables", pokerTablesView(tables)));
    }

    private static List<Map<String, Object>> pokerTablesView(List<PokerTableSummary> tables) {
        List<Map<String, Object>> tableViews = new ArrayList<>();
        for (PokerTableSummary table : tables) {
            Map<String, Object> tableView = new LinkedHashMap<>();
            tableView.put("id", table.tableId());
            tableView.put("started", table.started());
            tableView.put("finished", table.finished());
            tableView.put("hostId", table.hostId());
            tableView.put("players", table.playerIds());
            tableView.put("playerDetails", tablePlayerDetailsView(table.players()));
            tableViews.add(tableView);
        }
        return tableViews;
    }

    private static List<Map<String, Object>> tablePlayerDetailsView(List<PokerRoomPlayer> players) {
        List<Map<String, Object>> playerViews = new ArrayList<>();
        for (PokerRoomPlayer player : players) {
            Map<String, Object> playerView = new LinkedHashMap<>();
            playerView.put("id", player.playerId());
            playerView.put("ready", player.ready());
            playerViews.add(playerView);
        }
        return playerViews;
    }

    private static List<Map<String, Object>> playersView(List<PlayerSession> players) {
        List<Map<String, Object>> playerViews = new ArrayList<>();
        for (PlayerSession player : players) {
            Map<String, Object> playerView = new LinkedHashMap<>();
            playerView.put("id", player.playerId());
            playerView.put("status", player.status());
            playerView.put("tableId", POKER_ROOM_SERVICE.tableIdForPlayer(player.playerId()));
            playerViews.add(playerView);
        }
        return playerViews;
    }
}
