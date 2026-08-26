# RocksDB Package Guide

本包是项目的 RocksDB 系统：持有唯一 RocksDB 实例，并提供玩家积分和每局结算历史的存储，以及 `RocksDBController` 依赖的查询入口。
用法是原生的程序化配置，不依赖任何配置文件。

> **当前状态：系统已就绪，但暂未接入业务。** 游戏数据（在线玩家、房间、牌局、积分）仍全部在内存中，
> 本包存储不会写入任何真实业务数据，`/rocksdb` 接口查询结果为空。

## 主要职责

- `RocksDBStore.java`：唯一 `@Component` 实例持有者。直接通过 `Options` 打开数据库（目录默认 `data/rocksdb`，可用 `rocksdb.path` 配置），提供底层 `get/put/delete`、前缀遍历、key 计数、属性读取、强制刷盘。所有业务存储复用这一个实例，写入使用 `WriteOptions.setSync(true)` 同步刷盘。
- `RocksDBScoreStore.java`：玩家积分存储（`@Component`），key 为 `score:<playerId>`，value 为积分十进制字符串；`allScores()` 返回全部玩家积分。
- `RocksDBHandHistoryStore.java`：结算历史存储（`@Component`），`hand:seq` 自增计数，`hand:<seq>` 存 Jackson JSON；`recentSettlements` 从最新往回读。

## 存储布局

| key 前缀 | 值 | 设计写入方 |
| --- | --- | --- |
| `score:<playerId>` | 积分（十进制） | 入座读取、离桌/结算写回（待接入） |
| `hand:seq` | 结算记录自增编号 | 每局结算时（待接入） |
| `hand:<seq>` | 结算记录 JSON | 每局结算时（待接入） |

## 设计约定

- 当前 `RocksDBController` 直接注入 `RocksDBStore` / `RocksDBScoreStore` / `RocksDBHandHistoryStore` 做查询。
- 后续接入业务时，在 `PokerRoomService` 的入座/结算/离桌处调用 `RocksDBScoreStore` 与 `RocksDBHandHistoryStore` 即可；若希望业务与存储解耦，可以再加一层接口。
- key 设计使用可读前缀（`score:`、`hand:`），方便 `/rocksdb/keys` 和前缀遍历调试。
- 新增存储类型时优先复用现有 `RocksDBStore` 实例，不要在别处再开一个 RocksDB。

## 修改注意

- `RocksDBStore` 是原生资源（JNI），关闭逻辑在 `@PreDestroy close()`；直接 new 的测试必须手动 `close()`。
- 接入业务后注意语义：只有“结算结束的牌局”和“离桌”写回积分；牌局中途重启，本局押注筹码相当于退回。
- `rocksdbjni` 自带各平台 native 库，新增系统依赖请同步检查 pom.xml 版本。
- 修改存储逻辑后运行 `mvn test`，重点看 `rocksdb` 包测试。
