# Util Package Guide

本包存放不依赖业务状态的通用小工具。

## 当前工具

- `CollectionUtil.compareLexicographically(List<Integer> left, List<Integer> right)`：按字典序逐项比较两个整数列表。

## 使用约定

- util 方法应保持无状态、可测试、无副作用。
- 在 util 包新增方法时必须写清楚注释，至少说明方法用途、核心规则、返回值含义；如果可能抛异常，也要说明异常语义。
- 不要在 util 包里依赖 Servlet、Tomcat、WebSocket 或 `AppState`。
- 如果方法只服务某个强领域语义，优先留在领域对象里；只有通用逻辑才放到 util。
- 当前 `CollectionUtil` 服务于 `HandValue` 的 `tieBreakers` 比较，也可复用于其他整数列表的逐项比较。
