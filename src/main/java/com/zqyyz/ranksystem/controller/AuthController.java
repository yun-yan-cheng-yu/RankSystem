package com.zqyyz.ranksystem.controller;

import com.zqyyz.ranksystem.AppState;
import com.zqyyz.ranksystem.LoginService;
import com.zqyyz.ranksystem.PokerRoomService;
import com.zqyyz.ranksystem.RealtimeEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {

    private final LoginService loginService;
    private final PokerRoomService pokerRoomService;
    private final AppState appState;

    public AuthController(LoginService loginService, PokerRoomService pokerRoomService, AppState appState) {
        this.loginService = loginService;
        this.pokerRoomService = pokerRoomService;
        this.appState = appState;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestParam("id") String playerId) {
        appState.expireIdlePlayers();
        String token = loginService.login(playerId);
        RealtimeEndpoint.broadcastGlobalLobby();
        return ResponseEntity.ok(Map.of("success", true, "token", token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestParam("id") String playerId,
            @RequestParam("token") String token) {
        loginService.validateToken(playerId, token);
        pokerRoomService.leaveAnyTable(playerId);
        loginService.logout(playerId);
        RealtimeEndpoint.broadcastGlobalLobby();
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat(
            @RequestParam("id") String playerId,
            @RequestParam("token") String token) {
        loginService.heartbeat(playerId, token);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
