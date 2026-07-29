package com.zqyyz.ranksystem;

import com.zqyyz.ranksystem.model.PlayerSession;
import com.zqyyz.ranksystem.model.PlayingCard;
import com.zqyyz.ranksystem.model.PokerRoomPlayer;
import com.zqyyz.ranksystem.model.PokerRoomSnapshot;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PokerRoomServiceTest {
    @Test
    void playerCanJoinRoomUnready() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = new PokerRoomService(loginService);
        loginService.login("player-1");

        roomService.join("player-1");

        PokerRoomSnapshot room = roomService.snapshot();
        assertEquals(1, room.players().size());
        assertEquals(new PokerRoomPlayer("player-1", false), room.players().get(0));
        assertEquals(0, room.players().get(0).score());
        assertEquals("player-1", room.hostId());
        assertFalse(room.canStart());
        assertEquals(
                new PlayerSession("player-1", PlayerStatus.GAME_A_ROOM),
                loginService.getOnlinePlayers().get(0)
        );
    }

    @Test
    void roomCanStartWhenAtLeastTwoPlayersAreReady() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = new PokerRoomService(loginService);
        loginService.login("player-1");
        loginService.login("player-2");
        roomService.join("player-1");
        roomService.join("player-2");

        roomService.ready("player-1");
        roomService.ready("player-2");

        assertTrue(roomService.snapshot().canStart());
    }

    @Test
    void roomCannotStartWithOnlyOneReadyPlayer() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = new PokerRoomService(loginService);
        loginService.login("player-1");
        roomService.join("player-1");

        roomService.ready("player-1");

        assertFalse(roomService.snapshot().canStart());
    }

    @Test
    void startMovesPlayersToPokerPlayingStatus() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = new PokerRoomService(loginService);
        loginService.login("player-1");
        loginService.login("player-2");
        roomService.join("player-1");
        roomService.join("player-2");
        roomService.ready("player-1");
        roomService.ready("player-2");

        roomService.start("player-1");

        assertTrue(roomService.snapshot().started());
        assertEquals(PlayerStatus.GAME_A_PLAYING, loginService.getOnlinePlayers().get(0).status());
        assertEquals(PlayerStatus.GAME_A_PLAYING, loginService.getOnlinePlayers().get(1).status());
    }

    @Test
    void startDealsCardsAndCreatesInitialPot() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = new PokerRoomService(loginService);
        loginService.login("player-1");
        loginService.login("player-2");
        roomService.join("player-1");
        roomService.join("player-2");
        roomService.ready("player-1");
        roomService.ready("player-2");

        roomService.start("player-1");

        PokerRoomSnapshot room = roomService.snapshot();
        Set<String> dealtCards = new HashSet<>();
        assertEquals(2, room.communityCards().size());
        dealtCards.addAll(room.communityCards());
        assertEquals("player-1", room.dealerId());
        assertEquals("player-1", room.currentTurnId());
        assertEquals(4, room.pot());
        assertFalse(room.rules().isEmpty());
        for (PokerRoomPlayer player : room.players()) {
            assertEquals(2, player.holeCards().size());
            assertEquals(2, player.chipsCommitted());
            assertEquals(-2, player.score());
            assertTrue(dealtCards.addAll(player.holeCards()));
        }
        assertEquals(6, dealtCards.size());
    }

    @Test
    void roomJsonOnlyIncludesViewerHoleCards() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = new PokerRoomService(loginService);
        loginService.login("player-1");
        loginService.login("player-2");
        roomService.join("player-1");
        roomService.join("player-2");
        roomService.ready("player-1");
        roomService.ready("player-2");

        roomService.start("player-1");

        String playerOneJson = AppState.pokerRoomJson(roomService.snapshot(), "player-1");
        String publicJson = AppState.pokerRoomJson(roomService.snapshot());

        assertTrue(playerOneJson.matches("(?s).*\"id\":\"player-1\".*\"holeCards\":\\[[^]]+].*"));
        assertTrue(playerOneJson.matches("(?s).*\"id\":\"player-2\".*\"holeCards\":\\[\\].*"));
        assertTrue(publicJson.matches("(?s).*\"id\":\"player-1\".*\"holeCards\":\\[\\].*"));
        assertTrue(publicJson.matches("(?s).*\"id\":\"player-2\".*\"holeCards\":\\[\\].*"));
    }

    @Test
    void bettingMovesToNextAggressorBeforeDealingNextCommunityCard() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = startedTwoPlayerRoom(loginService);

        roomService.bet("player-1", 4);

        PokerRoomSnapshot afterFirstBet = roomService.snapshot();
        assertEquals("", afterFirstBet.currentTurnId());
        assertEquals(4, afterFirstBet.currentBet());
        assertEquals(8, afterFirstBet.pot());
        assertEquals(-6, scoreOf(afterFirstBet, "player-1"));
        assertEquals(-2, scoreOf(afterFirstBet, "player-2"));

        roomService.bet("player-2", 4);

        PokerRoomSnapshot afterMatch = roomService.snapshot();
        assertTrue(afterMatch.message().contains("跟注 4 筹码"));
        assertEquals(2, afterMatch.communityCards().size());
        assertEquals(0, afterMatch.currentBet());
        assertEquals(12, afterMatch.pot());
        assertEquals(-6, scoreOf(afterMatch, "player-1"));
        assertEquals(-6, scoreOf(afterMatch, "player-2"));
        assertEquals("player-2", afterMatch.currentAggressorId());
        assertEquals("player-2", afterMatch.currentTurnId());
    }

    @Test
    void callersCanActWithoutFollowingRoomOrder() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = startedFourPlayerRoom(loginService);

        roomService.bet("player-a", 4);
        roomService.bet("player-d", 4);
        roomService.fold("player-c");

        PokerRoomSnapshot beforeLastCaller = roomService.snapshot();
        assertEquals(2, beforeLastCaller.communityCards().size());
        assertEquals("", beforeLastCaller.currentTurnId());
        assertEquals(4, beforeLastCaller.currentBet());

        roomService.bet("player-b", 4);

        PokerRoomSnapshot afterLastCaller = roomService.snapshot();
        assertEquals("player-b", afterLastCaller.currentAggressorId());
        assertEquals("player-b", afterLastCaller.currentTurnId());
    }

    @Test
    void playerCannotSeeOtherCallerChoicesBeforeDeciding() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = new PokerRoomService(loginService);
        for (String playerId : List.of("player-a", "player-b", "player-c", "player-d", "player-e")) {
            loginService.login(playerId);
            roomService.join(playerId);
            roomService.ready(playerId);
        }
        roomService.start("player-a");

        roomService.bet("player-a", 4);
        roomService.bet("player-d", 4);
        roomService.fold("player-c");

        String playerBJson = AppState.pokerRoomJson(roomService.snapshot(), "player-b");
        assertTrue(playerBJson.contains("\"id\":\"player-d\",\"ready\":true,\"folded\":false,\"chipsCommitted\":2,\"roundBet\":0,\"acted\":false"));
        assertTrue(playerBJson.contains("\"id\":\"player-c\",\"ready\":true,\"folded\":false,\"chipsCommitted\":2,\"roundBet\":0,\"acted\":false"));

        roomService.bet("player-b", 4);

        String playerBDecidedJson = AppState.pokerRoomJson(roomService.snapshot(), "player-b");
        assertTrue(playerBDecidedJson.contains("\"id\":\"player-d\",\"ready\":true,\"folded\":false,\"chipsCommitted\":6,\"roundBet\":4,\"acted\":true"));
        assertTrue(playerBDecidedJson.contains("\"id\":\"player-c\",\"ready\":true,\"folded\":true"));
    }

    @Test
    void communityCardIsDealtAfterEveryActivePlayerCompletesAggressorStage() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = startedFourPlayerRoom(loginService);

        completeAggressorStage(roomService, "player-a", 4, "player-b", "player-c", "player-d");
        PokerRoomSnapshot afterA = roomService.snapshot();
        assertEquals(2, afterA.communityCards().size());
        assertEquals("player-b", afterA.currentAggressorId());
        assertEquals("player-b", afterA.currentTurnId());

        completeAggressorStage(roomService, "player-b", 5, "player-a", "player-c", "player-d");
        PokerRoomSnapshot afterB = roomService.snapshot();
        assertEquals(2, afterB.communityCards().size());
        assertEquals("player-c", afterB.currentAggressorId());
        assertEquals("player-c", afterB.currentTurnId());

        completeAggressorStage(roomService, "player-c", 6, "player-a", "player-b", "player-d");
        PokerRoomSnapshot afterC = roomService.snapshot();
        assertEquals(2, afterC.communityCards().size());
        assertEquals("player-d", afterC.currentAggressorId());
        assertEquals("player-d", afterC.currentTurnId());

        completeAggressorStage(roomService, "player-d", 7, "player-a", "player-b", "player-c");
        PokerRoomSnapshot afterD = roomService.snapshot();
        assertEquals(3, afterD.communityCards().size());
        assertEquals(0, afterD.currentBet());
        assertEquals("player-a", afterD.currentAggressorId());
        assertEquals("player-a", afterD.currentTurnId());
    }

    @Test
    void thirdBettingRoundDealsFifthCommunityCardAndStartsFinalBettingRound() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = startedFourPlayerRoom(loginService);

        completeFullBettingRound(roomService, 4);
        completeFullBettingRound(roomService, 5);
        completeFullBettingRound(roomService, 6);

        PokerRoomSnapshot room = roomService.snapshot();
        assertEquals(5, room.communityCards().size());
        assertFalse(room.finished());
        assertEquals("player-a", room.currentAggressorId());
        assertEquals("player-a", room.currentTurnId());
    }

    @Test
    void fourthBettingRoundAfterFifthCommunityCardFinishesByShowdown() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = startedFourPlayerRoom(loginService);

        completeFullBettingRound(roomService, 4);
        completeFullBettingRound(roomService, 5);
        completeFullBettingRound(roomService, 6);
        completeFullBettingRound(roomService, 7);

        PokerRoomSnapshot room = roomService.snapshot();
        assertEquals(5, room.communityCards().size());
        assertTrue(room.finished());
        assertFalse(room.winnerId().isBlank());
    }

    @Test
    void showdownJsonRevealsAllHoleCards() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = startedFourPlayerRoom(loginService);

        completeFullBettingRound(roomService, 4);
        completeFullBettingRound(roomService, 5);
        completeFullBettingRound(roomService, 6);
        completeFullBettingRound(roomService, 7);

        String publicJson = AppState.pokerRoomJson(roomService.snapshot());

        assertFalse(publicJson.contains("\"holeCards\":[]"));
        assertTrue(publicJson.contains("\"handName\":\""));
        assertTrue(publicJson.contains("\"bestCards\":["));
    }

    @Test
    void texasHoldemShowdownUsesFormalHandRanking() {
        assertTrue(PokerRoomService.compareBestHands(
                List.of("10♠", "J♠", "Q♠", "K♠", "A♠"),
                List.of("9♣", "9♦", "9♥", "9♠", "A♦")
        ) > 0);

        assertTrue(PokerRoomService.compareBestHands(
                List.of("8♣", "8♦", "8♥", "K♠", "K♦"),
                List.of("A♣", "Q♣", "10♣", "7♣", "3♣")
        ) > 0);

        assertTrue(PokerRoomService.compareBestHands(
                List.of("A♠", "2♦", "3♣", "4♥", "5♠"),
                List.of("K♠", "Q♦", "J♣", "10♥", "8♠")
        ) > 0);
    }

    @Test
    void texasHoldemShowdownPicksBestFiveCardsFromSeven() {
        assertTrue(PokerRoomService.compareBestHands(
                List.of("A♠", "K♠", "Q♠", "J♠", "10♠", "2♦", "3♣"),
                List.of("9♣", "9♦", "9♥", "9♠", "A♦", "2♣", "3♦")
        ) > 0);

        assertTrue(PokerRoomService.compareBestHands(
                List.of("A♠", "A♦", "K♣", "K♥", "2♠", "7♦", "8♣"),
                List.of("A♣", "A♥", "Q♣", "Q♥", "J♠", "7♣", "8♦")
        ) > 0);

        assertEquals(
                List.of("A♠", "K♠", "Q♠", "J♠", "10♠"),
                PokerRoomService.bestHandCards(List.of("A♠", "K♠"), List.of("Q♠", "J♠", "10♠", "2♦", "3♣"))
        );
    }

    @Test
    void bestHandFromCardsAcceptsSevenCardsDirectly() {
        var bestHand = PokerRoomService.bestHandFromCards(List.of("A♠", "K♠", "Q♠", "J♠", "10♠", "2♦", "3♣"));

        assertEquals("皇家同花顺", bestHand.name());
        assertEquals(List.of("A♠", "K♠", "Q♠", "J♠", "10♠"), bestHand.cards());
    }

    @Test
    void playingCardParsesRankAndSuitWithoutChangingDisplayText() {
        PlayingCard card = PlayingCard.parse("10♠");

        assertEquals(10, card.rank().value());
        assertEquals("♠", card.suit().symbol());
        assertEquals("10♠", card.toString());
    }

    @Test
    void nextHandRotatesDealerAndStartsFromNewDealer() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = startedFourPlayerRoom(loginService);
        completeFullBettingRound(roomService, 4);
        completeFullBettingRound(roomService, 5);
        completeFullBettingRound(roomService, 6);
        completeFullBettingRound(roomService, 7);

        assertThrows(IllegalStateException.class, () -> roomService.nextHand("player-a"));

        roomService.nextHand("player-b");

        PokerRoomSnapshot room = roomService.snapshot();
        assertTrue(room.started());
        assertFalse(room.finished());
        assertEquals("player-b", room.dealerId());
        assertEquals("player-b", room.currentAggressorId());
        assertEquals("player-b", room.currentTurnId());
        assertEquals(2, room.communityCards().size());
        assertEquals(8, room.pot());
    }

    @Test
    void foldingToOneActivePlayerFinishesRound() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = startedTwoPlayerRoom(loginService);

        roomService.fold("player-1");

        PokerRoomSnapshot room = roomService.snapshot();
        assertTrue(room.finished());
        assertEquals("player-2", room.winnerId());
        assertEquals("", room.currentTurnId());
        assertEquals(-2, scoreOf(room, "player-1"));
        assertEquals(2, scoreOf(room, "player-2"));
    }

    @Test
    void scoreCarriesIntoNextHand() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = startedTwoPlayerRoom(loginService);

        roomService.fold("player-1");
        roomService.nextHand("player-2");

        PokerRoomSnapshot room = roomService.snapshot();
        assertEquals(0, scoreOf(room, "player-2"));
    }

    @Test
    void nonCurrentPlayerCannotAct() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = startedTwoPlayerRoom(loginService);

        assertThrows(IllegalStateException.class, () -> roomService.bet("player-2", 4));
    }

    @Test
    void playersCanJoinDifferentTables() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = new PokerRoomService(loginService);
        loginService.login("table-a");
        loginService.login("table-b");
        loginService.login("table-c");

        roomService.join("table-a", 2);
        roomService.join("table-b", 2);
        roomService.join("table-c", 3);

        assertEquals(10, roomService.tableSummaries().size());
        assertEquals(List.of("table-a", "table-b"), roomService.tableSummaries().get(1).playerIds());
        assertEquals(List.of("table-c"), roomService.tableSummaries().get(2).playerIds());
        assertEquals("table-a", roomService.tableSummaries().get(1).hostId());
        assertEquals(
                List.of(new PokerRoomPlayer("table-a", false), new PokerRoomPlayer("table-b", false)),
                roomService.tableSummaries().get(1).players()
        );
        assertEquals(2, roomService.snapshot(2).players().size());
        assertEquals(1, roomService.snapshot(3).players().size());
    }

    @Test
    void tableIdForPlayerReturnsCurrentTable() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = new PokerRoomService(loginService);
        loginService.login("player-1");

        assertEquals(0, roomService.tableIdForPlayer("player-1"));

        roomService.join("player-1", 4);

        assertEquals(4, roomService.tableIdForPlayer("player-1"));
    }

    @Test
    void playerCanCancelReady() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = new PokerRoomService(loginService);
        loginService.login("player-1");
        roomService.join("player-1");
        roomService.ready("player-1");

        roomService.unready("player-1");

        assertFalse(roomService.snapshot().players().get(0).ready());
    }

    @Test
    void onlyHostCanStartRoom() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = new PokerRoomService(loginService);
        loginService.login("player-1");
        loginService.login("player-2");
        roomService.join("player-1");
        roomService.join("player-2");
        roomService.ready("player-1");
        roomService.ready("player-2");

        assertThrows(IllegalStateException.class, () -> roomService.start("player-2"));

        roomService.start("player-1");
        assertTrue(roomService.snapshot().started());
    }

    @Test
    void nextPlayerBecomesHostWhenHostLeaves() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = new PokerRoomService(loginService);
        loginService.login("player-1");
        loginService.login("player-2");
        roomService.join("player-1");
        roomService.join("player-2");

        roomService.leave("player-1");

        assertEquals("player-2", roomService.snapshot().hostId());
        assertEquals(PlayerStatus.GAME_A_LOBBY, loginService.getOnlinePlayers().get(0).status());
    }

    @Test
    void roomRejectsEleventhPlayer() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = new PokerRoomService(loginService);
        for (int i = 1; i <= 10; i++) {
            String playerId = "player-" + i;
            loginService.login(playerId);
            roomService.join(playerId);
        }
        loginService.login("player-11");

        assertThrows(IllegalStateException.class, () -> roomService.join("player-11"));
    }

    @Test
    void roomRejectsJoinAfterGameStarted() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = new PokerRoomService(loginService);
        loginService.login("player-1");
        loginService.login("player-2");
        loginService.login("player-3");
        roomService.join("player-1");
        roomService.join("player-2");
        roomService.ready("player-1");
        roomService.ready("player-2");
        roomService.start("player-1");

        assertThrows(IllegalStateException.class, () -> roomService.join("player-3"));
    }

    @Test
    void leavingStartedRoomIsRejected() {
        LoginService loginService = new LoginService();
        PokerRoomService roomService = new PokerRoomService(loginService);
        loginService.login("player-1");
        loginService.login("player-2");
        roomService.join("player-1");
        roomService.join("player-2");
        roomService.ready("player-1");
        roomService.ready("player-2");
        roomService.start("player-1");

        assertThrows(IllegalStateException.class, () -> roomService.leave("player-1"));

        assertEquals(2, roomService.snapshot().players().size());
        assertEquals(PlayerStatus.GAME_A_PLAYING, loginService.getOnlinePlayers().get(0).status());
    }

    private PokerRoomService startedTwoPlayerRoom(LoginService loginService) {
        PokerRoomService roomService = new PokerRoomService(loginService);
        loginService.login("player-1");
        loginService.login("player-2");
        roomService.join("player-1");
        roomService.join("player-2");
        roomService.ready("player-1");
        roomService.ready("player-2");
        roomService.start("player-1");
        return roomService;
    }

    private PokerRoomService startedFourPlayerRoom(LoginService loginService) {
        PokerRoomService roomService = new PokerRoomService(loginService);
        for (String playerId : List.of("player-a", "player-b", "player-c", "player-d")) {
            loginService.login(playerId);
            roomService.join(playerId);
            roomService.ready(playerId);
        }
        roomService.start("player-a");
        return roomService;
    }

    private void completeAggressorStage(PokerRoomService roomService, String aggressorId, int chips, String... callerIds) {
        roomService.bet(aggressorId, chips);
        for (String callerId : callerIds) {
            roomService.bet(callerId, chips);
        }
    }

    private void completeFullBettingRound(PokerRoomService roomService, int chips) {
        completeAggressorStage(roomService, "player-a", chips, "player-b", "player-c", "player-d");
        completeAggressorStage(roomService, "player-b", chips, "player-a", "player-c", "player-d");
        completeAggressorStage(roomService, "player-c", chips, "player-a", "player-b", "player-d");
        completeAggressorStage(roomService, "player-d", chips, "player-a", "player-b", "player-c");
    }

    private int scoreOf(PokerRoomSnapshot room, String playerId) {
        return room.players().stream()
                .filter(player -> playerId.equals(player.playerId()))
                .findFirst()
                .orElseThrow()
                .score();
    }
}
