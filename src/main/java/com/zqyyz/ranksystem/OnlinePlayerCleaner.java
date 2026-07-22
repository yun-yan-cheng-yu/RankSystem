package com.zqyyz.ranksystem;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class OnlinePlayerCleaner {
    private static final long CLEAN_INTERVAL_SECONDS = 30L;
    private static ScheduledExecutorService executor;
    private static ScheduledFuture<?> task;

    private OnlinePlayerCleaner() {
    }

    public static synchronized void start() {
        if (task != null && !task.isCancelled()) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ranksystem-online-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        task = executor.scheduleAtFixedRate(
                OnlinePlayerCleaner::cleanExpiredPlayers,
                CLEAN_INTERVAL_SECONDS,
                CLEAN_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    public static synchronized void stop() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private static void cleanExpiredPlayers() {
        if (AppState.expireIdlePlayers()) {
            RealtimeEndpoint.broadcastSnapshot();
        }
    }
}
