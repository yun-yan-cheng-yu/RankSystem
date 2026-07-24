package com.zqyyz.ranksystem.servlet;

import com.zqyyz.ranksystem.AppState;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/poker-tables")
public class PokerTableServlet extends BaseServlet {
    @Override
    protected ApiResult run(RequestContext context) {
        if (context.isGet()) {
            return ApiResult.ok(AppState.pokerTablesJson(AppState.POKER_ROOM_SERVICE.tableSummaries()));
        }
        return super.run(context);
    }
}
