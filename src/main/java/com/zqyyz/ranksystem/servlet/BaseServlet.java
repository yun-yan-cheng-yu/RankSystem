package com.zqyyz.ranksystem.servlet;

import com.zqyyz.ranksystem.AppState;
import com.zqyyz.ranksystem.RealtimeEndpoint;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class BaseServlet extends HttpServlet {


    @Override
    protected final void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        dispatch(request, response);
    }

    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        dispatch(request, response);
    }

    private void dispatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        expireIdlePlayers();
        try {
            writeResult(response, run(RequestContext.from(request)));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            handleException(response, exception);
        }
    }

    protected ApiResult run(RequestContext context) {
        return ApiResult.notFound(AppState.errorJson("not found"));
    }

    protected void writeResult(HttpServletResponse response, ApiResult result) throws IOException {
        writeBody(response, result.status(), result.contentType(), result.body());
        for (BroadcastTarget target : result.broadcastTargets()) {
            broadcast(target);
        }
    }

    private void broadcast(BroadcastTarget target) {
        switch (target.scope()) {
            case GLOBAL_LOBBY -> RealtimeEndpoint.broadcastGlobalLobby();
            case POKER_LOBBY -> RealtimeEndpoint.broadcastPokerLobby();
            case POKER_TABLE -> RealtimeEndpoint.broadcastPokerTable(target.tableId());
        }
    }

    private void writeBody(HttpServletResponse response, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        response.setStatus(status);
        response.setContentType(contentType);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    protected void writeJson(HttpServletResponse response, int status, String body) throws IOException {
        writeBody(response, status, "application/json; charset=utf-8", body);
    }

    protected void validateSession(RequestContext context) {
        AppState.LOGIN_SERVICE.validateToken(context.playerId(), context.token());
    }

    protected void validateHeartbeat(RequestContext context) {
        AppState.LOGIN_SERVICE.heartbeat(context.playerId(), context.token());
    }

    protected void validateAction(RequestContext context) {
        AppState.LOGIN_SERVICE.markAction(context.playerId(), context.token());
    }

    protected void handleException(HttpServletResponse response, RuntimeException exception) throws IOException {
        if (exception instanceof IllegalArgumentException) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, AppState.errorJson(exception.getMessage()));
            return;
        }
        if (exception instanceof IllegalStateException) {
            int status = isSessionError((IllegalStateException) exception)
                    ? HttpServletResponse.SC_UNAUTHORIZED
                    : HttpServletResponse.SC_CONFLICT;
            writeJson(response, status, AppState.errorJson(exception.getMessage()));
            return;
        }
        throw exception;
    }

    protected void expireIdlePlayers() {
        if (AppState.expireIdlePlayers()) {
            RealtimeEndpoint.broadcastGlobalLobby();
        }
    }

    private boolean isSessionError(IllegalStateException exception) {
        return "session expired".equals(exception.getMessage())
                || "login replaced".equals(exception.getMessage());
    }
}
