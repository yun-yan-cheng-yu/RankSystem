package com.example.ranksystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoginService {
    private final Map<String, PlayerSession> onlinePlayers = new ConcurrentHashMap<>();

    public void login(String playerId) {
        String normalizedPlayerId = normalizePlayerId(playerId);
        onlinePlayers.put(normalizedPlayerId, new PlayerSession(normalizedPlayerId, PlayerStatus.LOBBY));
    }

    public void logout(String playerId) {
        String normalizedPlayerId = normalizePlayerId(playerId);
        onlinePlayers.remove(normalizedPlayerId);
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
