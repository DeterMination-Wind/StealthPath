# StealthPath - Documentation (Merged)

This file consolidates the repository's main docs into a single entrypoint.

Sources (original files are kept):
- `README.md`
- `RELEASE_NOTES.md`
- `stealthpath_overview_dox.md`
- `stealthpath_arch_dox.md`
- `stealthpath_api_dox.md`
- `stealthpath_overlayui_dox.md`
- `stealthpath_files_dox.md`
- `stealthpath_release_dox.md`

Note: language variants (`README_*.md`) and auto-generated `dox__*.md` files are intentionally not inlined here to keep this document readable.

---

## README.md

# Stealth Path / 偷袭小道 (Mindustry Mod)

- [中文](#中文)
- [English](#english)

## 中文

### 简介

偷袭小道是一个纯客户端叠加层模组：根据敌方炮塔/单位的威胁范围，在地图上绘制“更安全 / 受伤更少”的路线预览；并提供自动模式，辅助选中单位集群进行移动与绕行。

当前版本：`4.2.0`

### 功能一览

- 路线叠加显示：一键计算并绘制路径；线宽/透明度/显示时长可调，可选显示起终点与预计受伤文字。
- 威胁过滤：可在“陆军 / 空军 / 全部”之间切换，更贴合当前单位类型。
- 多种目标/显示模式：支持常规目标模式、敌方发电集群模式、玩家→鼠标位置模式等。
- 手动实时预览：按住热键即可实时刷新路径（适合边移动边观察）。
- 自动模式（单位集群）：
  - `N`：单位集群 → 鼠标位置
  - `M`：单位集群 → 聊天坐标（`<Attack>(x,y)`）
- 自动移动：在 M/N 模式下，可用“自动移动”热键让选中单位沿最低受伤路线前进（可在设置中开关）。
- OverlayUI/HUD 信息窗：安装 MindustryX 时可显示模式/伤害/控制等窗口；未安装时会回退为普通 HUD 显示。
- 设置菜单：提供常规设置与 Pro Mode 高级设置，便于按个人习惯微调显示与自动模式行为。
- 多人兼容：客户端侧显示与操作辅助，不需要服务器安装。

### 使用方法

#### 热键（可在 `设置 → 控制` 中改键）

- `X`：仅敌方炮塔（按住=实时预览）
- `Y`：敌方炮塔 + 单位（按住=实时预览）
- `N`：自动模式（单位集群 → 鼠标）
- `M`：自动模式（单位集群 → `<Attack>(x,y)`）
- `K`：切换显示模式
- `L`：切换威胁过滤（陆军/空军/全部）
- `自动移动`：在 M/N 模式下下发沿路线前进的移动指令

#### 自动模式要点

- 起点默认使用“单位集群中心”：优先使用你框选的单位，否则使用你当前控制的单位。
- `M` 模式目标：在聊天发送 `"<Attack>(x,y)"`（x,y 为格子坐标）来设置目标点。
- 自动模式刷新频率与显示样式可在设置中调节（例如“预览刷新间隔”“自动模式颜色/阈值”等）。
- 选中单位较多或较分散时，可能会按多个集群分别绘制路径与下发移动，以尽量保持队形。

### 设置

设置入口：`设置 → 模组 → 偷袭小道 (Stealth Path)`

常用设置包括：路径显示秒数、线条粗细、透明度、预计受伤文字、实时预览刷新间隔、自动模式颜色与安全阈值、自动移动开关等。

### 其他语言

- Español: `README_es.md`
- Français: `README_fr.md`
- Русский: `README_ru.md`
- العربية: `README_ar.md`

### 安装

将 `stealth-path.zip` 放入 Mindustry 的 `mods` 目录并在游戏内启用。

### 安卓

安卓端需要包含 `classes.dex` 的 mod 包。请下载 Release 中的 `stealth-path-android.jar` 并放入 Mindustry 的 `mods` 目录。

### 反馈

【BEK辅助mod反馈群】：https://qm.qq.com/q/cZWzPa4cTu

![BEK辅助mod反馈群二维码](docs/bek-feedback-group.png)

### 构建（可选，开发者）

在 `Mindustry-master` 根目录运行：

```powershell
./gradlew.bat stealth-path:jar
```

输出：`mods/stealth-path/build/libs/stealth-path.zip`

本仓库本地构建（Android）：

```powershell
./gradlew.bat jarAndroid
```

输出：`dist/stealth-path-android.jar`

---

## English

### Overview

Stealth Path is a client-side overlay mod that draws “safer / lower-damage” route previews based on enemy turret and unit threat ranges. It also includes auto modes to help selected unit groups move and avoid danger.

Current version: `4.2.0`

### Features

- Path overlay: draw a route preview with configurable duration/width/opacity, optional endpoints, and optional estimated-damage labels.
- Threat filter: switch between ground / air / both to better match your current unit type.
- Multiple target/display modes: includes the normal target mode, enemy generator-cluster mode, and player-to-mouse mode.
- Manual live preview: hold a hotkey to continuously refresh the preview while you aim.
- Auto modes (unit cluster):
  - `N`: cluster → mouse position
  - `M`: cluster → chat coordinates (`<Attack>(x,y)`)
- Auto move: in M/N mode, press the auto-move keybind to command selected units along the lowest-damage path (can be enabled/disabled in settings).
- OverlayUI/HUD windows: when MindustryX is installed, mode/damage/controls windows can be shown via OverlayUI; otherwise the mod falls back to regular HUD.
- Settings menu: regular settings plus Pro Mode advanced options for finer tuning.
- Multiplayer-friendly: client-side overlay and assistance; no server install required.

### Usage

#### Hotkeys (rebind in `Settings → Controls`)

- `X`: turrets only (hold = live preview)
- `Y`: turrets + units (hold = live preview)
- `N`: auto mode (unit cluster → mouse)
- `M`: auto mode (unit cluster → `<Attack>(x,y)`)
- `K`: cycle display mode
- `L`: cycle threat filter (ground/air/both)
- `Auto move`: issue movement commands along the preview path in M/N mode

#### Auto Mode Notes

- The start point is the unit-cluster center: selected units if any, otherwise your current controlled unit.
- For `M` mode, send `"<Attack>(x,y)"` in chat (x,y are tile coordinates) to set the target.
- Auto refresh rate and visuals are configurable in settings (e.g. preview refresh interval, auto-mode colors/thresholds).
- Large or spread-out selections may be handled as multiple clusters to keep formation tighter.

### Settings

Open: `Settings → Mods → Stealth Path`

Common options include path duration, line width, opacity, estimated-damage labels, live preview refresh interval, auto-mode colors/safe thresholds, and auto-move enable.

### Other Languages

- Español: `README_es.md`
- Français: `README_fr.md`
- Русский: `README_ru.md`
- العربية: `README_ar.md`

### Install

Put `stealth-path.zip` into Mindustry's `mods` folder and enable it in-game.

### Android

Android requires a mod package that contains `classes.dex`. Download `stealth-path-android.jar` from Releases and put it into Mindustry's `mods` folder.

### Feedback

Discord: https://discord.com/channels/391020510269669376/1467903894716940522

### Build (Optional)

Run from the `Mindustry-master` repo root:

```powershell
./gradlew.bat stealth-path:jar
```

Output: `mods/stealth-path/build/libs/stealth-path.zip`

Local Android build (from this repo root):

```powershell
./gradlew.bat jarAndroid
```

Output: `dist/stealth-path-android.jar`

---

## RELEASE_NOTES.md

# 🥷 StealthPath 更新日志 / Release Notes

## 🆕 本次版本（持续维护）
- ✅ 这个文件会作为每次 GitHub Release 的说明文本。
- 📝 需要写“本次新增/修复”时，直接在这里追加即可（保留下面的“功能总览”）。
- 🔄 更新弹窗增强：支持展示可下载文件（assets）、镜像下载开关、下载并重启（桌面端）。

## ✨ 功能总览（历史累计）
- 🗺️ **路径叠加显示**：按键快速计算并在地图上绘制路线（可调线宽/透明度/显示时长）。
- ⚔️ **威胁计算**：根据敌方炮塔与单位射程/估算 DPS 生成威胁地图，并按移动速度换算预计受伤。
- 🧠 **寻路策略**：
  - 🟢 优先 0 受伤路径（Safe Only），找不到再退化为最小受伤路径（Min Damage）。
  - 📏 可选“始终规划最近路径”（Nearest）：忽略伤害，直接按最短路规划。
  - 🔁 可选寻路算法：A* / DFS（都按当前模式代价选择最优）。
- 🎯 **多目标核心预览**：核心模式可同时规划到最近的 K 个敌方核心（K 可在设置中调整）。
- 🤖 **自动模式（单位集群）**：
  - 🧩 选中单位会按距离自动分割为多个集群规划。
  - ⏱️ 自动模式寻路支持“分拆计算到 Tick”，减少卡顿。
  - 🛰️ RTS 自动移动：支持路点上限、更新间隔、指令间隔（减轻服务器 DoS 误判）。
- 🪟 **OverlayUI 窗口**：与 MindustryX OverlayUI 兼容（或回退 HUD），可显示模式/威胁与预计受伤，并提供快捷按钮。
- 🛡️ **绕开无敌盾范围**：规划时会将 `shield-projector` / `large-shield-projector` 盾域视作不可通行区域（防止卡住/穿不过）。
- ⚙️ **设置菜单**：MindustryX 风格的单页设置 + Pro Mode 高级选项（可展开/折叠）。
- 🌐 **多人兼容**：`hidden: true` 纯客户端显示/辅助逻辑，避免多人服 mod 校验冲突。
- 🔔 **启动检测更新**：启动后检查 GitHub Releases 是否有新版本，有则弹窗提示并支持跳转/忽略。

## 🔗 链接
- GitHub Releases：https://github.com/DeterMination-Wind/StealthPath/releases

---

## stealthpath_overview_dox.md

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
- （以及仓库上一级目录，用于快速安装）

---

## stealthpath_arch_dox.md

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

---

## stealthpath_api_dox.md

# StealthPath 接口说明（API dox）

本文件描述“对外可用/可配置/可依赖”的接口：热键、设置项（`Core.settings` keys）、OverlayUI 窗口、聊天格式，以及各源码文件中**主要可复用方法/模块职责**。

## 1. 玩家接口（Player-facing）

### 1.1 热键（KeyBind）

注册位置：`StealthPathMod.registerKeybinds()`

| KeyBind 名称 | 默认键位 | 用途 |
|---|---:|---|
| `sp_path_turrets` | `X` | 仅炮塔威胁路径预览（按住预览） |
| `sp_path_all` | `Y` | 炮塔+单位威胁路径预览（按住预览） |
| `sp_modifier` | unset | 修饰键（用于组合触发/减少误触） |
| `sp_mode_cycle` | `K` | 切换显示模式 |
| `sp_threat_cycle` | `L` | 切换威胁过滤：陆 / 空 / 混合 |
| `sp_auto_mouse` | `N` | 自动模式：集群→鼠标 |
| `sp_auto_attack` | `M` | 自动模式：集群→聊天坐标 |
| `sp_auto_move` | `鼠标右键` | 执行自动移动/下发 RTS |

### 1.2 聊天坐标（Attack target）

解析规则：仅解析你自己发送的聊天内容中出现的 `(x,y)`（Tile 坐标）：

- 正则：`\\((-?\\d+)\\s*,\\s*(-?\\d+)\\)`
- 超界坐标会被 clamp 到地图边界
- 主要用途：M 模式下设置目标坐标缓存

### 1.3 OverlayUI（MindustryX 可选）

窗口 ID：

- `stealthpath-mode`
- `stealthpath-damage`
- `stealthpath-controls`

窗口开关（设置项，见 2.2）会影响：

- MindustryX 安装时：窗口 `availability`（是否可见）
- 未安装时：HUD 回退元素是否挂载到 `ui.hudGroup`

详细机制见 `stealthpath_overlayui_dox.md`。

## 2. 设置项接口（Settings keys）

设置项由 `Core.settings` 管理，默认值在 `StealthPathMod.registerDefaults()`（或等价位置）写入。

### 2.1 总开关/模式

- `sp-enabled`：启用/禁用（总开关）
- `sp-pro-mode`：专业模式（展开/显示高级设置折叠区）

### 2.2 OverlayUI 窗口显示

- `sp-ov-window-mode`：显示“模式/威胁”窗口
- `sp-ov-window-damage`：显示“预估伤害”窗口
- `sp-ov-window-controls`：显示“快捷控制”窗口

### 2.3 路径绘制/显示

- `sp-path-duration`
- `sp-path-width`
- `sp-path-alpha`
- `sp-show-endpoints`
- `sp-start-dot-scale`
- `sp-end-dot-scale`
- `sp-show-damage-text`
- `sp-damage-text-scale`
- `sp-damage-label-at-end`
- `sp-damage-offset-scale`
- `sp-preview-refresh`

### 2.4 威胁/目标

- `sp-threat-mode`
- `sp-target-mode`
- `sp-target-block`

### 2.5 颜色

- `sp-arrow-color`
- `sp-mouse-path-color`
- `sp-auto-color-safe`
- `sp-auto-color-warn`
- `sp-auto-color-dead`

### 2.6 自动模式/自动移动（RTS）

（用于解决多人服 DoS 误判等问题的节流也在这里）

- `sp-auto-move-enabled`
- `sp-rts-max-waypoints`
- `sp-rts-update-interval`：RTS 更新最小间隔（不影响路径计算刷新本身）
- `sp-rts-command-spacing`：RTS 指令包之间额外延迟（降低服务器误判 DoS 风险）
- `sp-auto-batch-enabled` / `sp-auto-batch-size-pct` / `sp-auto-batch-delay-pct`
- `sp-auto-slow-multiplier`

### 2.7 高级：寻路/缓存/发电集群

- `sp-auto-cluster-split-tiles`
- `sp-formation-inflate-pct`
- `sp-safe-corridor-bias-pct`
- `sp-compute-safe-distance`
- `sp-passable-cache-entries`
- `sp-gencluster-*`（linkdist / near-turret / min-draw / fallback-backtrack 等）

## 3. 代码接口（Developer-facing）

> 这里的“接口”指模块边界/职责，不建议外部 mod 直接调用 StealthPath 内部方法（StealthPath 不是库）。整合包（Neon）是通过源码合并而非依赖调用。

### 3.1 `StealthPathMod`

主要职责模块（可按函数名在文件内检索）：

- 输入与模式：热键处理、模式切换、自动模式状态机
- ThreatMap 生成：根据炮塔/单位/护盾等生成风险图
- 寻路：根据 ThreatMap 计算 tile path，并生成 render path
- 绘制：在 `Trigger.draw` 中绘制路径/端点/伤害标签
- 自动移动：根据路径生成 RTS 路点并下发
- OverlayUI：注册 3 个窗口，并同步可见性与回退 HUD

### 3.2 `StealthPathPathUtil` / `StealthPathPathTypes`

- `ThreatMap`：地图宽高、风险数组等
- `Node`：A* / Dijkstra 类寻路节点（含 `compareTo`）
- `PathResult`：路径与估算信息
- `ControlledCluster`：单位集群（用于多集群分路径/自动模式）

### 3.3 `StealthPathSettingsWidgets`

SettingsMenuDialog 的自定义 Setting 组件：

- `HeaderSetting`：分组标题行（目前不带 icon）
- `IconCheckSetting` / `IconSliderSetting` / `IconTextSetting`：带描述与统一样式的设置项（icon 参数可为 `null`，即不显示 icon）

---

## stealthpath_overlayui_dox.md

# StealthPath × MindustryX OverlayUI 说明（OverlayUI dox）

StealthPath 支持在安装 MindustryX 的情况下，把 UI 内容托管到 `OverlayUI`（可拖拽、可缩放、可拉伸、可在 OverlayUI 面板管理）；未安装 MindustryX 时回退到 HUD 左上角的普通 `Table`。

## 1. 窗口列表

| 窗口 ID | 内容 | 说明 |
|---|---|---|
| `stealthpath-mode` | 当前路径模式 + 威胁模式 | 文字信息展示 |
| `stealthpath-damage` | 当前路径预估伤害 | 文字信息展示 |
| `stealthpath-controls` | X/Y/N/M/威胁切换按钮 | 可交互；支持随窗口尺寸自适应排布 |

## 2. 可见性/开关策略（修复“无法关闭单个窗口”的 bug）

实现位置：`StealthPathMod.ensureOverlayWindowsAttached()`

### 2.1 不再强制 pinned/enabled

曾经的问题：每帧/每次检查都把三个窗口强制 `enabled=true` 且 `pinned=true`，导致玩家即使在 OverlayUI 面板里关闭某个窗口，也会被立刻重新打开。

现在的策略：

- 仅在“首次注册窗口对象”时，根据当前 Mod 总开关与窗口开关，默认把该窗口设为 `enabled=true` 且 `pinned=false`（不再强制钉住）。
- 后续不再覆盖用户在 OverlayUI 面板中的 `enabled`/`pinned` 选择。

### 2.2 设置项控制（额外提供一层开关）

新增 3 个设置项（同样影响 HUD 回退模式）：

- `sp-ov-window-mode`
- `sp-ov-window-damage`
- `sp-ov-window-controls`

当 MindustryX 存在时，这些开关进入窗口的 `availability`（即窗口是否可见/是否参与 OverlayUI 渲染）。

## 3. 回退 HUD（无 MindustryX）

当无法使用 OverlayUI 时：

- 会把三个 `Table` 挂到 `ui.hudGroup`，并固定在左上角附近
- 当对应窗口开关关闭时，会从 `hudGroup` 移除该元素

## 4. “回弹”与任意尺寸拉伸（resize snap-back）

OverlayUI 的 `Window.endResize()` 会 `pack()` 并将 `data.size` 写回为 `table.width/height`；若内容 `Table` 的 `prefWidth/prefHeight` 被内容约束为固定值，玩家松手后可能看起来“回弹到默认大小”。

StealthPath 的处理：

- 内容表尾部添加 `PreferAnySize` 占位元素（`min=0`，`pref=当前宽高`），允许窗口保持任意尺寸。
- 同时 `stealthpath-controls` 内按钮区域会根据宽度阈值动态重排：窄则竖排，宽则多列。

---

## stealthpath_files_dox.md

# StealthPath 文件说明（files dox）

本文件按“仓库根目录相对路径”列出 StealthPath 的主要文件/目录作用，便于维护与整合包同步（Neon 的 `tools/update_submods.py` 主要同步 `src/main/java/stealthpath` 与 `src/main/resources/bundles`）。

> 说明范围：以 mod 的可维护代码/资源为主；Gradle wrapper、IDE 配置、构建缓存等不在此详述。

## 根目录

- `README.md`：玩家向说明（功能、热键、设置入口、构建方式等）
- `build.gradle`：Gradle 构建脚本（Mindustry core 依赖、zip 输出、复制到 `dist/`）
- `src/main/resources/mod.json`：Mindustry Mod 元数据（`name`/`version`/`minGameVersion`/`main` 等）
- `dist/stealth-path.zip`：构建产物（发布用；由 Gradle 生成）
- `stealthpath_overview_dox.md`：仓库级概览文档（维护者/开发者）
- `stealthpath_api_dox.md`：接口说明（热键、设置 key、OverlayUI 窗口、主要方法职责）
- `stealthpath_overlayui_dox.md`：OverlayUI/回退 HUD 机制与可见性控制
- `stealthpath_arch_dox.md`：核心算法/架构说明（ThreatMap、寻路、自动模式、RTS 下发）
- `stealthpath_release_dox.md`：发布流程与版本号规则（含与 Neon 同步建议）

## Java 源码（`src/main/java/stealthpath/`）

- `StealthPathMod.java`
  - Mod 主入口：事件注册、输入/热键、设置菜单、绘制、路径计算、自动模式、RTS 路点下发
  - 可选集成：MindustryX `OverlayUI`（反射注册窗口），缺失时回退 HUD
- `StealthPathPathUtil.java`
  - 寻路工具：可通行判断、路径压缩/哈希、路径后处理等
- `StealthPathPathTypes.java`
  - 数据结构：`ThreatMap`、`PathResult`、`Node`、`ControlledCluster` 等
- `StealthPathMathUtil.java`
  - 数学/几何工具：距离、角度、格子/世界坐标换算等
- `StealthPathSettingsWidgets.java`
  - 设置 UI 小组件：`HeaderSetting`、`IconCheckSetting`、`IconSliderSetting`、`IconTextSetting`
  - 注意：设置页内 icon 已被上层传入 `null`（仅保留一级分类 icon）
- `StealthPathUiUtil.java`
  - UI 布局通用参数（例如设置项推荐宽度 `prefWidth()`）
- `GithubUpdateCheck.java`
  - GitHub 更新检查/提示（若存在）

## 资源（`src/main/resources/`）

- `mod.json`：Mod 元信息
- `bundles/`：多语言文本
  - `bundle.properties`：默认语言（通常英文）
  - `bundle_zh_CN.properties`：简体中文
  - 其它 `bundle_*.properties`：其它语言

---

## stealthpath_release_dox.md

# StealthPath 发布/维护说明（release dox）

## 1. 版本号规则

采用 `Major.Minor.Patch`（大功能-小功能-bug 修复）：

- `Major`：大功能/大改动（可能影响配置/交互/兼容）
- `Minor`：新增小功能（尽量向后兼容）
- `Patch`：bug 修复/小调整

版本号需要同时更新：

- `build.gradle` 的 `version`
- `src/main/resources/mod.json` 的 `"version"`
- `README.md` 的 “Current version”

## 2. 构建

在 StealthPath 仓库根目录运行：

```powershell
./gradlew.bat --no-daemon clean jar
```

产物：

- `dist/stealth-path.zip`

## 3. 与 Neon 同步（维护者）

Neon 使用 `tools/update_submods.py` 读取本地 git checkout 的 HEAD 并合并源码：

```powershell
cd ..\\Neon
python tools/update_submods.py
```

同步后建议：

1) 更新 Neon 版本号（`build.gradle`、`src/main/resources/mod.json`，以及 README 的 StealthPath 当前版本行）
2) 构建验证：`./gradlew.bat --no-daemon clean jar jarAndroid`
3) 提交、推送、打 tag（`vX.Y.Z`）触发 GitHub Actions Release
