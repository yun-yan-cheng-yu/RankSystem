package com.zqyyz.ranksystem;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppLifecycleListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        RealtimeEndpoint.startHeartbeat();
        OnlinePlayerCleaner.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        OnlinePlayerCleaner.stop();
        RealtimeEndpoint.stopHeartbeat();
    }
}
