# AGENTS.md — betterStealthPath(偷袭小道+)

## 项目定位

betterStealthPath 是 Mindustry 客户端路线辅助模组(Stealth Path 概念的"更好版本"),
按工作区"洁净室再实现"流程开发:

- **唯一需求来源**:`FEATURE.md`(设计规格,黑盒行为描述)。实现工程师未见过、也不允许
  查看原版 Stealth Path 的任何代码或文档。
- **洁净室红线**:禁止读取 `../StealthPath/`、`../StealthPath-threat-model/`、`../Neon*`
  (内含原版同步副本)及 `../构建/` 下 StealthPath 相关产物;禁止反编译任何现成 jar。
- 对外承诺:本地参考 + 原生移动指令(见 README 公平性边界),`hidden: true`,
  不修改任何游戏数值,不做服务器端组件。

## 目录布局

```
FEATURE.md              唯一需求规格(不修改)
mod.json                模组描述(开发身份 name=betterStealthPath-dev, version=0.0.0, hidden, minGameVersion=154)
build.gradle            构建脚本(v157 依赖接线 / selfTest / checkBundleSync / bundled 开关 / 产物复制)
settings.gradle, gradle.properties, gradlew(.bat), gradle/wrapper/
assets/bundles/         bundle.properties + bundle_zh_CN.properties(仅两个全量 bundle)
src/bsp/                运行时源码
  BspMod.java           入口(Mod 子类;事件挂点)
  BspController.java    主控制器(按键/规划调度/路线簿记/喂给指令下发)
  BspBuildFlags.java    聚合态标记(读 /bsp/bundled.properties)
  core/                 纯逻辑核心(不 import mindustry/arc,脱离游戏可测):
    model/              GridPoint, Domain, TileEnv, ThreatShape, ThreatSource,
                        FormationProfile, RouteResult
    threat/             ThreatGrid, ThreatModel(编队感知采样公式)
    path/               CostModel, AStar, LiquidPolicy, PathPlanner, WaypointCompactor
    cluster/            ClusterSplitter
    power/              PowerClusterFinder
    geo/                GridUtils, ChatCoordinateParser
  command/MoveDispatcher  原生指挥通道下发(双节流+分批)
  input/BspKeys           KeyBind 注册与门控(修饰键/文本焦点/移动端)
  render/RouteRenderer    世界渲染(折线/端点/伤害字/热力图/目标点标记/悬停明细)
  settings/               BspSettings + TargetMode + FilterMode(AUTO 为默认档)
  ui/                     SettingsPage, HudController(HUD 回退), OverlayUiBridge(反射桥),
                          Toasts, UiState
  update/UpdateChecker    更新检查(只提示+打开浏览器/镜像,绝不自动替换重启)
  world/WorldScanner      游戏对象 → 纯核心输入翻译(炮塔/单位/护盾/地块/编队/目标)
test/bsp/               JUnit 4 测试 + selftest/SelfTestRunner(JavaExec 入口)
docs/                   六件套文档(见下)
```

## 真实构建命令

```
./gradlew build                     # 全部:编译 + selfTest(65 个 JUnit)+ checkBundleSync + 打包 + 复制
./gradlew selfTest                  # 仅跑测试
./gradlew build -Pbundled=true      # 聚合态构建(注入 bundled=true 标记)
```

- 依赖:本地 Gradle 缓存的官方 `com.github.Anuken.Mindustry:core:v157` +
  `com.github.Anuken.Arc:arc-core:4d9760e264`(v157 的 archash),离线;
  或 `-PmindustryJar=<路径>` 显式指定。**不要**改回 `../Mindustry-master` 的
  桌面 fat jar 编译 — 那是 v8 API(`mouseWorld(int,int)`、`Tile.x()`、
  `KeyBind.isUnset()` 等签名不同),编出的 class 在 v7 会崩。
- 字节码目标 Java 8(`options.release = 8`),源码必须是 Java 8 语法
  (无 `var`、无 instanceof 模式匹配)。
- 本机限制:Gradle Test Executor 无法在含非 ASCII 用户目录的主机上启动
  (worker classpath 乱码),因此测试经 `selfTest`(JavaExec + JUnitCore)
  运行,`test` task 已禁用。改动测试后必须跑 `./gradlew selfTest`。

## 代码约束

- `bsp.core.*` 绝不 import `mindustry.*` / `arc.*`;游戏交互只发生在
  WorldScanner / 渲染 / 指令 / UI 层。新增威胁类型或目标模式时先扩展
  纯核心模型 + 测试,再接运行时。
- 对 MindustryX 的一切访问走 `bsp.ui.OverlayUiBridge` 反射,缺失时静默
  回退 HUD,不得在别处 import `mindustryX.*`。
- 用户可见文案一律走 bundle(只维护 en + zh_CN 两个全量文件,二者 key
  集合必须一致,`checkBundleSync` 构建期强制)。禁止硬编码 UI 文案。
- 移动指令只允许 `Call.commandUnits(..., move 语义)`(posTarget,无攻击目标);
  保持重发/包间隔/分批节流,不得绕过。
- 设置项读取集中在 `BspSettings`,key 前缀 `bsp.`,即时生效(不得引入需重启的设置)。
- 注释克制:只在表达代码无法自明的约束时写(如上文的 v7/v8 API 陷阱)。

## 验证清单(交付/回归前)

1. `./gradlew build` 全绿(selfTest 0 failures、checkBundleSync OK)。
2. jar 内容检查:`mod.json`、根 `bundles/bundle.properties`、
   `bundles/bundle_zh_CN.properties`、`bsp/bundled.properties`(false);
   字节码 major version 52。
3. `./gradlew build -Pbundled=true` 产出 marker 为 true(契约可用)。
4. 产物已复制到 `../构建/betterStealthPath/betterStealthPath-dev.jar`。
5. 手测(结构化清单见 `docs/testing.md`):深水图陆军不进危险深水、
   跨矿渣+深水仍能出路线、无 X 环境不崩、HUD 回退可用。

## 提交规范

- 本仓库无 Git 时直接携带文件;有 Git 时按 `<ModName>: ...` 风格,如
  `betterStealthPath: formation-aware threat sampling`。
- 行为/构建变更须同步 `docs/` 与本文件。

## 文档

导航见 `docs/README.md`;架构分层、开发流程、发布(含聚合态契约)、测试与术语
分别对应 `docs/architecture.md`、`docs/development.md`、`docs/release.md`、
`docs/testing.md`、`docs/glossary.md`。
