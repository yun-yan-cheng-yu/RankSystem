package com.zqyyz.ranksystem.grpc;

import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thin blocking gRPC client for the Go {@code MathUtil} server.
 */
public class GrpcMathClient implements AutoCloseable {

    private static final long DEADLINE_SECONDS = 5L;

    private final ManagedChannel channel;
    private final MathUtilGrpc.MathUtilBlockingStub stub;
    private final MathUtilGrpc.MathUtilFutureStub futureStub;
    private final MathStreamUtilGrpc.MathStreamUtilStub streamStub;

    public GrpcMathClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = MathUtilGrpc.newBlockingStub(channel);
        this.futureStub = MathUtilGrpc.newFutureStub(channel);
        this.streamStub = MathStreamUtilGrpc.newStub(channel);
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

    /**
     * Asynchronous version of add: returns a ListenableFuture immediately,
     * the caller thread is not blocked.
     */
    public ListenableFuture<Num> addAsync(int a, int b) {
        return futureStub.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .add(Vector2.newBuilder().setNuma(a).setNumb(b).build());
    }

    public List<String> chat(List<String> messages) throws Exception {
        List<String> transcript = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        var requestObserver = streamStub
                .withDeadlineAfter(30, TimeUnit.SECONDS)
                .chat(new StreamObserver<>() {
                    @Override
                    public void onNext(Message message) {
                        transcript.add("server: " + message.getText());
                    }

                    @Override
                    public void onError(Throwable t) {
                        failure.set(t);
                        done.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("on complete");
                        done.countDown();
                    }
                });

        // 发送方向：独立线程，按自己的节奏发，不等回复
        Thread sender = new Thread(() -> {
            try {
                for (String m : messages) {
                    transcript.add("client: " + m);
                    requestObserver.onNext(Message.newBuilder().setText(m).build());
                    Thread.sleep(500);
                }
                requestObserver.onCompleted();
            } catch (Exception e) {
                requestObserver.onError(e);
            }
        });
        sender.start();

        if (!done.await(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("chat timeout");
        }
        if (failure.get() != null) {
            throw new IllegalStateException("chat failed", failure.get());
        }
        return transcript;
    }

    public int upload(List<Integer> nums) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Num> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        var requestObserver = streamStub
                .withDeadlineAfter(10, TimeUnit.SECONDS)
                .upload(new StreamObserver<>() {
                    @Override
                    public void onNext(Num num) {
                        result.set(num); // 客户端流只会收到一次响应
                    }

                    @Override
                    public void onError(Throwable t) {
                        failure.set(t);
                        done.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        done.countDown();
                    }
                });

        for (Integer n : nums) {
            requestObserver.onNext(Num.newBuilder().setNum(n).build());
        }
        // 客户端流必须半关闭，服务端才会 SendAndClose 回结果
        requestObserver.onCompleted();

        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("upload timeout");
        }
        if (failure.get() != null) {
            throw new IllegalStateException("upload failed", failure.get());
        }
        if (result.get() == null) {
            throw new IllegalStateException("upload got no response");
        }
        return result.get().getNum();
    }

    /**
     * Server-streaming demo: asks for {@code count} numbers, the server streams
     * back 1..count, collected as they arrive.
     */
    public List<Integer> download(int count) throws Exception {
        List<Integer> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        streamStub.withDeadlineAfter(10, TimeUnit.SECONDS)
                .download(Num.newBuilder().setNum(count).build(), new StreamObserver<>() {
                    @Override
                    public void onNext(Num num) {
                        results.add(num.getNum());
                    }

                    @Override
                    public void onError(Throwable t) {
                        failure.set(t);
                        done.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        done.countDown();
                    }
                });

        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("download timeout");
        }
        if (failure.get() != null) {
            throw new IllegalStateException("download failed", failure.get());
        }
        return results;
    }

    @Override
    public void close() {
        channel.shutdown();
    }
}
