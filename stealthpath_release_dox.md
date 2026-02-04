# StealthPath 发布/维护说明（release dox）

## 1. 版本号规则

采用 `Major.Minor.Patch`（大功能-小功能-bug 修复）：

- `Major`：大功能/大改动（可能影响配置/交互/兼容）
- `Minor`：新增小功能（尽量向后兼容）
- `Patch`：bug 修复/小调整

版本号需要同时更新：

- `build.gradle` 的 `version`
- `src/main/resources/mod.json` 的 `"version"`
- `README.md` 的 “Current version”

## 2. 构建

在 StealthPath 仓库根目录运行：

```powershell
./gradlew.bat --no-daemon clean jar
```

产物：

- `dist/stealth-path.zip`

## 3. 与 BEK-Tools 同步（维护者）

BEK-Tools 使用 `tools/update_submods.py` 读取本地 git checkout 的 HEAD 并合并源码：

```powershell
cd ..\\BEK-Tools
python tools/update_submods.py
```

同步后建议：

1) 更新 BEK-Tools 版本号（`build.gradle`、`src/main/resources/mod.json`，以及 README 的 StealthPath 当前版本行）
2) 构建验证：`./gradlew.bat --no-daemon clean jar jarAndroid`
3) 提交、推送、打 tag（`vX.Y.Z`）触发 GitHub Actions Release

