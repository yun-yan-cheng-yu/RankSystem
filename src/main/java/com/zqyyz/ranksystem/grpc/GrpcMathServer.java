package com.zqyyz.ranksystem.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Starts an in-process gRPC server (Java implementation of MathUtil) so the Go
 * client can call a Java server, the reverse of the normal Go-server setup.
 */
@Component
public class GrpcMathServer {

    private static final Logger log = LoggerFactory.getLogger(GrpcMathServer.class);

    private final Server server;

    public GrpcMathServer(@Value("${grpc.server.listen-port:9101}") int port) {
        this.server = ServerBuilder.forPort(port)
                .addService(new GrpcMathService())
                .build();
    }

    @PostConstruct
    public void start() throws Exception {
        server.start();
        log.info("Java gRPC MathUtil server listening on {}", server.getPort());
    }

    @PreDestroy
    public void stop() {
        server.shutdown();
    }
}
