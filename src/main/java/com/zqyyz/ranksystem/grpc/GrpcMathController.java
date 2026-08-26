package com.zqyyz.ranksystem.grpc;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * HTTP facade over the gRPC MathUtil calls, mainly for demo/testing convenience.
 */
@RestController
@RequestMapping("/grpc/math")
public class GrpcMathController {

    private final GrpcMathClient client;

    public GrpcMathController(GrpcMathClient client) {
        this.client = client;
    }

    @GetMapping("/add")
    public int add(@RequestParam("a") int a, @RequestParam("b") int b) {
        return client.add(a, b);
    }

    @GetMapping("/sub")
    public int sub(@RequestParam("a") int a, @RequestParam("b") int b) {
        return client.sub(a, b);
    }

    @GetMapping("/mul")
    public int mul(@RequestParam("a") int a, @RequestParam("b") int b) {
        return client.multiply(a, b);
    }

    @GetMapping("/dividedby")
    public int divided(@RequestParam("a") int a, @RequestParam("b") int b) {
        return client.dividedby(a, b);
    }

    /**
     * Async demo: returns immediately (Servlet thread is released), the HTTP
     * response completes when the gRPC future finishes. DeferredResult is used
     * because Spring MVC does not reliably treat a raw ListenableFuture return
     * value as async in @RestController.
     */
    @GetMapping("/add-async")
    public DeferredResult<Integer> addAsync(@RequestParam("a") int a, @RequestParam("b") int b) {
        DeferredResult<Integer> deferredResult = new DeferredResult<>(10_000L); // 超时 10 秒
        Futures.addCallback(client.addAsync(a, b), new FutureCallback<>() {
            @Override
            public void onSuccess(Num num) {
                deferredResult.setResult(num.getNum());
            }

            @Override
            public void onFailure(Throwable t) {
                deferredResult.setErrorResult(t);
            }
        }, MoreExecutors.directExecutor());
        return deferredResult;
    }

    /**
     * Full-duplex chat demo: client sends messages at its own pace while the
     * server replies with a delay, so sends and replies interleave.
     */
    @GetMapping("/chat")
    public List<String> chat(@RequestParam("messages") String messages) throws Exception {
        List<String> list = Arrays.stream(messages.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return client.chat(list);
    }

    /**
     * Client-streaming demo: nums=5,7,9 -> server counts the messages and
     * replies with that count via SendAndClose.
     */
    @GetMapping("/upload")
    public int upload(@RequestParam("nums") String nums) throws Exception {
        List<Integer> values = Arrays.stream(nums.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .toList();
        return client.upload(values);
    }

    /**
     * Server-streaming demo: count=5 -> the server streams back [1,2,3,4,5].
     */
    @GetMapping("/download")
    public List<Integer> download(@RequestParam("count") int count) throws Exception {
        return client.download(count);
    }
}
