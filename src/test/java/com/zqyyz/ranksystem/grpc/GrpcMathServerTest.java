package com.zqyyz.ranksystem.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrpcMathServerTest {

    @Test
    void javaServerImplServesUnaryCalls() throws Exception {
        Server server = ServerBuilder.forPort(0)
                .addService(new GrpcMathService())
                .build()
                .start();

        try {
            GrpcMathClient client = new GrpcMathClient("127.0.0.1", server.getPort());
            assertEquals(8, client.add(6, 2));
            assertEquals(4, client.sub(6, 2));
            assertEquals(12, client.multiply(6, 2));
            assertEquals(3, client.dividedby(6, 2));
            client.close();
        } finally {
            server.shutdownNow();
        }
    }
}
