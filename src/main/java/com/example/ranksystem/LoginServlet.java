package com.example.ranksystem;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@WebServlet(urlPatterns = {
        "/login",
        "/logout",
        "/state",
        "/players",
        "/poker-tables",
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
public class LoginServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if ("/players".equals(path)) {
            String game = request.getParameter("game");
            List<PlayerSession> players = game == null
                    ? AppState.LOGIN_SERVICE.getOnlinePlayers()
                    : AppState.LOGIN_SERVICE.getPlayersByGame(game);
            writeJson(response, 200, AppState.playersJson(players));
            return;
        }

        if ("/poker-tables".equals(path)) {
            writeJson(response, 200, AppState.pokerTablesJson(AppState.POKER_ROOM_SERVICE.tableSummaries()));
            return;
        }

        if ("/poker-room".equals(path)) {
            writeJson(response, 200, AppState.pokerRoomJson(
                    AppState.POKER_ROOM_SERVICE.snapshot(tableId(request)),
                    request.getParameter("id")
            ));
            return;
        }

        writeJson(response, 404, "{\"success\":false,\"message\":\"not found\"}");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        try {
            if ("/login".equals(path)) {
                AppState.LOGIN_SERVICE.login(request.getParameter("id"));
                writeJson(response, 200, "{\"success\":true}");
                RealtimeEndpoint.broadcastSnapshot();
                return;
            }

            if ("/logout".equals(path)) {
                String playerId = request.getParameter("id");
                AppState.POKER_ROOM_SERVICE.leaveAnyTable(playerId);
                AppState.LOGIN_SERVICE.logout(playerId);
                writeJson(response, 200, "{\"success\":true}");
                RealtimeEndpoint.broadcastSnapshot();
                return;
            }

            if ("/state".equals(path)) {
                AppState.LOGIN_SERVICE.updateStatus(request.getParameter("id"), request.getParameter("state"));
                writeJson(response, 200, "{\"success\":true}");
                RealtimeEndpoint.broadcastSnapshot();
                return;
            }

            if ("/poker-room/join".equals(path)) {
                AppState.POKER_ROOM_SERVICE.join(request.getParameter("id"), tableId(request));
                writeJson(response, 200, "{\"success\":true}");
                RealtimeEndpoint.broadcastSnapshot();
                return;
            }

            if ("/poker-room/ready".equals(path)) {
                AppState.POKER_ROOM_SERVICE.ready(request.getParameter("id"), tableId(request));
                writeJson(response, 200, "{\"success\":true}");
                RealtimeEndpoint.broadcastSnapshot();
                return;
            }

            if ("/poker-room/unready".equals(path)) {
                AppState.POKER_ROOM_SERVICE.unready(request.getParameter("id"), tableId(request));
                writeJson(response, 200, "{\"success\":true}");
                RealtimeEndpoint.broadcastSnapshot();
                return;
            }

            if ("/poker-room/start".equals(path)) {
                AppState.POKER_ROOM_SERVICE.start(request.getParameter("id"), tableId(request));
                writeJson(response, 200, "{\"success\":true}");
                RealtimeEndpoint.broadcastSnapshot();
                return;
            }

            if ("/poker-room/next".equals(path)) {
                AppState.POKER_ROOM_SERVICE.nextHand(request.getParameter("id"), tableId(request));
                writeJson(response, 200, "{\"success\":true}");
                RealtimeEndpoint.broadcastSnapshot();
                return;
            }

            if ("/poker-room/leave".equals(path)) {
                AppState.POKER_ROOM_SERVICE.leave(request.getParameter("id"), tableId(request));
                writeJson(response, 200, "{\"success\":true}");
                RealtimeEndpoint.broadcastSnapshot();
                return;
            }

            if ("/poker-room/fold".equals(path)) {
                AppState.POKER_ROOM_SERVICE.fold(request.getParameter("id"), tableId(request));
                writeJson(response, 200, "{\"success\":true}");
                RealtimeEndpoint.broadcastSnapshot();
                return;
            }

            if ("/poker-room/bet".equals(path)) {
                AppState.POKER_ROOM_SERVICE.bet(
                        request.getParameter("id"),
                        Integer.parseInt(request.getParameter("chips")),
                        tableId(request)
                );
                writeJson(response, 200, "{\"success\":true}");
                RealtimeEndpoint.broadcastSnapshot();
                return;
            }

            writeJson(response, 404, "{\"success\":false,\"message\":\"not found\"}");
        } catch (IllegalArgumentException exception) {
            writeJson(response, 400, "{\"success\":false,\"message\":\"" + AppState.escapeJson(exception.getMessage()) + "\"}");
        } catch (IllegalStateException exception) {
            writeJson(response, 409, "{\"success\":false,\"message\":\"" + AppState.escapeJson(exception.getMessage()) + "\"}");
        }
    }

    private void writeJson(HttpServletResponse response, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        response.setStatus(status);
        response.setContentType("application/json; charset=utf-8");
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    private int tableId(HttpServletRequest request) {
        String value = request.getParameter("table");
        if (value == null || value.isBlank()) {
            return 1;
        }
        return Integer.parseInt(value);
    }
}
