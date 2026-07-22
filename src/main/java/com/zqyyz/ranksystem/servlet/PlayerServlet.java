package com.zqyyz.ranksystem.servlet;

import com.zqyyz.ranksystem.AppState;
import com.zqyyz.ranksystem.model.PlayerSession;
import com.zqyyz.ranksystem.RealtimeEndpoint;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = {
        "/players",
        "/state"
})
public class PlayerServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        expireIdlePlayers();
        try {
            if ("/players".equals(request.getServletPath())) {
                String game = request.getParameter("game");
                List<PlayerSession> players = game == null
                        ? AppState.LOGIN_SERVICE.getOnlinePlayers()
                        : AppState.LOGIN_SERVICE.getPlayersByGame(game);
                writeJson(response, 200, AppState.playersJson(players));
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
            if ("/state".equals(request.getServletPath())) {
                validateSession(request, true);
                AppState.LOGIN_SERVICE.updateStatus(request.getParameter("id"), request.getParameter("state"));
                writeJson(response, 200, AppState.successJson());
                RealtimeEndpoint.broadcastSnapshot();
                return;
            }
            writeJson(response, 404, AppState.errorJson("not found"));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            handleException(response, exception);
        }
    }
}
