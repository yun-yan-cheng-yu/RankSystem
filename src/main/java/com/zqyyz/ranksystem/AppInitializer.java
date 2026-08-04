package com.zqyyz.ranksystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Replaces the old AppLifecycleListener (@WebListener).
 * Starts the WebSocket heartbeat on application startup. The idle player
 * cleaner is executed automatically by @Scheduled.
 */
@Component
public class AppInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AppInitializer.class);

    @Override
    public void run(String... args) {
        log.info("RankSystem starting up...");
        RealtimeEndpoint.startHeartbeat();
        log.info("RankSystem started.");
    }
}
