# Memory Login Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a simple in-memory login flow to the existing Tomcat web app.

**Architecture:** A small `LoginService` owns the in-memory player set. `LoginServlet` exposes login, logout, and player-list endpoints. `index.html` switches between the login screen and the main screen in the browser.

**Tech Stack:** Java 17, Jakarta Servlet, JUnit 5, Tomcat 11, plain HTML/CSS/JavaScript.

---

### Task 1: Login Service

**Files:**
- Create: `src/main/java/com/example/ranksystem/LoginService.java`
- Create: `src/test/java/com/example/ranksystem/LoginServiceTest.java`
- Modify: `pom.xml`

- [ ] Add JUnit 5 to `pom.xml`.
- [ ] Write failing tests for login, logout, blank ID rejection, and player listing.
- [ ] Implement `LoginService` with an in-memory concurrent set.
- [ ] Run `mvn test` and confirm tests pass.

### Task 2: Servlet API

**Files:**
- Create: `src/main/java/com/example/ranksystem/LoginServlet.java`

- [ ] Add `POST /login`, `POST /logout`, and `GET /players`.
- [ ] Return `400` when the `id` parameter is blank.
- [ ] Return JSON for all API responses.
- [ ] Verify with `curl` after deployment.

### Task 3: Frontend Flow

**Files:**
- Modify: `src/main/webapp/index.html`

- [ ] Replace the image-only page with a login screen and a main screen.
- [ ] On login, submit the unique ID and switch to the main screen.
- [ ] On logout, submit the current ID and switch back to login.
- [ ] On list players, fetch `/players` and show the online IDs.

### Task 4: Package And Deploy

**Files:**
- Build output: `target/RankSystem.war`

- [ ] Run `mvn clean package`.
- [ ] Copy the WAR to `/opt/homebrew/opt/tomcat/libexec/webapps/RankSystem.war`.
- [ ] Restart Tomcat with `brew services restart tomcat`.
- [ ] Verify `/RankSystem/login`, `/RankSystem/logout`, `/RankSystem/players`, and `/RankSystem/`.
