# Stealth Path / 偷袭小道

## 中文

> 让单位进攻时少走弯路，也少撞进敌方火力。

Stealth Path 是一个 Mindustry 客户端路线辅助模组。它把敌方炮塔和单位形成的威胁转化为直观的路线参考，帮助你在进攻、绕行或撤退时快速判断哪条路更值得走。

它适合需要指挥单位集群、经常处理复杂敌方防线，或希望在行动前先估计风险的玩家。路线只是本地决策参考，不改变单位属性和服务器规则。

当前版本：6.0.4。该功能仍在完善，默认关闭；如果你愿意尝试实验性路线辅助，可以在设置中手动开启。

### 安装与使用

将 stealth-path.zip 放入 Mindustry 的 mods 目录并启用。进入世界后，在 设置 → 控制 中查看并绑定路线预览与自动移动相关按键；详细显示方式可在 设置 → 模组 → Stealth Path 中调整。

MindustryX 可提供额外的 OverlayUI 窗口；没有 MindustryX 时，核心路线提示仍可使用。

### 安卓

Android 端请使用 Release 中包含 classes.dex 的 stealth-path-android.jar。

### 构建（开发者）

~~~powershell
.\gradlew.bat deploy
~~~

## English

> Help units take a safer-looking route through hostile fire.

Stealth Path is a Mindustry client-side route assistant. It turns turret and unit threat fields into an understandable path reference, helping you choose a better route when attacking, retreating, or moving a group through a complicated defense line.

It is intended for players who command unit groups or want to estimate risk before committing to a move. The route is local guidance; it does not change unit stats or server rules.

Current version: 6.0.4. The feature is still being refined and is disabled by default. Enable it manually if you want to try the experimental route assistance.

### Install and use

Put stealth-path.zip in Mindustry's mods directory and enable it. Bind the route-preview and auto-move controls under Settings → Controls, then adjust the presentation under Settings → Mods → Stealth Path.

MindustryX can provide additional OverlayUI windows. The core route guidance remains available without MindustryX.

### Android

Use stealth-path-android.jar from Releases on Android; it contains classes.dex.

### Build

~~~powershell
.\gradlew.bat deploy
~~~
