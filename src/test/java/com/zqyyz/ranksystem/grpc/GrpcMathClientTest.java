package com.zqyyz.ranksystem.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test
    void chatBidiFullDuplex() throws Exception {
        Server server = ServerBuilder.forPort(0)
                .addService(new MathStreamUtilGrpc.MathStreamUtilImplBase() {
                    @Override
                    public StreamObserver<Message> chat(StreamObserver<Message> responseObserver) {
                        return new StreamObserver<Message>() {
                            @Override
                            public void onNext(Message request) {
                                // 延迟回复，让客户端继续发与回复同时在路上
                                try {
                                    Thread.sleep(600);
                                } catch (InterruptedException ignored) {
                                }
                                responseObserver.onNext(Message.newBuilder()
                                        .setText("[echo] " + request.getText())
                                        .build());
                            }

                            @Override
                            public void onError(Throwable t) {
                                responseObserver.onError(t);
                            }

                            @Override
                            public void onCompleted() {
                                responseObserver.onCompleted();
                            }
                        };
                    }
                })
                .build()
                .start();

        try {
            GrpcMathClient client = new GrpcMathClient("127.0.0.1", server.getPort());
            List<String> transcript = client.chat(List.of("a", "b", "c"));
            assertEquals(6, transcript.size());
            assertEquals(3, transcript.stream().filter(s -> s.startsWith("client:")).count());
            assertEquals(3, transcript.stream().filter(s -> s.startsWith("server:")).count());
            client.close();
        } finally {
            server.shutdownNow();
        }
    }

    @Test
    void uploadClientStream() throws Exception {
        Server server = ServerBuilder.forPort(0)
                .addService(new MathStreamUtilGrpc.MathStreamUtilImplBase() {
                    @Override
                    public StreamObserver<Num> upload(StreamObserver<Num> responseObserver) {
                        return new StreamObserver<Num>() {
                            private int count;

                            @Override
                            public void onNext(Num request) {
                                count++;
                            }

                            @Override
                            public void onError(Throwable t) {
                                responseObserver.onError(t);
                            }

                            @Override
                            public void onCompleted() {
                                // 客户端流收完后，用 SendAndClose 一次性回结果
                                responseObserver.onNext(Num.newBuilder().setNum(count).build());
                                responseObserver.onCompleted();
                            }
                        };
                    }
                })
                .build()
                .start();

        try {
            GrpcMathClient client = new GrpcMathClient("127.0.0.1", server.getPort());
            assertEquals(3, client.upload(List.of(5, 7, 9)));
            client.close();
        } finally {
            server.shutdownNow();
        }
    }

    @Test
    void downloadServerStream() throws Exception {
        Server server = ServerBuilder.forPort(0)
                .addService(new MathStreamUtilGrpc.MathStreamUtilImplBase() {
                    @Override
                    public void download(Num request, StreamObserver<Num> responseObserver) {
                        for (int i = 1; i <= request.getNum(); i++) {
                            responseObserver.onNext(Num.newBuilder().setNum(i).build());
                        }
                        responseObserver.onCompleted();
                    }
                })
                .build()
                .start();

        try {
            GrpcMathClient client = new GrpcMathClient("127.0.0.1", server.getPort());
            assertEquals(List.of(1, 2, 3, 4, 5), client.download(5));
            client.close();
        } finally {
            server.shutdownNow();
        }
    }
}
