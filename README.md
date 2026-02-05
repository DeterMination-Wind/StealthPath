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
