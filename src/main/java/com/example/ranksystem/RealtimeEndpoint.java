package com.example.ranksystem;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws")
public class RealtimeEndpoint {
    private static final Set<Session> SESSIONS = ConcurrentHashMap.newKeySet();

    @OnOpen
    public void onOpen(Session session) {
        SESSIONS.add(session);
        session.getAsyncRemote().sendText(AppState.snapshotJson());
    }

    @OnClose
    public void onClose(Session session) {
        SESSIONS.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        SESSIONS.remove(session);
    }

    public static void broadcastSnapshot() {
        String message = AppState.snapshotJson();
        for (Session session : SESSIONS) {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(message);
            } else {
                SESSIONS.remove(session);
            }
        }
    }
}
