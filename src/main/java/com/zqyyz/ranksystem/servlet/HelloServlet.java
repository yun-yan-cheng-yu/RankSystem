package com.zqyyz.ranksystem.servlet;

import jakarta.servlet.annotation.WebServlet;

@WebServlet("/hello")
public class HelloServlet extends BaseServlet {
    @Override
    protected ApiResult run(RequestContext context) {
        return ApiResult.text("Hello world");
    }
}
