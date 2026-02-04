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

