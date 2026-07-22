package com.example.ranksystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PokerRoomService {
    private static final int TABLE_COUNT = 10;
    private static final int MAX_PLAYERS = 10;
    private static final int ANTE_CHIPS = 2;
    private static final List<String> RULES = List.of(
            "每个玩家初始发两张底牌，桌面先发两张公共牌。",
            "每位玩家需要押底 2 个筹码。",
            "从庄家开始叫牌：可以弃牌，也可以押注 2-10 个筹码。",
            "若有玩家押注，剩余玩家必须押注相同筹码，或选择弃牌。",
            "当弃牌后只剩一位玩家，该玩家赢得本局所有筹码。",
            "每一轮叫牌结束后，桌面发一张公共牌并进入下一轮叫牌。",
            "桌面发出第五张公共牌后为最后一轮，所有玩家叫牌结束后比大小，最大者赢得本局所有筹码。"
    );

    private final LoginService loginService;
    private final Map<Integer, TableState> tables = new LinkedHashMap<>();

    public PokerRoomService(LoginService loginService) {
        this.loginService = loginService;
        for (int tableId = 1; tableId <= TABLE_COUNT; tableId++) {
            tables.put(tableId, new TableState(tableId));
        }
    }

    public synchronized void join(String playerId) {
        join(playerId, 1);
    }

    public synchronized void join(String playerId, int tableId) {
        String normalizedPlayerId = loginService.normalizePlayerId(playerId);
        TableState targetTable = table(tableId);
        if (targetTable.started) {
            throw new IllegalStateException("game already started");
        }
        if (!targetTable.players.containsKey(normalizedPlayerId) && targetTable.players.size() >= MAX_PLAYERS) {
            throw new IllegalStateException("table is full");
        }

        leaveOtherTables(normalizedPlayerId, tableId);
        targetTable.players.putIfAbsent(normalizedPlayerId, new PokerRoomPlayer(normalizedPlayerId, false));
        loginService.updateStatus(normalizedPlayerId, PlayerStatus.GAME_A_ROOM);
    }

    public synchronized void ready(String playerId) {
        ready(playerId, 1);
    }

    public synchronized void ready(String playerId, int tableId) {
        String normalizedPlayerId = loginService.normalizePlayerId(playerId);
        TableState targetTable = table(tableId);
        ensureInTable(targetTable, normalizedPlayerId);
        PokerRoomPlayer player = targetTable.players.get(normalizedPlayerId);
        targetTable.players.put(normalizedPlayerId, copyPlayer(player, true, player.holeCards(), player.folded(), player.chipsCommitted(), player.roundBet(), player.acted(), player.score()));
    }

    public synchronized void unready(String playerId) {
        unready(playerId, 1);
    }

    public synchronized void unready(String playerId, int tableId) {
        String normalizedPlayerId = loginService.normalizePlayerId(playerId);
        TableState targetTable = table(tableId);
        ensureInTable(targetTable, normalizedPlayerId);
        PokerRoomPlayer player = targetTable.players.get(normalizedPlayerId);
        targetTable.players.put(normalizedPlayerId, copyPlayer(player, false, player.holeCards(), player.folded(), player.chipsCommitted(), player.roundBet(), player.acted(), player.score()));
    }

    public synchronized void start(String playerId) {
        start(playerId, 1);
    }

    public synchronized void start(String playerId, int tableId) {
        String normalizedPlayerId = loginService.normalizePlayerId(playerId);
        TableState targetTable = table(tableId);
        if (!normalizedPlayerId.equals(hostId(targetTable))) {
            throw new IllegalStateException("only host can start");
        }
        if (!canStart(targetTable)) {
            throw new IllegalStateException("table cannot start");
        }

        targetTable.started = true;
        dealInitialCards(targetTable);
        for (String roomPlayerId : targetTable.players.keySet()) {
            loginService.updateStatus(roomPlayerId, PlayerStatus.GAME_A_PLAYING);
        }
    }

    public synchronized void nextHand(String playerId) {
        nextHand(playerId, 1);
    }

    public synchronized void nextHand(String playerId, int tableId) {
        String normalizedPlayerId = loginService.normalizePlayerId(playerId);
        TableState targetTable = table(tableId);
        if (!targetTable.started || !targetTable.finished) {
            throw new IllegalStateException("current hand is not finished");
        }

        String nextDealerId = nextRoomPlayerId(targetTable, targetTable.dealerId);
        if (!normalizedPlayerId.equals(nextDealerId)) {
            throw new IllegalStateException("only next dealer can start next hand");
        }

        targetTable.dealerId = nextDealerId;
        dealInitialCards(targetTable);
        for (String roomPlayerId : targetTable.players.keySet()) {
            loginService.updateStatus(roomPlayerId, PlayerStatus.GAME_A_PLAYING);
        }
    }

    public synchronized void fold(String playerId) {
        fold(playerId, 1);
    }

    public synchronized void fold(String playerId, int tableId) {
        String normalizedPlayerId = loginService.normalizePlayerId(playerId);
        TableState targetTable = table(tableId);
        ensureCanAct(targetTable, normalizedPlayerId);
        PokerRoomPlayer player = targetTable.players.get(normalizedPlayerId);
        targetTable.players.put(normalizedPlayerId, copyPlayer(player, player.ready(), player.holeCards(), true, player.chipsCommitted(), player.roundBet(), true, player.score()));
        targetTable.message = normalizedPlayerId + " 弃牌";
        advanceAfterAction(targetTable);
    }

    public synchronized void bet(String playerId, int chips) {
        bet(playerId, chips, 1);
    }

    public synchronized void bet(String playerId, int chips, int tableId) {
        String normalizedPlayerId = loginService.normalizePlayerId(playerId);
        TableState targetTable = table(tableId);
        ensureCanAct(targetTable, normalizedPlayerId);
        if (chips < 2 || chips > 10) {
            throw new IllegalArgumentException("bet must be between 2 and 10");
        }

        boolean callAction = targetTable.currentBet > 0;
        if (targetTable.currentBet == 0) {
            if (!normalizedPlayerId.equals(targetTable.currentAggressorId)) {
                throw new IllegalStateException("waiting for " + targetTable.currentAggressorId + " to bet");
            }
            targetTable.currentBet = chips;
        } else if (chips != targetTable.currentBet) {
            throw new IllegalStateException("bet must match current bet: " + targetTable.currentBet);
        }

        PokerRoomPlayer player = targetTable.players.get(normalizedPlayerId);
        if (player.roundBet() == targetTable.currentBet) {
            throw new IllegalStateException("player already matched current bet");
        }

        int additionalChips = targetTable.currentBet - player.roundBet();
        targetTable.players.put(normalizedPlayerId, copyPlayer(player, player.ready(), player.holeCards(), false, player.chipsCommitted() + additionalChips, targetTable.currentBet, true, player.score() - additionalChips));
        targetTable.pot += additionalChips;
        targetTable.message = normalizedPlayerId + (callAction ? " 跟注 " : " 押注 ") + targetTable.currentBet + " 筹码";
        advanceAfterAction(targetTable);
    }

    public synchronized void leave(String playerId) {
        leave(playerId, 1);
    }

    public synchronized void leave(String playerId, int tableId) {
        String normalizedPlayerId = loginService.normalizePlayerId(playerId);
        TableState targetTable = table(tableId);
        if (targetTable.started && !targetTable.finished) {
            throw new IllegalStateException("cannot leave after game started");
        }
        targetTable.players.remove(normalizedPlayerId);
        loginService.updateStatus(normalizedPlayerId, PlayerStatus.GAME_A_ROOM);
        if (targetTable.players.isEmpty()) {
            resetGame(targetTable);
        } else if (normalizedPlayerId.equals(targetTable.dealerId) || normalizedPlayerId.equals(targetTable.currentTurnId)) {
            targetTable.dealerId = hostId(targetTable);
            targetTable.currentTurnId = targetTable.dealerId;
        }
    }

    public synchronized void leaveAnyTable(String playerId) {
        String normalizedPlayerId = loginService.normalizePlayerId(playerId);
        for (TableState targetTable : tables.values()) {
            if (targetTable.players.containsKey(normalizedPlayerId)) {
                leave(normalizedPlayerId, targetTable.tableId);
            }
        }
    }

    public synchronized PokerRoomSnapshot snapshot() {
        return snapshot(1);
    }

    public synchronized PokerRoomSnapshot snapshot(int tableId) {
        TableState targetTable = table(tableId);
        return new PokerRoomSnapshot(
                targetTable.tableId,
                new ArrayList<>(targetTable.players.values()),
                targetTable.started,
                canStart(targetTable),
                hostId(targetTable),
                targetTable.dealerId,
                targetTable.currentAggressorId,
                targetTable.currentTurnId,
                new ArrayList<>(targetTable.communityCards),
                targetTable.pot,
                targetTable.currentBet,
                targetTable.finished,
                targetTable.winnerId,
                targetTable.message,
                new ArrayList<>(targetTable.aggressorOrder),
                new ArrayList<>(targetTable.completedAggressorIds),
                RULES
        );
    }

    public synchronized List<PokerTableSummary> tableSummaries() {
        List<PokerTableSummary> summaries = new ArrayList<>();
        for (TableState targetTable : tables.values()) {
            summaries.add(new PokerTableSummary(
                    targetTable.tableId,
                    targetTable.players.keySet().stream().toList(),
                    hostId(targetTable),
                    new ArrayList<>(targetTable.players.values()),
                    targetTable.started,
                    targetTable.finished
            ));
        }
        return summaries;
    }

    public synchronized boolean isPlayerInAnyTable(String playerId) {
        String normalizedPlayerId = loginService.normalizePlayerId(playerId);
        return tables.values().stream()
                .anyMatch(targetTable -> targetTable.players.containsKey(normalizedPlayerId));
    }

    public synchronized int tableIdForPlayer(String playerId) {
        String normalizedPlayerId = loginService.normalizePlayerId(playerId);
        return tables.values().stream()
                .filter(targetTable -> targetTable.players.containsKey(normalizedPlayerId))
                .map(targetTable -> targetTable.tableId)
                .findFirst()
                .orElse(0);
    }

    private void leaveOtherTables(String playerId, int targetTableId) {
        for (TableState targetTable : tables.values()) {
            if (targetTable.tableId != targetTableId && targetTable.players.containsKey(playerId)) {
                leave(playerId, targetTable.tableId);
            }
        }
    }

    private TableState table(int tableId) {
        TableState targetTable = tables.get(tableId);
        if (targetTable == null) {
            throw new IllegalArgumentException("table must be between 1 and " + TABLE_COUNT);
        }
        return targetTable;
    }

    private void ensureInTable(TableState targetTable, String playerId) {
        if (!targetTable.players.containsKey(playerId)) {
            throw new IllegalStateException("player is not at table");
        }
    }

    private void ensureCanAct(TableState targetTable, String playerId) {
        ensureInTable(targetTable, playerId);
        if (!targetTable.started || targetTable.finished) {
            throw new IllegalStateException("game is not active");
        }
        PokerRoomPlayer player = targetTable.players.get(playerId);
        if (player.folded()) {
            throw new IllegalStateException("player already folded");
        }
        if (targetTable.currentBet == 0) {
            if (!playerId.equals(targetTable.currentAggressorId)) {
                throw new IllegalStateException("waiting for " + targetTable.currentAggressorId + " to bet");
            }
            return;
        }
        if (playerId.equals(targetTable.currentAggressorId)) {
            throw new IllegalStateException("waiting for other players to call");
        }
        if (player.roundBet() == targetTable.currentBet) {
            throw new IllegalStateException("player already matched current bet");
        }
    }

    private boolean canStart(TableState targetTable) {
        if (targetTable.started || targetTable.players.size() < 2) {
            return false;
        }
        return targetTable.players.values().stream().allMatch(PokerRoomPlayer::ready);
    }

    private String hostId(TableState targetTable) {
        return targetTable.players.keySet().stream().findFirst().orElse("");
    }

    private void dealInitialCards(TableState targetTable) {
        targetTable.deck.clear();
        targetTable.deck.addAll(newDeck());
        Collections.shuffle(targetTable.deck);
        targetTable.communityCards.clear();
        if (targetTable.dealerId.isBlank() || !targetTable.players.containsKey(targetTable.dealerId)) {
            targetTable.dealerId = hostId(targetTable);
        }
        targetTable.pot = targetTable.players.size() * ANTE_CHIPS;
        targetTable.currentBet = 0;
        targetTable.finished = false;
        targetTable.winnerId = "";

        List<String> playerIds = new ArrayList<>(targetTable.players.keySet());
        for (String playerId : playerIds) {
            PokerRoomPlayer existingPlayer = targetTable.players.get(playerId);
            List<String> holeCards = List.of(draw(targetTable.deck), draw(targetTable.deck));
            targetTable.players.put(playerId, new PokerRoomPlayer(playerId, true, holeCards, false, ANTE_CHIPS, 0, false, existingPlayer.score() - ANTE_CHIPS));
        }

        targetTable.communityCards.add(draw(targetTable.deck));
        targetTable.communityCards.add(draw(targetTable.deck));
        resetBettingRound(targetTable);
        targetTable.message = "游戏开始，请 " + targetTable.currentAggressorId + " 主动押注";
    }

    private void resetGame(TableState targetTable) {
        targetTable.started = false;
        targetTable.finished = false;
        targetTable.communityCards.clear();
        targetTable.deck.clear();
        targetTable.dealerId = "";
        targetTable.currentAggressorId = "";
        targetTable.currentTurnId = "";
        targetTable.winnerId = "";
        targetTable.message = "";
        targetTable.aggressorOrder.clear();
        targetTable.completedAggressorIds.clear();
        targetTable.pot = 0;
        targetTable.currentBet = 0;
    }

    private List<String> newDeck() {
        List<String> newDeck = new ArrayList<>();
        List<String> suits = List.of("♠", "♥", "♦", "♣");
        List<String> ranks = List.of("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K");
        for (String suit : suits) {
            for (String rank : ranks) {
                newDeck.add(rank + suit);
            }
        }
        return newDeck;
    }

    private String draw(List<String> targetDeck) {
        return targetDeck.remove(targetDeck.size() - 1);
    }

    private void advanceAfterAction(TableState targetTable) {
        List<PokerRoomPlayer> activePlayers = activePlayers(targetTable);
        if (activePlayers.size() == 1) {
            finish(targetTable, activePlayers.get(0).playerId(), activePlayers.get(0).playerId() + " 获胜，赢得底池 " + targetTable.pot + " 筹码");
            return;
        }

        if (isCurrentAggressorFolded(targetTable)) {
            completeCurrentAggressorStage(targetTable);
            return;
        }

        if (isAggressorStageComplete(targetTable)) {
            completeCurrentAggressorStage(targetTable);
            return;
        }

        targetTable.currentTurnId = "";
        targetTable.message = targetTable.message + "，等待剩余玩家跟注或弃牌";
    }

    private boolean isAggressorStageComplete(TableState targetTable) {
        if (targetTable.currentBet == 0) {
            return false;
        }
        return targetTable.players.values().stream()
                .filter(player -> !player.folded())
                .allMatch(player -> player.roundBet() == targetTable.currentBet);
    }

    private boolean isCurrentAggressorFolded(TableState targetTable) {
        PokerRoomPlayer player = targetTable.players.get(targetTable.currentAggressorId);
        return player == null || player.folded();
    }

    private void completeCurrentAggressorStage(TableState targetTable) {
        String previousMessage = targetTable.message;
        if (!targetTable.currentAggressorId.isBlank() && !targetTable.completedAggressorIds.contains(targetTable.currentAggressorId)) {
            targetTable.completedAggressorIds.add(targetTable.currentAggressorId);
        }

        if (isBettingRoundComplete(targetTable)) {
            if (targetTable.communityCards.size() < 5) {
                targetTable.communityCards.add(draw(targetTable.deck));
                resetBettingRound(targetTable);
                targetTable.message = appendMessage(previousMessage, "本轮完成，发出新的公共牌，请 " + targetTable.currentAggressorId + " 主动押注");
            } else {
                finishByShowdown(targetTable);
            }
            return;
        }

        resetAggressorStage(targetTable, nextIncompleteAggressorId(targetTable));
        targetTable.message = appendMessage(previousMessage, "进入 " + targetTable.currentAggressorId + " 的主动押注阶段");
    }

    private String appendMessage(String previousMessage, String nextMessage) {
        return previousMessage == null || previousMessage.isBlank() ? nextMessage : previousMessage + "，" + nextMessage;
    }

    private boolean isBettingRoundComplete(TableState targetTable) {
        return activePlayers(targetTable).stream()
                .map(PokerRoomPlayer::playerId)
                .allMatch(targetTable.completedAggressorIds::contains);
    }

    private void resetBettingRound(TableState targetTable) {
        targetTable.aggressorOrder.clear();
        targetTable.completedAggressorIds.clear();
        for (String playerId : roomPlayerIdsStartingFrom(targetTable, targetTable.dealerId)) {
            PokerRoomPlayer player = targetTable.players.get(playerId);
            if (player != null && !player.folded()) {
                targetTable.aggressorOrder.add(player.playerId());
            }
        }
        resetAggressorStage(targetTable, targetTable.aggressorOrder.isEmpty() ? "" : targetTable.aggressorOrder.get(0));
    }

    private void resetAggressorStage(TableState targetTable, String aggressorId) {
        targetTable.currentBet = 0;
        for (PokerRoomPlayer player : new ArrayList<>(targetTable.players.values())) {
            targetTable.players.put(player.playerId(), copyPlayer(player, player.ready(), player.holeCards(), player.folded(), player.chipsCommitted(), 0, false, player.score()));
        }
        targetTable.currentAggressorId = aggressorId;
        targetTable.currentTurnId = aggressorId;
    }

    private List<PokerRoomPlayer> activePlayers(TableState targetTable) {
        return targetTable.players.values().stream()
                .filter(player -> !player.folded())
                .toList();
    }

    private String nextRoomPlayerId(TableState targetTable, String playerId) {
        List<String> playerIds = new ArrayList<>(targetTable.players.keySet());
        if (playerIds.isEmpty()) {
            return "";
        }
        int startIndex = Math.max(0, playerIds.indexOf(playerId));
        return playerIds.get((startIndex + 1) % playerIds.size());
    }

    private List<String> roomPlayerIdsStartingFrom(TableState targetTable, String playerId) {
        List<String> playerIds = new ArrayList<>(targetTable.players.keySet());
        if (playerIds.isEmpty()) {
            return List.of();
        }
        int startIndex = Math.max(0, playerIds.indexOf(playerId));
        List<String> orderedPlayerIds = new ArrayList<>();
        for (int offset = 0; offset < playerIds.size(); offset++) {
            orderedPlayerIds.add(playerIds.get((startIndex + offset) % playerIds.size()));
        }
        return orderedPlayerIds;
    }

    private String nextIncompleteAggressorId(TableState targetTable) {
        for (String playerId : targetTable.aggressorOrder) {
            PokerRoomPlayer player = targetTable.players.get(playerId);
            if (player != null && !player.folded() && !targetTable.completedAggressorIds.contains(playerId)) {
                return playerId;
            }
        }
        for (PokerRoomPlayer player : activePlayers(targetTable)) {
            if (!targetTable.completedAggressorIds.contains(player.playerId())) {
                return player.playerId();
            }
        }
        return "";
    }

    private String nextPendingCallerId(TableState targetTable) {
        if (targetTable.currentBet == 0) {
            return targetTable.currentAggressorId;
        }

        for (String playerId : targetTable.aggressorOrder) {
            PokerRoomPlayer player = targetTable.players.get(playerId);
            if (player != null
                    && !player.folded()
                    && !player.playerId().equals(targetTable.currentAggressorId)
                    && player.roundBet() != targetTable.currentBet) {
                return player.playerId();
            }
        }
        return "";
    }

    private void finishByShowdown(TableState targetTable) {
        String bestPlayerId = "";
        HandValue bestHand = null;
        for (PokerRoomPlayer player : activePlayers(targetTable)) {
            HandValue hand = evaluateBestHand(player.holeCards(), targetTable.communityCards);
            if (bestHand == null || hand.compareTo(bestHand) > 0) {
                bestHand = hand;
                bestPlayerId = player.playerId();
            }
        }
        String handName = bestHand == null ? "未知牌型" : bestHand.name();
        finish(targetTable, bestPlayerId, bestPlayerId + " 以" + handName + "获胜，赢得底池 " + targetTable.pot + " 筹码");
    }

    static int compareBestHands(List<String> leftCards, List<String> rightCards) {
        return evaluateBestHand(leftCards, List.of()).compareTo(evaluateBestHand(rightCards, List.of()));
    }

    static String bestHandName(List<String> holeCards, List<String> communityCards) {
        return evaluateBestHand(holeCards, communityCards).name();
    }

    static List<String> bestHandCards(List<String> holeCards, List<String> communityCards) {
        return evaluateBestHand(holeCards, communityCards).cards();
    }

    private static HandValue evaluateBestHand(List<String> holeCards, List<String> communityCards) {
        List<String> cards = new ArrayList<>();
        cards.addAll(holeCards);
        cards.addAll(communityCards);
        if (cards.size() < 5) {
            throw new IllegalArgumentException("at least five cards are required");
        }

        HandValue bestHand = null;
        for (int first = 0; first < cards.size() - 4; first++) {
            for (int second = first + 1; second < cards.size() - 3; second++) {
                for (int third = second + 1; third < cards.size() - 2; third++) {
                    for (int fourth = third + 1; fourth < cards.size() - 1; fourth++) {
                        for (int fifth = fourth + 1; fifth < cards.size(); fifth++) {
                            HandValue hand = evaluateFiveCards(List.of(
                                    cards.get(first),
                                    cards.get(second),
                                    cards.get(third),
                                    cards.get(fourth),
                                    cards.get(fifth)
                            ));
                            if (bestHand == null || hand.compareTo(bestHand) > 0) {
                                bestHand = hand;
                            }
                        }
                    }
                }
            }
        }
        return bestHand;
    }

    private static HandValue evaluateFiveCards(List<String> cards) {
        Map<Integer, Integer> rankCounts = new LinkedHashMap<>();
        List<Integer> ranks = new ArrayList<>();
        List<String> suits = new ArrayList<>();
        for (String card : cards) {
            int rank = rankScore(card);
            ranks.add(rank);
            suits.add(card.substring(card.length() - 1));
            rankCounts.put(rank, rankCounts.getOrDefault(rank, 0) + 1);
        }

        ranks.sort(Collections.reverseOrder());
        boolean flush = suits.stream().distinct().count() == 1;
        int straightHigh = straightHigh(ranks);
        if (flush && straightHigh > 0) {
            return new HandValue(8, List.of(straightHigh), straightHigh == 14 ? "皇家同花顺" : "同花顺", cards);
        }

        List<Integer> fourRanks = ranksByCount(rankCounts, 4);
        if (!fourRanks.isEmpty()) {
            return new HandValue(7, List.of(fourRanks.get(0), highestRankWithCount(rankCounts, 1)), "四条", cards);
        }

        List<Integer> threeRanks = ranksByCount(rankCounts, 3);
        List<Integer> pairRanks = ranksByCount(rankCounts, 2);
        if (!threeRanks.isEmpty() && (!pairRanks.isEmpty() || threeRanks.size() > 1)) {
            int pairRank = !pairRanks.isEmpty() ? pairRanks.get(0) : threeRanks.get(1);
            return new HandValue(6, List.of(threeRanks.get(0), pairRank), "葫芦", cards);
        }

        if (flush) {
            return new HandValue(5, ranks, "同花", cards);
        }

        if (straightHigh > 0) {
            return new HandValue(4, List.of(straightHigh), "顺子", cards);
        }

        if (!threeRanks.isEmpty()) {
            List<Integer> tieBreakers = new ArrayList<>();
            tieBreakers.add(threeRanks.get(0));
            tieBreakers.addAll(highestRanksExcluding(rankCounts, List.of(threeRanks.get(0)), 2));
            return new HandValue(3, tieBreakers, "三条", cards);
        }

        if (pairRanks.size() >= 2) {
            List<Integer> tieBreakers = new ArrayList<>();
            tieBreakers.add(pairRanks.get(0));
            tieBreakers.add(pairRanks.get(1));
            tieBreakers.addAll(highestRanksExcluding(rankCounts, List.of(pairRanks.get(0), pairRanks.get(1)), 1));
            return new HandValue(2, tieBreakers, "两对", cards);
        }

        if (pairRanks.size() == 1) {
            List<Integer> tieBreakers = new ArrayList<>();
            tieBreakers.add(pairRanks.get(0));
            tieBreakers.addAll(highestRanksExcluding(rankCounts, List.of(pairRanks.get(0)), 3));
            return new HandValue(1, tieBreakers, "一对", cards);
        }

        return new HandValue(0, ranks, "高牌", cards);
    }

    private static List<Integer> ranksByCount(Map<Integer, Integer> rankCounts, int count) {
        return rankCounts.entrySet().stream()
                .filter(entry -> entry.getValue() == count)
                .map(Map.Entry::getKey)
                .sorted(Collections.reverseOrder())
                .toList();
    }

    private static int highestRankWithCount(Map<Integer, Integer> rankCounts, int count) {
        return ranksByCount(rankCounts, count).stream().findFirst().orElse(0);
    }

    private static List<Integer> highestRanksExcluding(Map<Integer, Integer> rankCounts, List<Integer> excludedRanks, int limit) {
        return rankCounts.keySet().stream()
                .filter(rank -> !excludedRanks.contains(rank))
                .sorted(Collections.reverseOrder())
                .limit(limit)
                .toList();
    }

    private static int straightHigh(List<Integer> ranks) {
        List<Integer> uniqueRanks = new ArrayList<>(ranks.stream().distinct().toList());
        if (uniqueRanks.contains(14)) {
            uniqueRanks.add(1);
        }
        for (int high = 14; high >= 5; high--) {
            int targetHigh = high;
            boolean found = true;
            for (int offset = 0; offset < 5; offset++) {
                if (!uniqueRanks.contains(targetHigh - offset)) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return high;
            }
        }
        return 0;
    }

    private static int rankScore(String card) {
        String rank = card.substring(0, card.length() - 1);
        return switch (rank) {
            case "A" -> 14;
            case "K" -> 13;
            case "Q" -> 12;
            case "J" -> 11;
            default -> Integer.parseInt(rank);
        };
    }

    private record HandValue(int category, List<Integer> tieBreakers, String name, List<String> cards) implements Comparable<HandValue> {
        @Override
        public int compareTo(HandValue other) {
            int categoryCompare = Integer.compare(category, other.category);
            if (categoryCompare != 0) {
                return categoryCompare;
            }
            int size = Math.min(tieBreakers.size(), other.tieBreakers.size());
            for (int index = 0; index < size; index++) {
                int tieBreakerCompare = Integer.compare(tieBreakers.get(index), other.tieBreakers.get(index));
                if (tieBreakerCompare != 0) {
                    return tieBreakerCompare;
                }
            }
            return Integer.compare(tieBreakers.size(), other.tieBreakers.size());
        }
    }

    private void finish(TableState targetTable, String winnerId, String message) {
        PokerRoomPlayer winner = targetTable.players.get(winnerId);
        if (winner != null) {
            targetTable.players.put(winnerId, copyPlayer(winner, winner.ready(), winner.holeCards(), winner.folded(), winner.chipsCommitted(), winner.roundBet(), winner.acted(), winner.score() + targetTable.pot));
        }
        targetTable.finished = true;
        targetTable.winnerId = winnerId;
        targetTable.currentAggressorId = "";
        targetTable.currentTurnId = "";
        targetTable.currentBet = 0;
        targetTable.message = message;
    }

    private PokerRoomPlayer copyPlayer(
            PokerRoomPlayer player,
            boolean ready,
            List<String> holeCards,
            boolean folded,
            int chipsCommitted,
            int roundBet,
            boolean acted,
            int score
    ) {
        return new PokerRoomPlayer(player.playerId(), ready, holeCards, folded, chipsCommitted, roundBet, acted, score);
    }

    private static final class TableState {
        private final int tableId;
        private final Map<String, PokerRoomPlayer> players = new LinkedHashMap<>();
        private final List<String> communityCards = new ArrayList<>();
        private final List<String> deck = new ArrayList<>();
        private final List<String> aggressorOrder = new ArrayList<>();
        private final List<String> completedAggressorIds = new ArrayList<>();
        private boolean started;
        private boolean finished;
        private String dealerId = "";
        private String currentAggressorId = "";
        private String currentTurnId = "";
        private String winnerId = "";
        private String message = "";
        private int pot;
        private int currentBet;

        private TableState(int tableId) {
            this.tableId = tableId;
        }
    }
}
