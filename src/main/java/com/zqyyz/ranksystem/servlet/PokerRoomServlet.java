package com.zqyyz.ranksystem.servlet;

import com.zqyyz.ranksystem.AppState;
import com.zqyyz.ranksystem.RealtimeEndpoint;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

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
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        expireIdlePlayers();
        try {
            if ("/poker-room".equals(request.getServletPath())) {
                validateSession(request);
                writeJson(response, 200, AppState.pokerRoomJson(
                        AppState.POKER_ROOM_SERVICE.snapshot(tableId(request)),
                        request.getParameter("id")
                ));
                return;
            }
            writeJson(response, 404, AppState.errorJson("not found"));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            handleException(response, exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        expireIdlePlayers();
        try {
            switch (request.getServletPath()) {
                case "/poker-room/join" -> {
                    validateSession(request, true);
                    AppState.POKER_ROOM_SERVICE.join(request.getParameter("id"), tableId(request));
                    writeSuccessAndBroadcast(response);
                }
                case "/poker-room/ready" -> {
                    validateSession(request, true);
                    AppState.POKER_ROOM_SERVICE.ready(request.getParameter("id"), tableId(request));
                    writeSuccessAndBroadcast(response);
                }
                case "/poker-room/unready" -> {
                    validateSession(request, true);
                    AppState.POKER_ROOM_SERVICE.unready(request.getParameter("id"), tableId(request));
                    writeSuccessAndBroadcast(response);
                }
                case "/poker-room/start" -> {
                    validateSession(request, true);
                    AppState.POKER_ROOM_SERVICE.start(request.getParameter("id"), tableId(request));
                    writeSuccessAndBroadcast(response);
                }
                case "/poker-room/next" -> {
                    validateSession(request, true);
                    AppState.POKER_ROOM_SERVICE.nextHand(request.getParameter("id"), tableId(request));
                    writeSuccessAndBroadcast(response);
                }
                case "/poker-room/leave" -> {
                    validateSession(request, true);
                    AppState.POKER_ROOM_SERVICE.leave(request.getParameter("id"), tableId(request));
                    writeSuccessAndBroadcast(response);
                }
                case "/poker-room/fold" -> {
                    validateSession(request, true);
                    AppState.POKER_ROOM_SERVICE.fold(request.getParameter("id"), tableId(request));
                    writeSuccessAndBroadcast(response);
                }
                case "/poker-room/bet" -> {
                    validateSession(request, true);
                    AppState.POKER_ROOM_SERVICE.bet(
                            request.getParameter("id"),
                            Integer.parseInt(request.getParameter("chips")),
                            tableId(request)
                    );
                    writeSuccessAndBroadcast(response);
                }
                default -> writeJson(response, 404, AppState.errorJson("not found"));
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            handleException(response, exception);
        }
    }

    private void writeSuccessAndBroadcast(HttpServletResponse response) throws IOException {
        writeJson(response, 200, AppState.successJson());
        RealtimeEndpoint.broadcastSnapshot();
    }
}
