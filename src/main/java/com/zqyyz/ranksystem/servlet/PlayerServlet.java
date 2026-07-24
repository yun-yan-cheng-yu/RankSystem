package com.zqyyz.ranksystem.servlet;

import com.zqyyz.ranksystem.AppState;
import com.zqyyz.ranksystem.model.PlayerSession;
import jakarta.servlet.annotation.WebServlet;

import java.util.List;

@WebServlet(urlPatterns = {
        "/players",
        "/state"
})
public class PlayerServlet extends BaseServlet {
    @Override
    protected ApiResult run(RequestContext context) {
        if (context.isGet() && "/players".equals(context.path())) {
            String game = context.parameter("game");
            List<PlayerSession> players = game == null
                    ? AppState.LOGIN_SERVICE.getOnlinePlayers()
                    : AppState.LOGIN_SERVICE.getPlayersByGame(game);
            return ApiResult.ok(AppState.playersJson(players));
        }

        if (context.isPost() && "/state".equals(context.path())) {
            validateAction(context);
            AppState.LOGIN_SERVICE.updateStatus(context.playerId(), context.parameter("state"));
            return ApiResult.ok(AppState.successJson()).withGlobalLobbyBroadcast();
        }

        return super.run(context);
    }
}
