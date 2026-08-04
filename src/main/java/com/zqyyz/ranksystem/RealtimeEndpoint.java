package com.zqyyz.ranksystem;

import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * JSR-356 WebSocket endpoint for realtime broadcasts.
 *
 * <p>The Jakarta WebSocket container instantiates this class once per connection,
 * so all shared state (open sessions, heartbeat task) is kept in static fields.
 * REST controllers and the scheduled idle cleaner trigger pushes through the
 * static broadcast methods below.
 */
@Component
@ServerEndpoint("/ws")
public class RealtimeEndpoint {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEndpoint.class);
    private static final long PING_INTERVAL_SECONDS = 30L;
    private static final int MAX_MISSED_PONGS = 3;
    private static final ByteBuffer PING_PAYLOAD = ByteBuffer.wrap("ping".getBytes(StandardCharsets.UTF_8));

    private static final Set<Session> SESSIONS = ConcurrentHashMap.newKeySet();
    private static final Map<Session, Integer> MISSED_PONGS = new ConcurrentHashMap<>();
    private static final Map<Session, String> SESSION_PLAYERS = new ConcurrentHashMap<>();
    private static final Map<String, Session> PLAYER_SESSIONS = new ConcurrentHashMap<>();
    private static ScheduledExecutorService heartbeatExecutor;
    private static ScheduledFuture<?> heartbeatTask;

    // ---- Heartbeat lifecycle ------------------------------------------------

    public static synchronized void startHeartbeat() {
        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            return;
        }
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "ranksystem-ws-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        heartbeatTask = heartbeatExecutor.scheduleAtFixedRate(
                RealtimeEndpoint::pingOpenSessions,
                PING_INTERVAL_SECONDS,
                PING_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    public static synchronized void stopHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
            heartbeatTask = null;
        }
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
            heartbeatExecutor = null;
        }
        for (Session session : SESSIONS) {
            closeSession(session, "application stopped");
            removeSession(session);
        }
    }

    // ---- WebSocket handlers -------------------------------------------------

    @OnOpen
    public void onOpen(Session session) {
        startHeartbeat();
        String playerId = authenticatedPlayerId(session);
        if (playerId.isEmpty()) {
            closeSession(session, "websocket auth failed");
            return;
        }

        Session oldSession = PLAYER_SESSIONS.put(playerId, session);
        if (oldSession != null && oldSession != session) {
            closeSession(oldSession, "login replaced");
            removeSession(oldSession);
        }

        SESSIONS.add(session);
        MISSED_PONGS.put(session, 0);
        SESSION_PLAYERS.put(session, playerId);

        try {
            session.getAsyncRemote().sendText(AppState.instance().snapshotJson());
        } catch (RuntimeException e) {
            log.warn("Failed to send initial snapshot to {}", session.getId(), e);
        }
    }

    @OnMessage
    public void onPong(PongMessage message, Session session) {
        MISSED_PONGS.put(session, 0);
    }

    @OnClose
    public void onClose(Session session) {
        removeSession(session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket error for session {}", session.getId(), error);
        removeSession(session);
    }

    // ---- Broadcasts (called by REST controllers and scheduled cleaner) -------

    public static void broadcastGlobalLobby() {
        broadcastToMatching(AppState.instance().snapshotJson(), playerId -> true);
    }

    public static void broadcastPokerLobby() {
        broadcastToMatching(
                AppState.instance().snapshotJson(),
                playerId -> PlayerStatus.isGameAStatus(AppState.loginService().statusOf(playerId))
        );
    }

    public static void broadcastPokerTable(int tableId) {
        broadcastToMatching(
                AppState.instance().snapshotJson(),
                playerId -> AppState.pokerRoomService().tableIdForPlayer(playerId) == tableId
        );
    }

    // ---- Private helpers ----------------------------------------------------

    private static void pingOpenSessions() {
        for (Session session : SESSIONS) {
            if (!session.isOpen()) {
                removeSession(session);
                continue;
            }

            int missed = MISSED_PONGS.merge(session, 1, Integer::sum);
            if (missed > MAX_MISSED_PONGS) {
                closeSession(session, "websocket heartbeat timeout");
                removeSession(session);
                continue;
            }

            try {
                session.getBasicRemote().sendPing(PING_PAYLOAD.asReadOnlyBuffer());
            } catch (IOException | RuntimeException e) {
                closeSession(session, "websocket heartbeat failed");
                removeSession(session);
            }
        }
    }

    private static void broadcastToMatching(String message, PlayerMatcher matcher) {
        for (Session session : SESSIONS) {
            if (!session.isOpen()) {
                removeSession(session);
                continue;
            }
            String playerId = SESSION_PLAYERS.get(session);
            if (playerId != null && matcher.matches(playerId)) {
                try {
                    session.getAsyncRemote().sendText(message);
                } catch (RuntimeException e) {
                    log.warn("Failed to send message to {}", session.getId(), e);
                }
            }
        }
    }

    private static String authenticatedPlayerId(Session session) {
        String playerId = parameter(session, "id");
        String token = parameter(session, "token");
        try {
            AppState.loginService().validateToken(playerId, token);
            return AppState.loginService().normalizePlayerId(playerId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return "";
        }
    }

    private static String parameter(Session session, String name) {
        List<String> values = session.getRequestParameterMap().get(name);
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.get(0);
    }

    private static void closeSession(Session session, String reason) {
        try {
            if (session != null && session.isOpen()) {
                session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, reason));
            }
        } catch (IOException ignored) {
            // The connection is already unusable; removal from local state is enough.
        }
    }

    private static void removeSession(Session session) {
        if (session == null) {
            return;
        }
        SESSIONS.remove(session);
        MISSED_PONGS.remove(session);
        String playerId = SESSION_PLAYERS.remove(session);
        if (playerId != null) {
            PLAYER_SESSIONS.remove(playerId, session);
        }
    }

    @FunctionalInterface
    private interface PlayerMatcher {
        boolean matches(String playerId);
    }
}
