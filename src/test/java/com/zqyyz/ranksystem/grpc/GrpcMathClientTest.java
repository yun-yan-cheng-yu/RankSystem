package com.zqyyz.ranksystem.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrpcMathClientTest {

    @Test
    void addAndSubRoundTrip() throws Exception {
        Server server = ServerBuilder.forPort(0)
                .addService(new MathUtilGrpc.MathUtilImplBase() {
                    @Override
                    public void add(Vector2 request, StreamObserver<Num> responseObserver) {
                        responseObserver.onNext(Num.newBuilder()
                                .setNum(request.getNuma() + request.getNumb())
                                .build());
                        responseObserver.onCompleted();
                    }

                    @Override
                    public void sub(Vector2 request, StreamObserver<Num> responseObserver) {
                        responseObserver.onNext(Num.newBuilder()
                                .setNum(request.getNuma() - request.getNumb())
                                .build());
                        responseObserver.onCompleted();
                    }
                })
                .build()
                .start();

        try {
            GrpcMathClient client = new GrpcMathClient("127.0.0.1", server.getPort());
            assertEquals(8, client.add(5, 3));
            assertEquals(2, client.sub(5, 3));
            client.close();
        } finally {
            server.shutdownNow();
        }
    }
}
