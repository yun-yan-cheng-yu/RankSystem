package com.zqyyz.ranksystem.servlet;

import com.zqyyz.ranksystem.AppState;
import jakarta.servlet.annotation.WebServlet;

@WebServlet(urlPatterns = {
        "/login",
        "/logout",
        "/heartbeat"
})
public class AuthServlet extends BaseServlet {
    @Override
    protected ApiResult run(RequestContext context) {
        if (context.isPost() && "/login".equals(context.path())) {
            return login(context);
        }
        if (context.isPost() && "/logout".equals(context.path())) {
            return logout(context);
        }
        if (context.isPost() && "/heartbeat".equals(context.path())) {
            return heartbeat(context);
        }
        return super.run(context);
    }

    private ApiResult login(RequestContext context) {
        String token = AppState.LOGIN_SERVICE.login(context.playerId());
        return ApiResult.ok(AppState.loginSuccessJson(token)).withGlobalLobbyBroadcast();
    }

    private ApiResult logout(RequestContext context) {
        validateSession(context);
        AppState.POKER_ROOM_SERVICE.leaveAnyTable(context.playerId());
        AppState.LOGIN_SERVICE.logout(context.playerId());
        return ApiResult.ok(AppState.successJson()).withGlobalLobbyBroadcast();
    }

    private ApiResult heartbeat(RequestContext context) {
        validateHeartbeat(context);
        return ApiResult.ok(AppState.successJson());
    }
}
