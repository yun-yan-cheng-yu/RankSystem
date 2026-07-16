# Lobby Poker Sidebar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the simple login app with a lobby, a poker screen, player statuses, and a persistent right-side function area outside the login screen.

**Architecture:** `LoginService` changes from an online-player set to an in-memory player-session map. `LoginServlet` exposes login, logout, status update, all-player list, and game-filtered player list. `index.html` remains a single-page app with internal views for login, lobby, and poker.

**Tech Stack:** Java 17, Jakarta Servlet, JUnit 5, Tomcat 11, plain HTML/CSS/JavaScript.

---

### Task 1: Player Status Model

**Files:**
- Modify: `src/main/java/com/example/ranksystem/LoginService.java`
- Create: `src/main/java/com/example/ranksystem/PlayerSession.java`
- Create: `src/main/java/com/example/ranksystem/PlayerStatus.java`
- Modify: `src/test/java/com/example/ranksystem/LoginServiceTest.java`

- [ ] Write failing tests for lobby login status, status update, player listing with status, and game A filtering.
- [ ] Implement a map-backed in-memory session store.
- [ ] Run `mvn test`.

### Task 2: Servlet Endpoints

**Files:**
- Modify: `src/main/java/com/example/ranksystem/LoginServlet.java`

- [ ] Keep `POST /login` and `POST /logout`.
- [ ] Add `POST /state?id=...&state=...`.
- [ ] Return all player sessions from `GET /players`.
- [ ] Return game A player sessions from `GET /players?game=A`.

### Task 3: Frontend Views

**Files:**
- Modify: `src/main/webapp/index.html`

- [ ] Login screen stays standalone.
- [ ] Lobby screen shows "德州扑克" and "其他".
- [ ] Poker screen shows "显示在德州扑克功能的所有人" and "显示那张扑克牌".
- [ ] Right sidebar appears on lobby and poker screens, with logout and all-player status list.
- [ ] Refresh restores the current player and view.

### Task 4: Deploy And Verify

**Files:**
- Build output: `target/RankSystem.war`

- [ ] Run `mvn clean package`.
- [ ] Copy `target/RankSystem.war` into Tomcat webapps.
- [ ] Restart Tomcat.
- [ ] Verify login, state switch, all-player list, game A list, and logout with curl.
