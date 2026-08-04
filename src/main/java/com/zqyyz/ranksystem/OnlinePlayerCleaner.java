package com.zqyyz.ranksystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Replaces the old scheduled executor-based cleaner.
 * Uses Spring's @Scheduled annotation instead of manual ScheduledExecutorService.
 */
@Component
public class OnlinePlayerCleaner {

    private static final Logger log = LoggerFactory.getLogger(OnlinePlayerCleaner.class);
    
    private final AppState appState;

    public OnlinePlayerCleaner(AppState appState) {
        this.appState = appState;
    }

    // Clean every 30 seconds (same as original interval)
    @Scheduled(fixedRate = 30000)
    public void cleanExpiredPlayers() {
        if (appState.expireIdlePlayers()) {
            RealtimeEndpoint.broadcastGlobalLobby();
        }
    }
}
