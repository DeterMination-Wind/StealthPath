# StealthPath 文档（维护者/开发者）

本目录下的 `*_dox.md` 为 StealthPath 仓库级说明文档，统一放在仓库根目录，便于维护与在整合包（Neon）中同步时查阅。

## 1. 这个 Mod 做什么

StealthPath 是一个 **纯客户端叠加层** 模组，用于基于敌方炮塔/单位威胁范围计算路径，并绘制“最安全/最少受伤”的路径预览；同时提供自动模式（单位集群跟随鼠标/聊天坐标）与 RTS 路点下发（自动移动）。

## 2. 关键入口

- 主类：`src/main/java/stealthpath/StealthPathMod.java`
- 关键算法/数据结构：
  - `src/main/java/stealthpath/StealthPathPathUtil.java`（寻路/可通行/路径压缩等工具）
  - `src/main/java/stealthpath/StealthPathPathTypes.java`（ThreatMap/Node/PathResult 等类型）
  - `src/main/java/stealthpath/StealthPathMathUtil.java`（几何/网格相关计算）
- UI/设置：
  - `src/main/java/stealthpath/StealthPathSettingsWidgets.java`（Settings UI 小组件）
  - `src/main/java/stealthpath/StealthPathUiUtil.java`（UI 尺寸/通用样式辅助）
- 可选集成：
  - MindustryX `OverlayUI`：在 `StealthPathMod` 内通过 **反射** 注册窗口；缺失 MindustryX 时回退到 HUD。

## 3. “对外接口”一览（给玩家/整合包）

### 3.1 热键（KeyBind）

在 `StealthPathMod.registerKeybinds()` 注册（可在 游戏设置 → Controls 中改键）：

- `sp_path_turrets`（默认 `X`）：仅炮塔威胁路径
- `sp_path_all`（默认 `Y`）：炮塔 + 单位威胁路径
- `sp_modifier`（默认 unset）：修饰键（用于组合操作）
- `sp_mode_cycle`（默认 `K`）：切换显示模式
- `sp_threat_cycle`（默认 `L`）：切换威胁过滤（陆/空/混合）
- `sp_auto_mouse`（默认 `N`）：自动（集群→鼠标）
- `sp_auto_attack`（默认 `M`）：自动（集群→聊天坐标）
- `sp_auto_move`（默认 `鼠标右键`）：执行自动移动/下发 RTS

### 3.2 OverlayUI 窗口（MindustryX 可选）

窗口 ID（注册名）：

- `stealthpath-mode`
- `stealthpath-damage`
- `stealthpath-controls`

说明与可见性/缩放/可拖拽等行为，见 `stealthpath_overlayui_dox.md`。

### 3.3 聊天坐标格式

`StealthPathMod` 解析你自己发送的消息中形如 `(x,y)` 的坐标（Tile 坐标），用于 M 模式目标缓存。

## 4. 版本号策略

从 `v3.0.1` 开始采用 `Major.Minor.Patch`：

- `Major`：大功能/大改动（可能影响玩法/交互/配置习惯）
- `Minor`：小功能/新增能力（向后兼容）
- `Patch`：bug 修复/小改动

## 5. 构建与产物

仓库内 `build.gradle` 会将 jar/zip 输出并复制到：

- `dist/stealth-path.zip`
-（以及仓库上一级目录，用于快速安装）

