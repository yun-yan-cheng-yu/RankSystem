package com.zqyyz.ranksystem.servlet;

import com.zqyyz.ranksystem.AppState;
import com.zqyyz.ranksystem.RealtimeEndpoint;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {
        "/login",
        "/logout",
        "/heartbeat"
})
public class AuthServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        expireIdlePlayers();
        try {
            if ("/heartbeat".equals(request.getServletPath())) {
                validateSession(request, true);
                writeJson(response, 200, AppState.successJson());
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
                case "/login" -> {
                    String token = AppState.LOGIN_SERVICE.login(request.getParameter("id"));
                    writeJson(response, 200, AppState.loginSuccessJson(token));
                    RealtimeEndpoint.broadcastSnapshot();
                }
                case "/logout" -> {
                    validateSession(request, false);
                    String playerId = request.getParameter("id");
                    AppState.POKER_ROOM_SERVICE.leaveAnyTable(playerId);
                    AppState.LOGIN_SERVICE.logout(playerId);
                    writeJson(response, 200, AppState.successJson());
                    RealtimeEndpoint.broadcastSnapshot();
                }
                default -> writeJson(response, 404, AppState.errorJson("not found"));
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            handleException(response, exception);
        }
    }
}
