# StealthPath 架构说明（architecture dox）

本文件描述 StealthPath 的核心数据流与模块边界，便于后续维护（性能、联机节流、UI 托管等）。

## 1. 数据流总览

1) **输入**：玩家热键 / 自动模式状态 / 目标模式（核心/建筑/鼠标等）  
2) **威胁图**：从世界内容构建 `ThreatMap`（炮塔射程、单位威胁、护盾等）  
3) **寻路**：在 tile 网格上做代价最小路径搜索，得到 tile path  
4) **后处理**：路径压缩、端点/标签位置、集群拆分/合并  
5) **输出**：
   - 绘制：显示路径、端点、伤害数字
   - 自动移动：把 tile path 转换为 RTS 路点并下发（带节流）
   - OverlayUI：同步窗口显示的文本/按钮状态

## 2. 核心类型

见 `src/main/java/stealthpath/StealthPathPathTypes.java`：

- `ThreatMap`：地图宽高与风险数组（按 tile 索引）
- `Node`：寻路节点（实现 `Comparable` 供优先队列排序）
- `PathResult`：寻路结果（tile path、估算伤害等）
- `ControlledCluster`：单位集群（用于多集群自动模式与多路径绘制）

## 3. 寻路与代价模型（概念）

实现主要在：

- `src/main/java/stealthpath/StealthPathMod.java`（调度、不同模式选择、结果缓存）
- `src/main/java/stealthpath/StealthPathPathUtil.java`（工具函数、可通行、路径压缩/哈希等）

基本原则：

- 以 tile 为节点做图搜索
- 代价由“路径长度 + 风险积分（威胁图）”组合而成（不同模式权重可能不同）
- 单位集群的寻路起点通常为集群中心点（或选中单位的中心）

## 4. 自动模式（N/M）

- N：集群 → 鼠标
- M：集群 → 聊天坐标（解析 `(x,y)`）

自动模式的路径刷新可通过设置项控制（例如预览刷新间隔与“空闲时放慢”）。

## 5. 自动移动 / RTS 下发（联机重点）

StealthPath 会把路径转换为一系列 RTS 路点并下发。为避免服务器误判 DoS：

- `sp-rts-update-interval`：限制重复更新整条路径的最小间隔（**不减缓路径本身计算**）
- `sp-rts-command-spacing`：在每条 RTS 指令包之间插入额外延迟（降低瞬时包量）
- `sp-rts-max-waypoints`：限制单次下发的路点数量（避免过长路径带来大量包）

此外，RTS 路点生成有一个关键规则：

- **不在路径起点设置 RTS 路点**（避免单位卡住/抖动），而是从起点之后的点开始下发。

## 6. OverlayUI 与 HUD 回退

见 `stealthpath_overlayui_dox.md`。

