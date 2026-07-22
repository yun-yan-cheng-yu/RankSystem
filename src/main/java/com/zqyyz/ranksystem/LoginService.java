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
    private final Map<String, Long> lastActiveAtMillis = new ConcurrentHashMap<>();

    public String login(String playerId) {
        String normalizedPlayerId = normalizePlayerId(playerId);
        String status = onlinePlayers.getOrDefault(
                normalizedPlayerId,
                new PlayerSession(normalizedPlayerId, PlayerStatus.LOBBY)
        ).status();
        String token = UUID.randomUUID().toString();
        onlinePlayers.put(normalizedPlayerId, new PlayerSession(normalizedPlayerId, status));
        loginTokens.put(normalizedPlayerId, token);
        lastActiveAtMillis.put(normalizedPlayerId, System.currentTimeMillis());
        return token;
    }

    public void logout(String playerId) {
        String normalizedPlayerId = normalizePlayerId(playerId);
        onlinePlayers.remove(normalizedPlayerId);
        loginTokens.remove(normalizedPlayerId);
        lastActiveAtMillis.remove(normalizedPlayerId);
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

    public void touch(String playerId, String token) {
        touch(playerId, token, System.currentTimeMillis());
    }

    void touch(String playerId, String token, long nowMillis) {
        validateToken(playerId, token);
        lastActiveAtMillis.put(normalizePlayerId(playerId), nowMillis);
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
            long lastActiveAt = lastActiveAtMillis.getOrDefault(playerId, nowMillis);
            if (nowMillis - lastActiveAt >= timeoutMillis) {
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
