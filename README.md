# RankSystem

RankSystem 是一个基于 Java + Spring Boot 的 Web Demo 项目。当前主要功能是一个内存版德州扑克房间系统，用来演示登录、房间、桌子、实时广播、押注、摊牌和积分结算流程。

数据暂时全部保存在内存中，没有接入数据库。应用重启后，在线玩家、房间状态、牌局状态和积分都会重置。

## 当前功能

- 伪登录系统：玩家输入唯一 ID 登录，登出后回到登录界面。
- 总大厅：登录后进入大厅，可以选择德州扑克。
- 右侧玩家状态区：非登录界面显示登出按钮、在线玩家状态和所在桌号；游戏进行中会隐藏该区域。
- 德州扑克桌子大厅：进入德州扑克后显示 10 张桌子，桌卡展示人数、房主、游戏状态、玩家准备状态和是否正在游戏。
- 玩家状态层级：当前区分在总大厅、在德州扑克大厅、在德州扑克桌面、在德州扑克中。
- 准备机制：玩家准备后不能切换桌子，取消准备后可以重新切换。
- 房主机制：桌面和座位上都会标识当前房主。
- 实时广播：玩家登录、入座、准备、押注、弃牌等操作会通过 WebSocket 推送给相关在线页面。
- 分层广播：支持全大厅广播、德州扑克大厅广播、桌面内广播。
- 心跳机制：前端定时 POST `/heartbeat`，WebSocket 连接使用服务端 ping 和客户端 pong 检测断线。
- 游戏流程：押底、发牌、押注、跟注、弃牌、公共牌推进、最终比牌。
- 积分系统：押底和押注会扣积分，赢家获得底池。
- 摊牌展示：最后比牌阶段展示每位玩家的底牌、最终牌型和组成牌型的 5 张成牌。
- 牌型模型：内部使用 `PlayingCard`、`CardRank`、`CardSuit` 表达扑克牌，使用 `HandCategory` 表示牌型强度和展示名，`HandValue` 统一比较牌型和关键点数。

## 技术栈

- Java 21
- Maven
- Spring Boot 3.2.5
- Spring MVC（REST 接口）
- Jakarta WebSocket（JSR-356）+ ServerEndpointExporter
- Jackson JSON
- JUnit 5

迁移过程与前后对比见 [SPRING_BOOT_MIGRATION.md](SPRING_BOOT_MIGRATION.md)。

## 项目结构

```text
pom.xml
README.md
SPRING_BOOT_MIGRATION.md      # Spring Boot 迁移说明
src/main/java/com/zqyyz/ranksystem/
  AGENTS.md                 # 主业务包 AI 导读
  RankSystemApplication.java # Spring Boot 启动类
  AppInitializer.java        # 启动初始化（WebSocket 心跳）
  AppState.java              # 全局内存状态和 JSON 输出（Spring Bean）
  LoginService.java          # 在线玩家状态管理（Service）
  PokerRoomService.java      # 德州扑克核心逻辑（Service）
  OnlinePlayerCleaner.java   # 空闲玩家定时清理（@Scheduled）
  PlayerStatus.java          # 玩家状态常量
  RealtimeEndpoint.java      # /ws WebSocket 连接、心跳和广播
  SpringContextHolder.java   # Spring 上下文持有器
  GlobalExceptionHandler.java# 全局异常处理
  WebConfig.java             # Web 配置（WebSocket 端点注册）
  controller/
    AuthController.java      # /login、/logout、/heartbeat
    PlayerController.java    # /players、/state
    PokerRoomController.java # /poker-room 系列接口
    PokerTableController.java# /poker-tables
src/main/java/com/zqyyz/ranksystem/model/
  AGENTS.md                 # 模型包 AI 导读
  CardRank.java              # 扑克牌点数枚举
  CardSuit.java              # 扑克牌花色枚举
  HandCategory.java          # 德州扑克牌型枚举
  HandValue.java             # 一手牌的评估结果和比较逻辑
  PlayerSession.java         # 在线玩家会话数据
  PlayingCard.java           # 后端内部扑克牌模型
  PokerRoomPlayer.java       # 房间玩家数据
  PokerRoomSnapshot.java     # 房间快照
  PokerTableSummary.java     # 桌子摘要
src/main/java/com/zqyyz/ranksystem/util/
  AGENTS.md                 # 工具包 AI 导读
  CollectionUtil.java        # 集合字典序比较工具
src/main/resources/
  application.properties     # 端口、静态资源配置
  static/index.html          # 前端页面
cards_54/                    # 备用扑克牌图片素材；当前前端没有引用
src/test/java/com/zqyyz/ranksystem/
  LoginServiceTest.java
  PokerRoomServiceTest.java
```

## 图片资源说明

当前页面里的扑克牌由前端 DOM 和文字渲染，不依赖图片文件。

已确认不再使用、可以删除的旧图片：

```text
A_red_peach.png
src/main/webapp/images/A_red_peach.png
```

备用素材：

```text
cards_54/png/
cards_54/svg/
```

这两组扑克牌素材当前没有被代码引用。如果后续要把牌面改成真实图片，建议优先使用 `cards_54/svg/`；如果继续保持文字牌面，整个 `cards_54/` 可以删除。

## 本地运行

开发模式运行：

```bash
mvn spring-boot:run
```

打包并运行可执行 JAR：

```bash
mvn clean package
java -jar target/ranksystem.jar
```

访问地址：

```text
http://localhost:8081/
```

## 常用接口

主要页面直接访问：

```text
GET /
```

常用 HTTP 接口：

```text
POST /login
POST /logout
POST /heartbeat
GET  /players
POST /state
GET  /poker-tables
GET  /poker-room
POST /poker-room/join
POST /poker-room/ready
POST /poker-room/unready
POST /poker-room/start
POST /poker-room/next
POST /poker-room/leave
POST /poker-room/fold
POST /poker-room/bet
```

WebSocket：

```text
ws://localhost:8081/ws?id=玩家ID&token=登录token
```

如果通过 HTTPS 访问，WebSocket 地址应使用 `wss://`。

## 实时同步和心跳

本项目有两套连接状态机制：

- HTTP 心跳：前端每 30 秒 POST `/heartbeat`，只刷新 `lastHeartbeatAtMillis`，不算业务活跃操作。
- WebSocket 心跳：服务端每 30 秒发送 ping，连续 3 次未收到 pong 会关闭连接。

空闲清理基于 `lastActionAtMillis` 判断，玩家进入桌面后不会被空闲清理踢出。业务操作会刷新 `lastActionAtMillis`，单纯心跳不会刷新业务活跃时间。

广播范围是包含关系：

```text
全大厅广播 > 德州扑克大厅广播 > 某张桌面广播
```

当前语义：

- 全大厅广播：所有在线玩家。
- 德州扑克大厅广播：所有德州扑克模块内玩家，包括德州扑克大厅、桌面和游戏中。
- 桌面内广播：指定桌子的玩家。

## 牌型比较

德州扑克牌型计算在 `PokerRoomService` 中完成：

```text
2 张底牌 + 公共牌
  -> 枚举所有 5 张组合
  -> evaluateFiveCards 判断每组牌型
  -> HandValue.compareTo 选出最大牌型
```

房间状态和 JSON 当前仍使用 `"A♠"`、`"10♦"` 这种字符串，牌型计算内部会先解析成 `PlayingCard`。牌型强度由 `HandCategory.strength()` 显式定义，不依赖 `enum.ordinal()`。同牌型大小通过 `HandValue.tieBreakers` 按字典序比较，比较细节复用 `CollectionUtil.compareLexicographically(...)`。

## 测试

运行全部测试：

```bash
mvn test
```

当前测试覆盖：

- 登录和在线玩家状态
- 房间加入、准备、取消准备
- 10 张桌子系统
- 房主和庄家流转
- 押注、跟注、弃牌
- 四轮押注和公共牌推进
- 正式德州扑克牌型比较
- 摊牌时底牌、牌型和成牌展示数据
- 积分扣除和底池结算

## AI 导读文件

项目在关键 Java 包下放了 `AGENTS.md`，用于帮助 AI 或后续维护者快速理解目录职责：

```text
src/main/java/com/zqyyz/ranksystem/AGENTS.md
src/main/java/com/zqyyz/ranksystem/model/AGENTS.md
src/main/java/com/zqyyz/ranksystem/servlet/AGENTS.md
src/main/java/com/zqyyz/ranksystem/util/AGENTS.md
```

修改对应包代码前，可以先阅读同目录下的 `AGENTS.md`。

## 分享给别人访问

`localhost` 只能自己电脑访问。朋友不能直接打开：

```text
http://localhost:8081/RankSystem/
```

如果朋友和你在同一个 Wi-Fi，可以先查本机局域网 IP：

```bash
ipconfig getifaddr en0
```

假设输出是 `192.168.1.23`，朋友访问：

```text
http://192.168.1.23:8081/RankSystem/
```

如果朋友不在同一个网络，可以使用内网穿透工具，例如 ngrok：

```bash
brew install ngrok
ngrok http 8081
```

ngrok 会生成一个公网地址，把这个地址后面加上 `/RankSystem/` 发给朋友即可。

## 当前限制

- 没有数据库，所有数据都在内存里。
- 没有真实账号系统，玩家 ID 由前端手动输入。
- 没有筹码余额上限校验，积分可以为负数。
- 游戏规则仍是 Demo 版本，适合学习和演示，不适合作为正式线上游戏。
