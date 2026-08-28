# 发布

## 版本方案

语义化 `大功能.小功能.修复`。当前仓库为开发身份(mod.json:
`name=betterStealthPath-dev`、`version=0.0.0`、`hidden=true`),发布时改回
正式 `name=betterStealthPath` 与正式版本号。本地开发产物固定为
`构建/betterStealthPath/betterStealthPath-dev.jar`。

## 产物形态与取舍

- **桌面专用**:jar 只含 Java 8 字节码 + mod.json + bundles,**不含**
  Android `classes.dex`。理由:全部交互依赖键盘/鼠标;产出无法测试的
  "能加载但无功能"的安卓包是死重(该品类的经典批评)。README 已明示。
- **设置面板去实现化**(规格 9.4 授权的取舍):原高级区中纯实现细节项
  ——寻路算法选择、安全走廊居中偏好、计算安全距离图开关、可通行性缓存
  条目数、发电集群最小绘制长度/回退格数——不再暴露给玩家。"距离 vs 风险"
  的权衡升级为"谨慎←→激进"滑杆承担;缓存/性能类旋钮(通行性缓存、安全
  距离图)改用固定策略(建筑增减自动失效重建、威胁图按世界尺寸复用)。
  保留的每个设置都接真实行为,不存在只改文案不接行为的空设置。
- 兼容目标:Mindustry v7,`minGameVersion: 154`(编译基线 v157 官方 API,
  未使用 158+ 才有的接口)。
- Release 纪律:一个 Release **有且只有一个** `.jar` 资产(最终安装包),
  不上传任何中间产物;发布后复查资产列表。

## 独立 / 聚合双形态契约(第一天实现)

- 独立态(默认):自有设置分类(`设置 → 模组 → 偷袭小道+` 文案由本仓库
  bundle 提供)、自有按键、自有更新检查(仓库地址可配,默认空=禁用)。
- 聚合态:`./gradlew build -Pbundled=true` 向 jar 注入
  `bsp/bundled.properties(bundled=true)`;`BspBuildFlags.bundled()` 读到
  true 后:
  1. `SettingsPage.register()` 直接返回(不注册独立设置分类);
  2. `UpdateChecker.onMainMenu()` 直接返回(禁用独立更新检查)。
  聚合方负责:设置页吸收(文案 key 约定即本仓库 bundle)、发版流程。
  聚合态不改变行为逻辑,只让出"门面"。

## 更新检查口径(简化安全版)

只做:GitHub Release API 轮询(主菜单、6 小时节流、可在设置关闭)→
弹窗列资产/版本 → "打开 GitHub / 经镜像打开 / 忽略该版本 / 稍后";
镜像前缀在弹窗内配置。**不做**下载替换自身文件与自动重启;dev 身份
(0.0.0)自动跳过检查。

## 发布步骤(桌面正式版)

1. `mod.json` 换正式身份(name/version),`./gradlew build` 全绿;
2. 确认 jar:根 `bundles/` 两份全量 bundle、`bsp/bundled.properties=false`、
   字节码 major 52;
3. 上传唯一 `.jar` 资产,复查资产列表;
4. 版本号与变更说明进 tag/Release notes;`docs/` 与 AGENTS.md 同步。
