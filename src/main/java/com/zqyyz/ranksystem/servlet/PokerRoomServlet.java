package com.zqyyz.ranksystem.servlet;

import com.zqyyz.ranksystem.AppState;
import jakarta.servlet.annotation.WebServlet;

@WebServlet(urlPatterns = {
        "/poker-room",
        "/poker-room/join",
        "/poker-room/ready",
        "/poker-room/unready",
        "/poker-room/start",
        "/poker-room/next",
        "/poker-room/leave",
        "/poker-room/fold",
        "/poker-room/bet"
})
public class PokerRoomServlet extends BaseServlet {
    @Override
    protected ApiResult run(RequestContext context) {
        if (context.isGet() && "/poker-room".equals(context.path())) {
            validateSession(context);
            return ApiResult.ok(AppState.pokerRoomJson(
                    AppState.POKER_ROOM_SERVICE.snapshot(context.tableId()),
                    context.playerId()
            ));
        }

        if (!context.isPost()) {
            return super.run(context);
        }

        return switch (context.path()) {
            case "/poker-room/join" -> {
                validateAction(context);
                AppState.POKER_ROOM_SERVICE.join(context.playerId(), context.tableId());
                yield successAndBroadcastPokerLobby();
            }
            case "/poker-room/ready" -> {
                validateAction(context);
                AppState.POKER_ROOM_SERVICE.ready(context.playerId(), context.tableId());
                yield successAndBroadcastPokerLobby();
            }
            case "/poker-room/unready" -> {
                validateAction(context);
                AppState.POKER_ROOM_SERVICE.unready(context.playerId(), context.tableId());
                yield successAndBroadcastPokerLobby();
            }
            case "/poker-room/start" -> {
                validateAction(context);
                AppState.POKER_ROOM_SERVICE.start(context.playerId(), context.tableId());
                yield successAndBroadcastPokerLobby();
            }
            case "/poker-room/next" -> {
                validateAction(context);
                AppState.POKER_ROOM_SERVICE.nextHand(context.playerId(), context.tableId());
                yield successAndBroadcastPokerLobby();
            }
            case "/poker-room/leave" -> {
                validateAction(context);
                AppState.POKER_ROOM_SERVICE.leave(context.playerId(), context.tableId());
                yield successAndBroadcastPokerLobby();
            }
            case "/poker-room/fold" -> {
                validateAction(context);
                AppState.POKER_ROOM_SERVICE.fold(context.playerId(), context.tableId());
                yield successAndBroadcastPokerTable(context.tableId());
            }
            case "/poker-room/bet" -> {
                validateAction(context);
                AppState.POKER_ROOM_SERVICE.bet(
                        context.playerId(),
                        context.intParameter("chips"),
                        context.tableId()
                );
                yield successAndBroadcastPokerTable(context.tableId());
            }
            default -> super.run(context);
        };
    }

    private ApiResult successAndBroadcastPokerLobby() {
        return ApiResult.ok(AppState.successJson()).withPokerLobbyBroadcast();
    }

    private ApiResult successAndBroadcastPokerTable(int tableId) {
        return ApiResult.ok(AppState.successJson()).withPokerTableBroadcast(tableId);
    }
}
