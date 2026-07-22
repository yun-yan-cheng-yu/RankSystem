package com.zqyyz.ranksystem.servlet;

import com.zqyyz.ranksystem.AppState;
import com.zqyyz.ranksystem.RealtimeEndpoint;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class BaseServlet extends HttpServlet {
    protected void writeJson(HttpServletResponse response, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        response.setStatus(status);
        response.setContentType("application/json; charset=utf-8");
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    protected int tableId(HttpServletRequest request) {
        String value = request.getParameter("table");
        if (value == null || value.isBlank()) {
            return 1;
        }
        return Integer.parseInt(value);
    }

    protected void validateSession(HttpServletRequest request) {
        AppState.LOGIN_SERVICE.validateToken(request.getParameter("id"), request.getParameter("token"));
    }

    protected void validateSession(HttpServletRequest request, boolean touch) {
        if (touch) {
            AppState.LOGIN_SERVICE.touch(request.getParameter("id"), request.getParameter("token"));
        } else {
            validateSession(request);
        }
    }

    protected void handleException(HttpServletResponse response, RuntimeException exception) throws IOException {
        if (exception instanceof IllegalArgumentException) {
            writeJson(response, 400, AppState.errorJson(exception.getMessage()));
            return;
        }
        if (exception instanceof IllegalStateException) {
            int status = isSessionError((IllegalStateException) exception) ? 401 : 409;
            writeJson(response, status, AppState.errorJson(exception.getMessage()));
            return;
        }
        throw exception;
    }

    protected void expireIdlePlayers() {
        if (AppState.expireIdlePlayers()) {
            RealtimeEndpoint.broadcastSnapshot();
        }
    }

    private boolean isSessionError(IllegalStateException exception) {
        return "session expired".equals(exception.getMessage())
                || "login replaced".equals(exception.getMessage());
    }
}
