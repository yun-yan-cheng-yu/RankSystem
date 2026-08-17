package com.zqyyz.ranksystem.grpc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    @Bean(destroyMethod = "close")
    public GrpcMathClient grpcMathClient(
            @Value("${grpc.server.host:127.0.0.1}") String host,
            @Value("${grpc.server.port:9100}") int port) {
        return new GrpcMathClient(host, port);
    }
}
