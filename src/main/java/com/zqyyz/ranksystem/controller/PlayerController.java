package com.zqyyz.ranksystem.controller;

import com.zqyyz.ranksystem.AppState;
import com.zqyyz.ranksystem.LoginService;
import com.zqyyz.ranksystem.RealtimeEndpoint;
import com.zqyyz.ranksystem.model.PlayerSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class PlayerController {

    private static final MediaType JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

    private final LoginService loginService;
    private final AppState appState;

    public PlayerController(LoginService loginService, AppState appState) {
        this.loginService = loginService;
        this.appState = appState;
    }

    @GetMapping("/players")
    public ResponseEntity<String> getPlayers(@RequestParam(value = "game", required = false) String game) {
        List<PlayerSession> players = game == null
                ? loginService.getOnlinePlayers()
                : loginService.getPlayersByGame(game);
        return ResponseEntity.ok().contentType(JSON_UTF8).body(appState.playersJson(players));
    }

    @PostMapping("/state")
    public ResponseEntity<String> updateStatus(
            @RequestParam("id") String playerId,
            @RequestParam("token") String token,
            @RequestParam("state") String state) {
        loginService.markAction(playerId, token);
        loginService.updateStatus(playerId, state);
        RealtimeEndpoint.broadcastGlobalLobby();
        return ResponseEntity.ok().contentType(JSON_UTF8).body(appState.successJson());
    }
}
