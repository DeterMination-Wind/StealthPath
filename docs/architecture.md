# 架构

## 分层原则

```
+--------------------------------------------------------------+
| 运行时层 (src/bsp, import mindustry/arc)                      |
|   BspMod -> BspController -> {render, command, ui, update}     |
|   WorldScanner: 游戏对象 -> 纯核心输入(唯一的翻译点)           |
+------------------------------+-------------------------------+
                               | 纯数据(ThreatSource/TileEnv/...)
+------------------------------v-------------------------------+
| 纯逻辑核心 (src/bsp/core, 不 import mindustry/arc)            |
|   threat(威胁场) path(规划) cluster power geo               |
+--------------------------------------------------------------+
```

- `bsp.core.*` 可以在任何 JVM 上运行并被 JUnit 断言(见 testing.md)。
  威胁数值口径、路径代价、液体策略的全部"决策公式"都住在核心层。
- 运行时层只做三件事:把游戏状态翻译成核心输入、把核心产出翻译成
  绘制/指令/UI。新增威胁类型 = WorldScanner 造出新的 ThreatSource +
  核心采样公式;新增目标模式 = TargetMode 枚举 + goalsFor 分支。

## 威胁模型(对外契约:每格一个威胁值,全编队 HP/s)

推导链(`ThreatModel`):

1. **有效 DPS**:`baseDps + statusDps*0.5`,减去护甲减免
   `avgArmor * shotsPerSecond`,下限 25%(`armorPierce` 无视)。
2. **可交战数量 engaged**:由弹种决定
   - DIRECT:`min(targetable, max(1, simultaneousTargets))`
   - SPLASH:溅射覆盖率 `splashRadius / formationRadius` 决定一次波及多少人
   - LINE:`max(simultaneousTargets, pierce)`
   - CONTINUOUS:束类同时锁定数
   `targetable` 由炮塔的 targetsAir/targetsGround 与编队地/空构成决定 ——
   纯对地炮对空军编队威胁为 0。
3. **空间衰减**:命中概率随距离二次衰减(DIRECT 40%、SPLASH 20%、
   线/束 10%),`minRange` 内圈盲区为 0。
4. 格值 = 有效DPS × engaged × 空间因子,多源叠加。

该模型的关键性质(有测试断言):单目标直射炮的威胁不随编队人数增长
(多目标上限);溅射炮对密集编队比对散开编队更疼;混合编队只被
"能打它的那部分火力"威胁。

威胁过滤四档:`AUTO`(默认,每次规划按选中编队推断)/ GROUND / AIR / MIXED,
语义 = 只采集能覆盖该域的威胁源。

## 寻路与液体策略

- `CostModel`:格代价 = 1(距离)+ 预计受伤 × HARM_TILE_SCALE × riskWeight。
  玩家侧"谨慎←→激进"滑杆映射 riskWeight ∈ [0.05, 8](0→0.05,50→1,100→8,
  分段指数,单调)。`shortestOnly` 时威胁项为 0(统计仍记录)。
- `AStar`:8 方向 octile,禁止穿角(对角两正交邻格阻塞则不许斜走),
  代价回调带液体减速的时间膨胀。
- 液体轮次(`PathPlanner.plan`):
  1. 严格轮:阻塞全部可溺水液体;
  2. 生存轮:放行液体寻路,按**路径实际穿越的连续液体段**做生存判定
     (`cross + reserve ≤ hitSizeTiles × drownMultiplier × floor.drownTime`),
     不可生存段封锁后重搜(最多 3 轮);
  3. 替代轮:目标被液体围死时在候选半径内找替代终点重试严格轮;
  4. 强制轮:全放行,路线标记 liquidRound=2(渲染加 "!")。
- 护盾圈(ForceProjector.realRadius)在运行时合成 extraBlocked,视为不可通行。

## 自动模式调度

- 自动规划优先走"威胁避让带"(`ThreatGrid.avoidanceMask`,按 threatExpand
  格外扩威胁包络、内圈盲区保持敞开);找不到路或被迫替代终点时整体放宽
  回代价加权寻路——放宽量即 threatExpand 的语义上限。
- 多集群时按 spreadTicks 把一次规划分摊到多帧(`AutoPlanRun` 状态机,
  每帧处理 ⌈集群数/分帧数⌉ 个);静止降频(签名不变时按 idleSlow 倍率拉长
  重算间隔)与它正交。
- 手动发电集群模式:每个有价值群画一条"接近点→群心"的渗透路线
  (approach 点取玩家侧或最近炮塔侧),与编队集群规划相互独立。

## 指令下发

`MoveDispatcher` 唯一出口 `Call.commandUnits(player, ids, null, null, pos, queue, finalBatch)`
—— 与玩家右键指挥完全同通道。节流:整路重发间隔、逐路点包间隔、
≥16 单位分批(按 hitSize 降序、大单位先行)。路点推进按到达半径,
但下一路点进入可溺水液体段时收紧为严格到点(0.75 格),防止抄角下水;
路线签名变化才重发。

## UI 形态

- HUD 回退(`HudController`):模式/伤害/控制三块,左上角堆叠,开关
  同时管两态。
- OverlayUI 桥(`OverlayUiBridge`):反射 `mindustryX.features.ui.OverlayUI`
  的 `INSTANCE/init/registerWindow`;附着探测版本宽容(宿主有 `isAttached()`
  就用,否则读私有 `group` 字段看是否已在场景中,再不行信任幂等的 `init()`),
  注册自家三个窗口时尽力开启 `resizable`(可拉伸),懒绑定、失败静默回退
  HUD,绝不覆盖玩家在 OverlayUI 面板里的显示/钉住选择。
- 设置页(`SettingsPage`):`Vars.ui.settings.addCategory`,分组 + 专业模式
  折叠,全部即时生效;不暴露任何算法名。

## 状态与生命周期

`BspMod` 构造器注册全部事件(ClientLoad/WorldLoad/PlayerChat/ClientChat/
Trigger.update/Trigger.draw);`BspController.resetWorld()` 在换世界时清空
路线、指令与威胁缓存。headless/mobile 直接不注册客户端内容。
- 通行性缓存(每世界一份 TileEnv 快照)在建筑数量变化时自动失效重建
  (`WorldScanner.ready()` 内的 stamp 检查),即规格第 7 章的缓存失效要求。
