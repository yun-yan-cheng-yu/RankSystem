# RankSystem - Spring Boot 迁移

本文件记录 RankSystem 从传统 Jakarta Servlet + WAR 架构迁移到 Spring Boot 的过程与结果。

## 状态：已完成

迁移已成功完成并通过验证：

- `mvn test`：40 个测试全部通过（LoginServiceTest 11 + PokerRoomServiceTest 29）
- 可执行 JAR 启动后，首页、登录、玩家、桌子和房间接口均正常
- WebSocket `/ws` 握手成功并能收到实时快照

## 主要变化

### 架构

| 旧实现 | 新实现 |
| --- | --- |
| `HttpServlet` + `@WebServlet` + WAR | `@RestController` + 可执行 JAR |
| 外部 Tomcat 10 部署 | Spring Boot 内置 Tomcat |
| `@WebListener`（AppLifecycleListener） | `@Component` + `CommandLineRunner`（AppInitializer） |
| 静态 `AppState` 单例 | `@Component` + Spring 依赖注入（保留静态访问器供 WebSocket 使用） |
| `@ServerEndpoint` + 手动 ScheduledExecutorService | `@ServerEndpoint` + `ServerEndpointExporter` Bean；空闲清理改为 `@Scheduled` |
| `jakarta.servlet` provided 依赖 | `spring-boot-starter-web` 内置 Tomcat |

### 代码结构

- `servlet/` 包删除，替换为 `controller/` 包（AuthController、PlayerController、PokerRoomController、PokerTableController）
- `webapp/index.html` 移到 `resources/static/index.html`
- 新增 `RankSystemApplication`、`WebConfig`、`GlobalExceptionHandler`、`SpringContextHolder`
- 核心游戏逻辑保持不变：`PokerRoomService` 从 `main` 分支恢复完整实现（此前迁移中被截断），牌型计算、押注流程、积分结算等逻辑原样保留

## 运行方式

```bash
# 开发模式
mvn spring-boot:run

# 打包运行
mvn clean package
java -jar target/ranksystem.jar
```

访问地址：`http://localhost:8081/`

## 接口一览

- `POST /login`、`POST /logout`、`POST /heartbeat`
- `GET /players`、`POST /state`
- `GET /poker-tables`
- `GET /poker-room`、`POST /poker-room/{join,ready,unready,start,next,leave,fold,bet}`
- WebSocket：`ws://localhost:8081/ws?id=玩家ID&token=登录token`

## 测试

```bash
mvn test
```

覆盖范围：

- 登录和在线玩家状态
- 房间加入、准备、取消准备
- 10 张桌子系统、房主和庄家流转
- 押注、跟注、弃牌、四轮押注和公共牌推进
- 德州扑克牌型比较、摊牌展示、积分扣除和底池结算
