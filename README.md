# Stealth Path / 偷袭小道 (Mindustry Mod)

Compute and draw safest/lowest-damage paths using enemy turret/unit ranges.

根据敌方炮塔/单位射程计算并绘制最安全/最少受伤路线。

Current version / 当前版本: `2.0.7`

## Other languages / 其他语言

- Español: `README_es.md`
- Français: `README_fr.md`
- Русский: `README_ru.md`
- العربية: `README_ar.md`

## Usage / 使用方法

### Hotkeys / 热键

- `X` (`sp_path_turrets`): turrets only / 仅敌方炮塔（长按实时预览）
- `Y` (`sp_path_all`): turrets + units / 敌方炮塔 + 单位（长按实时预览）
- `N` (`sp_auto_mouse`): auto mode (unit cluster → mouse) / 自动模式（单位群→鼠标）
- `M` (`sp_auto_attack`): auto mode (unit cluster → <Attack>(x,y)) / 自动模式（单位群→<Attack>(x,y)）
- `K` (`sp_mode_cycle`): tap to cycle display mode / 点按轮流切换显示模式
- `L` (`sp_threat_cycle`): cycle threat filter (ground/air/both) / 切换威胁计算（陆军/空军/一起）

### Auto modes / 自动模式

- Auto mode uses the **unit cluster center** as start (selected units if any, otherwise your current unit). It auto-detects air/ground/mixed and computes a Y-mode (turrets + units) lowest-damage path.  
  自动模式以**单位群中心**为起点（若有框选单位则用框选，否则用玩家当前单位），自动识别空军/陆军/混合，并按 Y 模式（炮塔+单位威胁）计算最少受伤路径。
- Hold `X/Y` is still the manual live preview; auto mode refresh rate is controlled by **Settings → Stealth Path → Preview refresh interval**.  
  长按 `X/Y` 仍是手动实时预览；自动模式刷新频率由 **设置 → 偷袭小道 → 预览刷新间隔** 控制。
- Attack chat target format: send `"<Attack>(x,y)"` in chat (x,y are **tile coordinates**). Only your own sent message is parsed client-side.  
  攻击聊天目标格式：在聊天发送 `"<Attack>(x,y)"`（x,y 为**格子坐标**）；仅客户端解析你自己发出的消息。
- Large selections may be moved in batches when you trigger auto-move to keep the formation tighter.  
  选中单位过多时，触发自动移动会自动分批下达指令，以尽量保持队形更紧凑。
- If the selected units are split into multiple groups with gaps > 5 tiles, auto mode will compute and draw a path for each group separately.  
  若选中单位分成多个相距超过 5 格的集群，自动模式会分别计算并同时绘制每个集群的路径。
- While following an auto-move command, the mod will update RTS waypoints when the best path changes (e.g. a turret starts firing).  
  在自动移动执行过程中，若最优路径发生变化（例如炮塔突然启用），mod 会自动更新 RTS 路点让单位改走新路线。

### Modes / 模式

Tap `K` to cycle these 3 display modes / 点按 `K` 轮流切换 3 种显示模式：

1) Normal target mode / 常规目标模式（设置里选择）
2) Enemy generator clusters / 敌方发电群（可同时绘制多条）
3) Player -> mouse position / 玩家->鼠标位置

## Settings / 设置

Open: `Settings → Stealth Path` / `设置 → 偷袭小道`

- Path display seconds / 路径显示秒数
- Line width / 线条粗细
- Live preview refresh interval / 预览刷新间隔（长按 X/Y 时）
- Pro Mode / 专业模式：解锁“高级设置”二级菜单
- Advanced settings / 高级设置（仅专业模式）：寻路/自动移动/缓存/发电集群等参数调节
- Generator mode:
  - Path color / 路线颜色
  - Draw count / 同时绘制数量
  - Min cluster size / 发电群最小数量
  - Start from player / 从玩家开始绘制（否则从靠近敌方炮塔的位置开始）
- Player→mouse mode:
  - Path color / 路线颜色（默认紫色）

## Multiplayer / 多人游戏

This is a client-side overlay mod. `mod.json` uses `"hidden": true` to avoid server/client mod mismatch checks.

## Build / 构建

Run from the `Mindustry-master` repo root:

```powershell
./gradlew.bat stealth-path:jar
```

Output zip: `mods/stealth-path/build/libs/stealth-path.zip`
