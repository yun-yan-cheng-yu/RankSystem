# Servlet Package Guide

本包是 HTTP API 层，负责请求路由、参数读取、鉴权、异常映射、响应输出和广播目标声明。

## 核心结构

- `BaseServlet.java` 统一处理 `doGet`、`doPost`，并调用子类的 `run(RequestContext context)`。
- `RequestContext.java` 封装请求路径、HTTP 方法、玩家 ID、token 和常用参数解析。
- `ApiResult.java` 封装 HTTP 状态码、响应体、Content-Type 和广播目标。
- `BroadcastScope.java` / `BroadcastTarget.java` 定义广播范围。
- `AuthServlet.java` 处理 `/login`、`/logout`、`/heartbeat`。
- `PlayerServlet.java` 处理 `/players`、`/state`。
- `PokerTableServlet.java` 处理 `/poker-tables`。
- `PokerRoomServlet.java` 处理 `/poker-room` 及桌内操作。
- `HelloServlet.java` 是简单示例接口。

## run 风格

Servlet 子类不要重写 `doGet` 或 `doPost`，只重写：

```java
protected ApiResult run(RequestContext context)
```

根据 `context.isGet()`、`context.isPost()` 和 `context.path()` 分发业务逻辑。

## 鉴权和活跃时间

- 只校验 token：调用 `validateSession(context)`。
- 客户端心跳：调用 `validateHeartbeat(context)`，只刷新 `lastHeartbeatAtMillis`。
- 业务操作：调用 `validateAction(context)`，刷新 `lastActionAtMillis`。

不要把心跳当作业务活跃时间。

## 广播目标

- 登录、登出、状态切换、空闲清理使用全局广播。
- 进桌、准备、取消准备、开始、下一局、离桌使用德州扑克大厅广播。
- 押注、弃牌使用桌面内广播。

接口返回时通过 `ApiResult.withGlobalLobbyBroadcast()`、`withPokerLobbyBroadcast()` 或 `withPokerTableBroadcast(tableId)` 声明。

## 修改注意

- 响应体默认使用 JSON：`application/json; charset=utf-8`。
- `IllegalArgumentException` 会映射为 400。
- session 类错误会映射为 401。
- 其他业务状态冲突会映射为 409。
- 新增接口后同步 README 的常用接口列表。
