package com.zqyyz.ranksystem.servlet;

import com.zqyyz.ranksystem.AppState;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/poker-tables")
public class PokerTableServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        expireIdlePlayers();
        try {
            writeJson(response, 200, AppState.pokerTablesJson(AppState.POKER_ROOM_SERVICE.tableSummaries()));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            handleException(response, exception);
        }
    }
}
