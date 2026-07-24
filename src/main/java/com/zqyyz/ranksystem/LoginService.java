package com.zqyyz.ranksystem;

import com.zqyyz.ranksystem.model.PlayerSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class LoginService {
    private final Map<String, PlayerSession> onlinePlayers = new ConcurrentHashMap<>();
    private final Map<String, String> loginTokens = new ConcurrentHashMap<>();
    private final Map<String, Long> lastHeartbeatAtMillis = new ConcurrentHashMap<>();
    private final Map<String, Long> lastActionAtMillis = new ConcurrentHashMap<>();

    public String login(String playerId) {
        String normalizedPlayerId = normalizePlayerId(playerId);
        String status = onlinePlayers.getOrDefault(
                normalizedPlayerId,
                new PlayerSession(normalizedPlayerId, PlayerStatus.LOBBY)
        ).status();
        String token = UUID.randomUUID().toString();
        onlinePlayers.put(normalizedPlayerId, new PlayerSession(normalizedPlayerId, status));
        loginTokens.put(normalizedPlayerId, token);
        long nowMillis = System.currentTimeMillis();
        lastHeartbeatAtMillis.put(normalizedPlayerId, nowMillis);
        lastActionAtMillis.put(normalizedPlayerId, nowMillis);
        return token;
    }

    public void logout(String playerId) {
        String normalizedPlayerId = normalizePlayerId(playerId);
        onlinePlayers.remove(normalizedPlayerId);
        loginTokens.remove(normalizedPlayerId);
        lastHeartbeatAtMillis.remove(normalizedPlayerId);
        lastActionAtMillis.remove(normalizedPlayerId);
    }

    public void validateToken(String playerId, String token) {
        String normalizedPlayerId = normalizePlayerId(playerId);
        String activeToken = loginTokens.get(normalizedPlayerId);
        if (activeToken == null || token == null || token.isBlank()) {
            throw new IllegalStateException("session expired");
        }
        if (!activeToken.equals(token)) {
            throw new IllegalStateException("login replaced");
        }
    }

    public boolean isValidToken(String playerId, String token) {
        try {
            validateToken(playerId, token);
            return true;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return false;
        }
    }

    public void heartbeat(String playerId, String token) {
        heartbeat(playerId, token, System.currentTimeMillis());
    }

    void heartbeat(String playerId, String token, long nowMillis) {
        validateToken(playerId, token);
        lastHeartbeatAtMillis.put(normalizePlayerId(playerId), nowMillis);
    }

    public void markAction(String playerId, String token) {
        markAction(playerId, token, System.currentTimeMillis());
    }

    void markAction(String playerId, String token, long nowMillis) {
        validateToken(playerId, token);
        String normalizedPlayerId = normalizePlayerId(playerId);
        lastActionAtMillis.put(normalizedPlayerId, nowMillis);
    }

    Long lastHeartbeatAtMillis(String playerId) {
        return lastHeartbeatAtMillis.get(normalizePlayerId(playerId));
    }

    Long lastActionAtMillis(String playerId) {
        return lastActionAtMillis.get(normalizePlayerId(playerId));
    }

    public List<String> expireIdlePlayers(long timeoutMillis, Predicate<String> exemptPlayer) {
        return expireIdlePlayers(System.currentTimeMillis(), timeoutMillis, exemptPlayer);
    }

    List<String> expireIdlePlayers(long nowMillis, long timeoutMillis, Predicate<String> exemptPlayer) {
        List<String> expiredPlayerIds = new ArrayList<>();
        for (String playerId : onlinePlayers.keySet()) {
            if (exemptPlayer.test(playerId)) {
                continue;
            }
            long lastActionAt = lastActionAtMillis.getOrDefault(playerId, nowMillis);
            if (nowMillis - lastActionAt >= timeoutMillis) {
                logout(playerId);
                expiredPlayerIds.add(playerId);
            }
        }
        return expiredPlayerIds;
    }

    public void updateStatus(String playerId, String status) {
        String normalizedPlayerId = normalizePlayerId(playerId);
        String normalizedStatus = normalizeStatus(status);
        onlinePlayers.computeIfPresent(
                normalizedPlayerId,
                (id, session) -> new PlayerSession(id, normalizedStatus)
        );
    }

    public List<PlayerSession> getOnlinePlayers() {
        List<PlayerSession> players = new ArrayList<>(onlinePlayers.values());
        players.sort((left, right) -> left.playerId().compareTo(right.playerId()));
        return players;
    }

    public String statusOf(String playerId) {
        PlayerSession session = onlinePlayers.get(normalizePlayerId(playerId));
        return session == null ? "" : session.status();
    }

    public List<PlayerSession> getPlayersByGame(String game) {
        if (!"A".equalsIgnoreCase(game)) {
            return List.of();
        }

        return getOnlinePlayers().stream()
                .filter(player -> PlayerStatus.isGameAStatus(player.status()))
                .toList();
    }

    public String normalizePlayerId(String playerId) {
        if (playerId == null || playerId.trim().isEmpty()) {
            throw new IllegalArgumentException("player id is required");
        }

        return playerId.trim();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("status is required");
        }

        return status.trim();
    }
}
