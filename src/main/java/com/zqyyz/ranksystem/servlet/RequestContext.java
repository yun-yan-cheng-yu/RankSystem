package com.zqyyz.ranksystem.servlet;

import jakarta.servlet.http.HttpServletRequest;

public record RequestContext(
        String path,
        String method,
        String playerId,
        String token,
        HttpServletRequest request
) {
    public static RequestContext from(HttpServletRequest request) {
        return new RequestContext(
                request.getServletPath(),
                request.getMethod(),
                request.getParameter("id"),
                request.getParameter("token"),
                request
        );
    }

    public boolean isGet() {
        return "GET".equalsIgnoreCase(method);
    }

    public boolean isPost() {
        return "POST".equalsIgnoreCase(method);
    }

    public String parameter(String name) {
        return request.getParameter(name);
    }

    public int intParameter(String name) {
        return Integer.parseInt(parameter(name));
    }

    public int tableId() {
        String value = parameter("table");
        if (value == null || value.isBlank()) {
            return 1;
        }
        return Integer.parseInt(value);
    }
}
