# Model Package Guide

本包存放领域数据模型，不处理 Servlet 请求，也不直接修改全局状态。

## 当前模型

- `PlayerSession.java`：在线玩家和玩家状态。
- `PokerRoomPlayer.java`：桌内玩家数据，包括准备、底牌、弃牌、下注、积分等。
- `PokerRoomSnapshot.java`：单张桌子的完整快照。
- `PokerTableSummary.java`：桌子大厅需要展示的桌子摘要。
- `SettlementRecord.java`：一局结束时的结算记录（时间、桌子、赢家、牌型、底池、每人最终积分），是 `rocksdb` 包结算历史存储的数据模型，当前未接入业务。
- `CardRank.java`：扑克牌点数枚举，包含点数数值和展示文本。
- `CardSuit.java`：扑克牌花色枚举，包含花色符号。
- `PlayingCard.java`：后端内部使用的扑克牌模型，可从 `"A♠"`、`"10♦"` 等字符串解析，也可转回字符串。
- `HandCategory.java`：德州扑克牌型枚举，使用显式 `strength` 比较强度，不使用 `ordinal()`。
- `HandValue.java`：一手牌的评估结果，组合 `HandCategory`、`tieBreakers` 和实际 5 张成牌。

## 设计约定

- 模型优先保持不可变，当前主要使用 `record`。
- 房间状态和 JSON 当前仍使用 `"A♠"` 这类字符串；牌型计算内部优先解析成 `PlayingCard`，不要在牌型逻辑里散落 `substring` 解析。
- 牌型比较遵循“组合优于继承”：`HandValue` 组合 `HandCategory`，不为每种牌型建立子类。
- `HandValue.compareTo()` 只负责比较流程：先比 `category.strength()`，再用 `CollectionUtil.compareLexicographically(...)` 比较 `tieBreakers`。
- 如果需要新增前端展示字段，优先确认它是否属于领域模型；只用于 JSON 展示的字段可以放在 `AppState` 的 view 构造逻辑里。

## 修改注意

- 不要在 model 里依赖 Servlet API。
- 不要在 model 里调用 `AppState` 或服务单例。
- 修改牌型字段时，同步检查 `PokerRoomService.bestHandName(...)`、`bestHandCards(...)` 和前端摊牌展示。
