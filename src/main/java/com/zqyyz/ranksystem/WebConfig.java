package com.zqyyz.ranksystem;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * Replaces the old web.xml configuration.
 * Registers the JSR-356 WebSocket endpoint. Static resources (index.html and
 * friends) are served from classpath:/static/ by Spring Boot's default handler.
 */
@Configuration
public class WebConfig {

    /**
     * Registers the JSR-356 @ServerEndpoint("/ws") with the embedded Tomcat
     * WebSocket container.
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
