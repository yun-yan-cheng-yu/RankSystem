package com.zqyyz.ranksystem.controller;

import com.zqyyz.ranksystem.AppState;
import com.zqyyz.ranksystem.LoginService;
import com.zqyyz.ranksystem.PokerRoomService;
import com.zqyyz.ranksystem.RealtimeEndpoint;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/poker-room")
public class PokerRoomController {

    private static final MediaType JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

    private final LoginService loginService;
    private final PokerRoomService pokerRoomService;
    private final AppState appState;

    public PokerRoomController(LoginService loginService, PokerRoomService pokerRoomService, AppState appState) {
        this.loginService = loginService;
        this.pokerRoomService = pokerRoomService;
        this.appState = appState;
    }

    // GET /poker-room -> current room snapshot
    @GetMapping
    public ResponseEntity<String> getRoom(
            @RequestParam("id") String playerId,
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "table", defaultValue = "1") int tableId) {
        loginService.validateToken(playerId, token);
        var room = pokerRoomService.snapshot(tableId);
        return ResponseEntity.ok()
                .contentType(JSON_UTF8)
                .body(appState.pokerRoomJson(room, playerId));
    }

    // POST /poker-room/join
    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> join(
            @RequestParam("id") String playerId,
            @RequestParam("token") String token,
            @RequestParam(value = "table", defaultValue = "1") int tableId) {
        loginService.markAction(playerId, token);
        pokerRoomService.join(playerId, tableId);
        RealtimeEndpoint.broadcastPokerLobby();
        return ResponseEntity.ok(Map.of("success", true));
    }

    // POST /poker-room/ready
    @PostMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready(
            @RequestParam("id") String playerId,
            @RequestParam("token") String token,
            @RequestParam(value = "table", defaultValue = "1") int tableId) {
        loginService.markAction(playerId, token);
        pokerRoomService.ready(playerId, tableId);
        RealtimeEndpoint.broadcastPokerLobby();
        return ResponseEntity.ok(Map.of("success", true));
    }

    // POST /poker-room/unready
    @PostMapping("/unready")
    public ResponseEntity<Map<String, Object>> unready(
            @RequestParam("id") String playerId,
            @RequestParam("token") String token,
            @RequestParam(value = "table", defaultValue = "1") int tableId) {
        loginService.markAction(playerId, token);
        pokerRoomService.unready(playerId, tableId);
        RealtimeEndpoint.broadcastPokerLobby();
        return ResponseEntity.ok(Map.of("success", true));
    }

    // POST /poker-room/start
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(
            @RequestParam("id") String playerId,
            @RequestParam("token") String token,
            @RequestParam(value = "table", defaultValue = "1") int tableId) {
        loginService.markAction(playerId, token);
        pokerRoomService.start(playerId, tableId);
        RealtimeEndpoint.broadcastPokerLobby();
        return ResponseEntity.ok(Map.of("success", true));
    }

    // POST /poker-room/next
    @PostMapping("/next")
    public ResponseEntity<Map<String, Object>> nextHand(
            @RequestParam("id") String playerId,
            @RequestParam("token") String token,
            @RequestParam(value = "table", defaultValue = "1") int tableId) {
        loginService.markAction(playerId, token);
        pokerRoomService.nextHand(playerId, tableId);
        RealtimeEndpoint.broadcastPokerLobby();
        return ResponseEntity.ok(Map.of("success", true));
    }

    // POST /poker-room/leave
    @PostMapping("/leave")
    public ResponseEntity<Map<String, Object>> leave(
            @RequestParam("id") String playerId,
            @RequestParam("token") String token,
            @RequestParam(value = "table", defaultValue = "1") int tableId) {
        loginService.markAction(playerId, token);
        pokerRoomService.leave(playerId, tableId);
        RealtimeEndpoint.broadcastPokerLobby();
        return ResponseEntity.ok(Map.of("success", true));
    }

    // POST /poker-room/fold
    @PostMapping("/fold")
    public ResponseEntity<Map<String, Object>> fold(
            @RequestParam("id") String playerId,
            @RequestParam("token") String token,
            @RequestParam(value = "table", defaultValue = "1") int tableId) {
        loginService.markAction(playerId, token);
        pokerRoomService.fold(playerId, tableId);
        RealtimeEndpoint.broadcastPokerTable(tableId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // POST /poker-room/bet
    @PostMapping("/bet")
    public ResponseEntity<Map<String, Object>> bet(
            @RequestParam("id") String playerId,
            @RequestParam("token") String token,
            @RequestParam("chips") int chips,
            @RequestParam(value = "table", defaultValue = "1") int tableId) {
        loginService.markAction(playerId, token);
        pokerRoomService.bet(playerId, chips, tableId);
        RealtimeEndpoint.broadcastPokerTable(tableId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
