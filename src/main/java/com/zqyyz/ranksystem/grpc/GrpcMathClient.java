package com.zqyyz.ranksystem.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Thin blocking gRPC client for the Go {@code MathUtil} server.
 */
public class GrpcMathClient implements AutoCloseable {

    private static final long DEADLINE_SECONDS = 5L;

    private final ManagedChannel channel;
    private final MathUtilGrpc.MathUtilBlockingStub stub;

    public GrpcMathClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = MathUtilGrpc.newBlockingStub(channel);
    }

    public int add(int a, int b) {
        return stub.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .add(Vector2.newBuilder().setNuma(a).setNumb(b).build())
                .getNum();
    }

    public int sub(int a, int b) {
        return stub.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .sub(Vector2.newBuilder().setNuma(a).setNumb(b).build())
                .getNum();
    }

    public int multiply(int a, int b) {
        return stub.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .multiply(Vector2.newBuilder().setNuma(a).setNumb(b).build())
                .getNum();
    }

    public int dividedby(int a, int b) {
        return stub.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .dividedBy(Vector2.newBuilder().setNuma(a).setNumb(b).build())
                .getNum();
    }

    @Override
    public void close() {
        channel.shutdown();
    }
}
