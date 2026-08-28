# 开发

## 常用命令

```
./gradlew build                 # 编译 + selfTest + checkBundleSync + 打包 + 复制产物
./gradlew selfTest              # 仅 JUnit 自测
./gradlew compileJava           # 仅编译主代码
./gradlew build -Pbundled=true  # 聚合态构建
```

产物:`build/libs/betterStealthPath-dev.jar`,并自动复制到
`../构建/betterStealthPath/betterStealthPath-dev.jar`。

## 依赖接线(重要)

编译依赖来自本地 Gradle 缓存的官方 v7 巄件:

- `com.github.Anuken.Mindustry:core:v157`
- `com.github.Anuken.Arc:arc-core:4d9760e264`(v157 的 archash,见
  `Mindustry-v157/gradle.properties`)

缓存缺失时用 `-PmindustryJar=<现成 jar>` 兜底。

**陷阱 1:不要用 `../Mindustry-master` 的桌面 fat jar 编译。** 那是 v8 API:
`Input.mouseWorld(int,int)`、`Element.setVisible`、`KeyBind.isUnset()`、
`ForceBuild.radius()`、`Tile.x()` 等签名与 v7 不同,v8 编译产物在 v7 运行时会
`NoSuchMethodError`。v7 正确写法:`Core.input.mouseWorld(x,y)`、
`element.visible` 字段、`modifier.value.key != KeyCode.unset`、
`ForceBuild.realRadius()`、`tile.x` 字段。

**陷阱 2:本机(非 ASCII 用户目录)Gradle Test Executor 无法启动**
(worker classpath 乱码 → `ClassNotFoundException: GradleWorkerMain`)。
因此测试通过 `selfTest`(JavaExec + JUnitCore)运行,`test.enabled = false`。
这是 LogicSugar 同款规避;新测试类要登记进
`test/bsp/selftest/SelfTestRunner.java` 的 suites 数组。

**陷阱 3:Java 8 目标。** `options.release = 8` 下源码不能用 `var`、
instanceof 模式匹配等 10+ 语法;新增代码注意。

## i18n 流程

- 只维护 `assets/bundles/bundle.properties`(en,源头)与
  `bundle_zh_CN.properties`(zh_CN);两者 key 集合必须完全一致,
  `checkBundleSync` 在 `check` 阶段强制,不一致直接构建失败。
- 设置文案 key 规则:`setting.<settingsKey>.name/.description`;
  按键:`keybind.bsp.<name>.name`、`category.bsp.name`。

## 开发约定

- 纯逻辑进 `bsp.core.*`(禁止 import mindustry/arc),配 JUnit 测试并登记
  SelfTestRunner;游戏交互进运行时层。
- 文案只走 bundle;设置读取只走 `BspSettings`。
- MindustryX 只经 `OverlayUiBridge` 反射访问。
- 移动指令只经 `MoveDispatcher`(原生通道 + 节流)。
- 改动行为后同步 `docs/` 与根 `AGENTS.md`。
