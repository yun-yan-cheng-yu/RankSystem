package com.example.ranksystem;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginServiceTest {
    @Test
    void loginAddsPlayerInLobbyStatus() {
        LoginService service = new LoginService();

        service.login("player-1");

        assertEquals(
                List.of(new PlayerSession("player-1", PlayerStatus.LOBBY)),
                service.getOnlinePlayers()
        );
    }

    @Test
    void logoutRemovesPlayerSession() {
        LoginService service = new LoginService();
        service.login("player-1");

        service.logout("player-1");

        assertEquals(List.of(), service.getOnlinePlayers());
    }

    @Test
    void duplicateLoginKeepsOnePlayerAndResetsToLobby() {
        LoginService service = new LoginService();

        service.login("player-1");
        service.updateStatus("player-1", PlayerStatus.GAME_A_ROOM);
        service.login("player-1");

        assertEquals(
                List.of(new PlayerSession("player-1", PlayerStatus.LOBBY)),
                service.getOnlinePlayers()
        );
    }

    @Test
    void blankLoginIdIsRejected() {
        LoginService service = new LoginService();

        assertThrows(IllegalArgumentException.class, () -> service.login("  "));
    }

    @Test
    void onlinePlayersAreSortedWithStatus() {
        LoginService service = new LoginService();

        service.login("player-2");
        service.login("player-1");
        service.updateStatus("player-2", PlayerStatus.GAME_A_ROOM);

        assertEquals(
                List.of(
                        new PlayerSession("player-1", PlayerStatus.LOBBY),
                        new PlayerSession("player-2", PlayerStatus.GAME_A_ROOM)
                ),
                service.getOnlinePlayers()
        );
    }

    @Test
    void gameAPlayersIncludeRoomAndPlayingStatuses() {
        LoginService service = new LoginService();

        service.login("player-1");
        service.login("player-2");
        service.login("player-3");
        service.updateStatus("player-1", PlayerStatus.GAME_A_ROOM);
        service.updateStatus("player-2", PlayerStatus.GAME_A_PLAYING);
        service.updateStatus("player-3", PlayerStatus.GAME_B_ROOM);

        assertEquals(
                List.of(
                        new PlayerSession("player-1", PlayerStatus.GAME_A_ROOM),
                        new PlayerSession("player-2", PlayerStatus.GAME_A_PLAYING)
                ),
                service.getPlayersByGame("A")
        );
    }
}
