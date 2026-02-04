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

> 这里的“接口”指模块边界/职责，不建议外部 mod 直接调用 StealthPath 内部方法（StealthPath 不是库）。整合包（BEK-Tools）是通过源码合并而非依赖调用。

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

