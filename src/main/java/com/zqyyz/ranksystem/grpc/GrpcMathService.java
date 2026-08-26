package com.zqyyz.ranksystem.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * Handwritten Java implementation of the MathUtil service (server side),
 * mirroring the Go implementation in internal/service/mathutil.go.
 */
public class GrpcMathService extends MathUtilGrpc.MathUtilImplBase {

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

    @Override
    public void multiply(Vector2 request, StreamObserver<Num> responseObserver) {
        responseObserver.onNext(Num.newBuilder()
                .setNum(request.getNuma() * request.getNumb())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void dividedBy(Vector2 request, StreamObserver<Num> responseObserver) {
        if (request.getNumb() == 0) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("division by zero")
                    .asRuntimeException());
            return;
        }
        responseObserver.onNext(Num.newBuilder()
                .setNum(request.getNuma() / request.getNumb())
                .build());
        responseObserver.onCompleted();
    }
}
