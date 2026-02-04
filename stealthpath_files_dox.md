# StealthPath 文件说明（files dox）

本文件按“仓库根目录相对路径”列出 StealthPath 的主要文件/目录作用，便于维护与整合包同步（BEK-Tools 的 `tools/update_submods.py` 主要同步 `src/main/java/stealthpath` 与 `src/main/resources/bundles`）。

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
- `stealthpath_release_dox.md`：发布流程与版本号规则（含与 BEK-Tools 同步建议）

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

