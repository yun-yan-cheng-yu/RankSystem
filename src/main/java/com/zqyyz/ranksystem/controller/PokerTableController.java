package com.zqyyz.ranksystem.controller;

import com.zqyyz.ranksystem.AppState;
import com.zqyyz.ranksystem.PokerRoomService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
public class PokerTableController {

    private static final MediaType JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

    private final PokerRoomService pokerRoomService;
    private final AppState appState;

    public PokerTableController(PokerRoomService pokerRoomService, AppState appState) {
        this.pokerRoomService = pokerRoomService;
        this.appState = appState;
    }

    @GetMapping("/poker-tables")
    public ResponseEntity<String> getTables() {
        return ResponseEntity.ok()
                .contentType(JSON_UTF8)
                .body(appState.pokerTablesJson(pokerRoomService.tableSummaries()));
    }
}
