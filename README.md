# RankSystem

RankSystem 是一个基于 Java + Tomcat 的 Web Demo 项目。当前主要功能是一个内存版德州扑克房间系统，用来演示登录、房间、桌子、实时广播、押注、摊牌和积分结算流程。

数据暂时全部保存在内存中，没有接入数据库。Tomcat 重启后，在线玩家、房间状态、牌局状态和积分都会重置。

## 当前功能

- 伪登录系统：玩家输入唯一 ID 登录，登出后回到登录界面。
- 总大厅：登录后进入大厅，可以选择德州扑克。
- 右侧功能区：非登录界面常驻显示，支持登出、查看当前在线玩家和状态。
- 德州扑克房间：进入德州扑克后显示 10 张桌子，玩家主动选择桌子入座。
- 准备机制：玩家准备后不能切换桌子，取消准备后可以重新切换。
- 房主机制：桌面和座位上都会标识当前房主。
- 实时广播：玩家登录、入座、准备、押注、弃牌等操作会通过 WebSocket 推送给其他在线页面。
- 游戏流程：押底、发牌、押注、跟注、弃牌、公共牌推进、最终比牌。
- 积分系统：押底和押注会扣积分，赢家获得底池。
- 摊牌展示：最后比牌阶段展示每位玩家的底牌、最终牌型和组成牌型的 5 张成牌。

## 技术栈

- Java 17
- Maven
- Tomcat 10+
- Jakarta Servlet 6
- Jakarta WebSocket
- JUnit 5

注意：本项目使用 `jakarta.servlet`，需要 Tomcat 10 或更新版本。Tomcat 9 使用的是 `javax.servlet`，不能直接运行当前版本。

## 项目结构

```text
pom.xml
README.md
src/main/java/com/example/ranksystem/
  AppState.java              # 全局内存状态和 JSON 输出
  HelloServlet.java          # Hello world 示例接口
  LoginServlet.java          # 登录、登出、房间和游戏 HTTP 接口
  LoginService.java          # 在线玩家状态管理
  PokerRoomService.java      # 德州扑克核心逻辑
  PokerRoomPlayer.java       # 房间玩家数据
  PokerRoomSnapshot.java     # 房间快照
  PokerTableSummary.java     # 桌子摘要
  RealtimeEndpoint.java      # WebSocket 广播
src/main/webapp/
  index.html                 # 前端页面
src/test/java/com/example/ranksystem/
  LoginServiceTest.java
  PokerRoomServiceTest.java
```

## 本地运行

先确认本机有 Maven：

```bash
mvn -v
```

打包：

```bash
mvn clean package
```

生成的 WAR 文件：

```text
target/RankSystem.war
```

如果 Tomcat 是通过 Homebrew 安装的，可以直接部署并重启：

```bash
cp target/RankSystem.war /opt/homebrew/opt/tomcat/libexec/webapps/RankSystem.war
brew services restart tomcat
```

访问地址：

```text
http://localhost:8081/RankSystem/
```

如果你的 Tomcat 使用默认端口，地址可能是：

```text
http://localhost:8080/RankSystem/
```

## 常用接口

主要页面直接访问：

```text
GET /RankSystem/
```

常用 HTTP 接口：

```text
POST /RankSystem/login
POST /RankSystem/logout
GET  /RankSystem/poker-tables
GET  /RankSystem/poker-room
POST /RankSystem/poker-room/join
POST /RankSystem/poker-room/ready
POST /RankSystem/poker-room/unready
POST /RankSystem/poker-room/start
POST /RankSystem/poker-room/next
POST /RankSystem/poker-room/leave
POST /RankSystem/poker-room/fold
POST /RankSystem/poker-room/bet
```

WebSocket：

```text
ws://localhost:8081/RankSystem/realtime
```

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

