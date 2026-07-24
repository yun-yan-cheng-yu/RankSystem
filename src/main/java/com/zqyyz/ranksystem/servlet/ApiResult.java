package com.zqyyz.ranksystem.servlet;

import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record ApiResult(int status, String body, String contentType, List<BroadcastTarget> broadcastTargets) {
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String TEXT_CONTENT_TYPE = "text/plain; charset=utf-8";

    public static ApiResult ok(String body) {
        return new ApiResult(HttpServletResponse.SC_OK, body, JSON_CONTENT_TYPE, List.of());
    }

    public static ApiResult text(String body) {
        return new ApiResult(HttpServletResponse.SC_OK, body, TEXT_CONTENT_TYPE, List.of());
    }

    public static ApiResult notFound(String body) {
        return new ApiResult(HttpServletResponse.SC_NOT_FOUND, body, JSON_CONTENT_TYPE, List.of());
    }

    public ApiResult withGlobalLobbyBroadcast() {
        return withBroadcastTargets(BroadcastTarget.globalLobby());
    }

    public ApiResult withPokerLobbyBroadcast() {
        return withBroadcastTargets(BroadcastTarget.pokerLobby());
    }

    public ApiResult withPokerTableBroadcast(int tableId) {
        return withBroadcastTargets(BroadcastTarget.pokerTable(tableId));
    }

    public ApiResult withBroadcastTargets(BroadcastTarget... targets) {
        List<BroadcastTarget> nextTargets = new ArrayList<>(broadcastTargets);
        nextTargets.addAll(Arrays.asList(targets));
        return new ApiResult(status, body, contentType, List.copyOf(nextTargets));
    }
}
