package com.zqyyz.ranksystem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqyyz.ranksystem.model.PlayerSession;
import com.zqyyz.ranksystem.model.PokerRoomPlayer;
import com.zqyyz.ranksystem.model.PokerRoomSnapshot;
import com.zqyyz.ranksystem.model.PokerTableSummary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AppState {

    public static final long IDLE_TIMEOUT_MILLIS = 5L * 60L * 1000L;

    private final ObjectMapper json = new ObjectMapper();
    private final LoginService loginService;
    private final PokerRoomService pokerRoomService;
    private static final ObjectMapper STATIC_JSON = new ObjectMapper();

    public AppState(LoginService loginService, PokerRoomService pokerRoomService) {
        this.loginService = loginService;
        this.pokerRoomService = pokerRoomService;
    }

    // ---- Instance methods used by controllers -------------------------------

    public String snapshotJson() {
        expireIdlePlayers();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("type", "snapshot");
        snapshot.put("players", playersView(loginService.getOnlinePlayers()));
        snapshot.put("pokerTables", Map.of("tables", pokerTablesView(pokerRoomService.tableSummaries())));
        snapshot.put("pokerRoom", pokerRoomView(pokerRoomService.snapshot(), ""));
        return toJsonInternal(snapshot);
    }

    public String playersJson(List<PlayerSession> players) {
        return toJsonInternal(Map.of("players", playersView(players)));
    }

    public String successJson() {
        return toJsonInternal(Map.of("success", true));
    }

    public String loginSuccessJson(String token) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("token", token);
        return toJsonInternal(response);
    }

    public String errorJson(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("message", message);
        return toJsonInternal(response);
    }

    public boolean expireIdlePlayers() {
        return !loginService.expireIdlePlayers(
                IDLE_TIMEOUT_MILLIS,
                pokerRoomService::isPlayerInAnyTable
        ).isEmpty();
    }

    public static String pokerRoomJson(PokerRoomSnapshot room) {
        return pokerRoomJson(room, "");
    }

    public static String pokerRoomJson(PokerRoomSnapshot room, String viewerId) {
        return toJsonStatic(pokerRoomView(room, viewerId));
    }

    public String pokerTablesJson(List<PokerTableSummary> tables) {
        return toJsonInternal(Map.of("tables", pokerTablesView(tables)));
    }

    // ---- Internal JSON serialization ----------------------------------------

    private String toJsonInternal(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize json", exception);
        }
    }

    private static String toJsonStatic(Object value) {
        try {
            return STATIC_JSON.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize json", exception);
        }
    }

    // ---- View builders ------------------------------------------------------

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

    private List<Map<String, Object>> pokerTablesView(List<PokerTableSummary> tables) {
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

    private List<Map<String, Object>> tablePlayerDetailsView(List<PokerRoomPlayer> players) {
        List<Map<String, Object>> playerViews = new ArrayList<>();
        for (PokerRoomPlayer player : players) {
            Map<String, Object> playerView = new LinkedHashMap<>();
            playerView.put("id", player.playerId());
            playerView.put("ready", player.ready());
            playerViews.add(playerView);
        }
        return playerViews;
    }

    private List<Map<String, Object>> playersView(List<PlayerSession> players) {
        List<Map<String, Object>> playerViews = new ArrayList<>();
        for (PlayerSession player : players) {
            Map<String, Object> playerView = new LinkedHashMap<>();
            playerView.put("id", player.playerId());
            playerView.put("status", player.status());
            playerView.put("tableId", pokerRoomService.tableIdForPlayer(player.playerId()));
            playerViews.add(playerView);
        }
        return playerViews;
    }

    // ---- Static accessors for backward compatibility -------------------------

    /** Get the LoginService instance from Spring context. */
    public static LoginService loginService() {
        return SpringContextHolder.getBean(LoginService.class);
    }

    /** Get the PokerRoomService instance from Spring context. */
    public static PokerRoomService pokerRoomService() {
        return SpringContextHolder.getBean(PokerRoomService.class);
    }

    /** Get the AppState instance from Spring context. */
    public static AppState instance() {
        return SpringContextHolder.getBean(AppState.class);
    }

    /** Static JSON serialization for use by non-Spring classes (e.g., RealtimeEndpoint). */
    public static String json(Object value) {
        return instance().toJsonInternal(value);
    }
}
