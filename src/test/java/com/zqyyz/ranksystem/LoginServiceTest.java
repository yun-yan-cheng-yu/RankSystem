package com.zqyyz.ranksystem;

import com.zqyyz.ranksystem.model.PlayerSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginServiceTest {
    @Test
    void loginAddsPlayerInLobbyStatus() {
        LoginService service = new LoginService();

        String token = service.login("player-1");

        assertEquals(
                List.of(new PlayerSession("player-1", PlayerStatus.LOBBY)),
                service.getOnlinePlayers()
        );
        assertTrue(service.isValidToken("player-1", token));
    }

    @Test
    void logoutRemovesPlayerSession() {
        LoginService service = new LoginService();
        service.login("player-1");

        service.logout("player-1");

        assertEquals(List.of(), service.getOnlinePlayers());
    }

    @Test
    void duplicateLoginReplacesTokenAndPreservesStatus() {
        LoginService service = new LoginService();

        String firstToken = service.login("player-1");
        service.updateStatus("player-1", PlayerStatus.GAME_A_ROOM);
        String secondToken = service.login("player-1");

        assertEquals(
                List.of(new PlayerSession("player-1", PlayerStatus.GAME_A_ROOM)),
                service.getOnlinePlayers()
        );
        assertNotEquals(firstToken, secondToken);
        assertFalse(service.isValidToken("player-1", firstToken));
        assertTrue(service.isValidToken("player-1", secondToken));
    }

    @Test
    void idlePlayerExpiresAndTokenBecomesInvalid() {
        LoginService service = new LoginService();
        String token = service.login("player-1");

        assertEquals(List.of("player-1"), service.expireIdlePlayers(
                System.currentTimeMillis() + AppState.IDLE_TIMEOUT_MILLIS,
                AppState.IDLE_TIMEOUT_MILLIS,
                playerId -> false
        ));

        assertEquals(List.of(), service.getOnlinePlayers());
        assertFalse(service.isValidToken("player-1", token));
    }

    @Test
    void idlePlayerInRoomIsNotExpired() {
        LoginService service = new LoginService();
        String token = service.login("player-1");

        assertEquals(List.of(), service.expireIdlePlayers(
                System.currentTimeMillis() + AppState.IDLE_TIMEOUT_MILLIS,
                AppState.IDLE_TIMEOUT_MILLIS,
                "player-1"::equals
        ));

        assertEquals(
                List.of(new PlayerSession("player-1", PlayerStatus.LOBBY)),
                service.getOnlinePlayers()
        );
        assertTrue(service.isValidToken("player-1", token));
    }

    @Test
    void touchKeepsPlayerActiveUntilTimeoutAfterLastHeartbeat() {
        LoginService service = new LoginService();
        String token = service.login("player-1");
        long heartbeatAtMillis = 1_000L;
        long timeoutMillis = AppState.IDLE_TIMEOUT_MILLIS;

        service.touch("player-1", token, heartbeatAtMillis);

        assertEquals(List.of(), service.expireIdlePlayers(
                heartbeatAtMillis + timeoutMillis - 1,
                timeoutMillis,
                playerId -> false
        ));

        assertEquals(List.of("player-1"), service.expireIdlePlayers(
                heartbeatAtMillis + timeoutMillis,
                timeoutMillis,
                playerId -> false
        ));
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
