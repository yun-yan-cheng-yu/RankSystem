package com.zqyyz.ranksystem;

import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@ServerEndpoint("/ws")
public class RealtimeEndpoint {
    private static final long PING_INTERVAL_SECONDS = 30L;
    private static final int MAX_MISSED_PONGS = 3;
    private static final ByteBuffer PING_PAYLOAD = ByteBuffer.wrap("ping".getBytes(StandardCharsets.UTF_8));
    private static final Set<Session> SESSIONS = ConcurrentHashMap.newKeySet();
    private static final Map<Session, Integer> MISSED_PONGS = new ConcurrentHashMap<>();
    private static final Map<Session, String> SESSION_PLAYERS = new ConcurrentHashMap<>();
    private static final Map<String, Session> PLAYER_SESSIONS = new ConcurrentHashMap<>();
    private static ScheduledExecutorService heartbeatExecutor;
    private static ScheduledFuture<?> heartbeatTask;

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
        session.getAsyncRemote().sendText(AppState.snapshotJson());
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
        removeSession(session);
    }

    public static void broadcastSnapshot() {
        String message = AppState.snapshotJson();
        for (Session session : SESSIONS) {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(message);
            } else {
                removeSession(session);
            }
        }
    }

    private static void pingOpenSessions() {
        for (Session session : SESSIONS) {
            if (!session.isOpen()) {
                removeSession(session);
                continue;
            }

            int missedPongs = MISSED_PONGS.merge(session, 1, Integer::sum);
            if (missedPongs > MAX_MISSED_PONGS) {
                closeSession(session, "websocket heartbeat timeout");
                removeSession(session);
                continue;
            }

            try {
                session.getBasicRemote().sendPing(PING_PAYLOAD.asReadOnlyBuffer());
            } catch (IOException | RuntimeException exception) {
                closeSession(session, "websocket heartbeat failed");
                removeSession(session);
            }
        }
    }

    private static String authenticatedPlayerId(Session session) {
        String playerId = parameter(session, "id");
        String token = token(session);
        try {
            AppState.LOGIN_SERVICE.validateToken(playerId, token);
            return AppState.LOGIN_SERVICE.normalizePlayerId(playerId);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return "";
        }
    }

    private static String token(Session session) {
        return parameter(session, "token");
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
                session.close(new CloseReason(
                        CloseReason.CloseCodes.GOING_AWAY,
                        reason
                ));
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
}
