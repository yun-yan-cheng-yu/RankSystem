package com.zqyyz.ranksystem.grpc;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
