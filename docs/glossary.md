# 术语表

| 术语 | 含义 |
| --- | --- |
| 纯逻辑核心 (`bsp.core`) | 不依赖 Mindustry/Arc、可在任意 JVM 上测试的决策层:威胁采样、路径规划、液体策略、路点压缩、集群分割、发电聚类、坐标换算 |
| 威胁值 (threat value) | 每格一个浮点数,含义为"全编队 HP/s"——编队占据该格时预计每秒损失的总体血量 |
| 编队画像 (FormationProfile) | 选中单位的聚合描述:数量、地/空构成、散布半径、最脆血量、平均护甲、最慢速度、最小碰撞尺寸、可溺水性 |
| ThreatShape | 弹种类别:DIRECT 直射 / LINE 线状穿透 / SPLASH 溅射 / CONTINUOUS 持续束;决定空间衰减与多目标行为 |
| engaged(可交战数量) | 一次交火中该威胁源能同时波及的编队成员数;受 simultaneousTargets、pierce、溅射覆盖率与地/空覆盖约束 |
| 目标域 (Domain) | GROUND / AIR / MIXED;威胁过滤的语义域。AUTO 档每次规划按编队构成推断 |
| 预计受伤 (harm) | 全编队口径的 HP 损失估计 = Σ(格威胁 + 地块伤害)× 通行时间 |
| 三色风险 (RiskBand) | SAFE(< 阈值)/ WARNING(人均存活)/ FATAL(预计死人);红阈 = 全队受伤 ≥ 最脆血量 × 人数 |
| 谨慎←→激进滑杆 (caution) | 玩家可懂的风险权衡,映射为代价模型 riskWeight ∈ [0.05, 8];替代暴露内部算法的选项 |
| 可溺水液体 (drownable liquid) | `isLiquid && drownTime > 0` 的地块(深水等);生存时长 = hitSizeTiles × drownTimeMultiplier × floor.drownTime(镜像游戏公式) |
| 液体轮次 | 严格阻塞 → 路径段级可生存评估+封锁重搜(≤3 轮)→ 目标替代 → 强制穿越(liquidRound 0/1/2 标记) |
| 目标候选半径 (candidate radius) | 目标不可达/被液体围死时,螺旋搜索替代终点的半径 |
| 集群分割 (cluster split) | 选中单位按距离(默认 5 格)连通分群,各自规划/绘制/指挥 |
| 发电集群 (power cluster) | 敌方发电机按连接距离聚成的群(排除火力/蒸汽/涡轮冷凝/太阳能类低价值单机);渗透起点默认取朝最近炮塔一侧 |
| 原生指挥通道 | `Call.commandUnits(player, ids, null, null, pos, queue, finalBatch)`——与玩家右键指挥等价、服务器无需安装的移动指令通道 |
| 双节流 | 整路重发最小间隔 + 逐路点指令包间隔;配合大编队分批避免触发服务器反 DoS |
| HUD 回退 | 无 MindustryX 时的固定位置悬浮块(模式/伤害/控制),与 OverlayUI 窗口同开关、功能等价 |
| OverlayUI 桥 | 对 `mindustryX.features.ui.OverlayUI` 的反射访问;缺失时静默回退,绝不覆盖玩家的窗口管理选择 |
| 聚合态 (bundled form) | `-Pbundled=true` 注入标记后的构建形态:不注册独立设置分类、禁用独立更新检查,门面让位给聚合方 |
| 洁净室再实现 | 只依据 FEATURE.md 与公开资料从零实现;全程不接触原版代码/文档/反编译产物 |
