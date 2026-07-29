# RankSystem Package Guide

本目录是项目的核心业务包，包含全局内存状态、登录状态、德州扑克房间逻辑、WebSocket 实时推送和应用生命周期任务。

## 主要职责

- `AppState.java` 负责持有内存单例服务，并把玩家、桌子、房间快照序列化为 JSON。
- `LoginService.java` 负责玩家登录、token 校验、在线状态、心跳时间和业务活跃时间。
- `PokerRoomService.java` 负责德州扑克桌子、入座、准备、发牌、押注、弃牌、结算、轮庄和牌型计算。
- `RealtimeEndpoint.java` 是 `/ws` WebSocket 端点，负责绑定玩家连接、ping/pong 心跳和分层广播。
- `OnlinePlayerCleaner.java` 是后台清理任务，按业务活跃时间清理空闲玩家；已进入桌面的玩家不会被清理。
- `AppLifecycleListener.java` 在 Tomcat 启停应用时启动/停止 WebSocket 心跳和在线玩家清理线程。
- `PlayerStatus.java` 定义玩家状态文案和德州扑克模块状态判断。

## 状态约定

玩家状态分为：

- `在总大厅`
- `在德州扑克大厅`
- `在德州扑克桌面`
- `在德州扑克中`

如果未来新增游戏，优先沿用“总大厅 -> 游戏大厅 -> 游戏桌面/房间 -> 游戏中”的层级。

## 广播约定

广播是包含关系：

- `broadcastGlobalLobby()` 通知所有在线玩家。
- `broadcastPokerLobby()` 通知德州扑克模块内玩家，包括大厅、桌面和游戏中玩家。
- `broadcastPokerTable(tableId)` 只通知指定桌子的玩家。

业务接口不要直接遍历 WebSocket session，优先通过 `ApiResult` 声明广播目标，再由 `BaseServlet` 分发。

## 修改注意

- 内存数据没有数据库持久化，Tomcat 重启会重置玩家、桌子、牌局和积分。
- 登录 token 是单端机制，后登录会挤掉先登录。
- 心跳和业务活跃时间语义不同：`/heartbeat` 只刷新心跳，业务操作刷新 `lastActionAtMillis`。
- 牌型比较对象在 `model/HandCategory.java` 和 `model/HandValue.java`，不要再在 `PokerRoomService` 里新增内部牌型模型。
- 修改业务逻辑后至少运行 `mvn test`。
