# Stealth Path / 偷袭小道 (Mindustry Mod)

Compute and draw safest/lowest-damage paths using enemy turret/unit ranges.

根据敌方炮塔/单位射程计算并绘制最安全/最少受伤路线。

## Other languages / 其他语言

- Español: `README_es.md`
- Français: `README_fr.md`
- Русский: `README_ru.md`
- العربية: `README_ar.md`

## Usage / 使用方法

### Hotkeys / 热键

- `X` (`sp_path_turrets`): turrets only / 仅敌方炮塔（长按实时预览）
- `Y` (`sp_path_all`): turrets + units / 敌方炮塔 + 单位（长按实时预览）
- `K` (`sp_mode_cycle`): tap to cycle display mode / 点按轮流切换显示模式
- `L` (`sp_threat_cycle`): cycle threat filter (ground/air/both) / 切换威胁计算（陆军/空军/一起）

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
