# betterStealthPath / 偷袭小道+ (Stealth Path+)

A threat-aware route assistant for Mindustry attack planning — a clean-room
reimplementation of the "Stealth Path" concept, built strictly from its
feature specification (`FEATURE.md`) plus public Mindustry modding knowledge.

**This repository contains no code, documents or decompiled material from the
original Stealth Path mod.** The only requirement source is `FEATURE.md`.

---

## What it does / 它做什么

Holding one key draws a low-casualty route from your units to a target,
with an expected-damage estimate; auto modes keep replanning for the selected
formation and can walk it along the route using the game's **native move
command channel** (equivalent to you right-clicking your own selected units).

按住一个键,从你的单位到目标画出一条预估受伤更低的路线并给出预计受伤;
自动模式持续为选中编队重算路线,并可沿路线用**原生移动指挥通道**行进
(等价于你本人对自己选中单位的右键指挥)。

Highlights / 亮点:

- Formation-aware threat model: projectile shape (direct / piercing line /
  splash / continuous beam), multi-target caps, hit falloff, flat armor
  mitigation, air/ground coverage — the external contract stays "one threat
  value per tile".
- `Auto` threat filter: re-inferred from the selected formation on every plan.
- Liquid & deep-water protection: block-then-relax rounds, survivable-crossing
  windows with a reserve margin, goal substitution when the target is sealed.
- Single player-facing "reckless <-> cautious" slider; no algorithm names in
  settings.
- English + Simplified Chinese bundles, key-set equality enforced at build time.
- Update check that only *informs* — opens the release page (directly or via a
  configurable mirror); never self-installs or restarts.

## Fairness boundary / 公平性边界

- Everything is a **local, visual reference**: routes and damage numbers are
  estimates drawn on your screen. Whether to follow them is your decision.
- The only influence on a game session: native move commands to **your own
  selected units**, identical to manual right-click commanding. Servers need
  nothing installed; no unit stats, damage, ranges or server rules are touched;
  no auto-firing, no attack commands.
- Command dispatch is throttled (waypoint cap, resend interval, packet
  interval, formation batching) to stay a polite client.
- The mod is `hidden` to keep mod lists clean.

## Deployment trade-off / 部署形态取舍

**Desktop-only.** This project deliberately ships no Android `classes.dex`:
every interaction is keyboard/mouse driven, and producing an untestable
"loads but does nothing" Android package would be dead weight (the classic
criticism of this mod category). Installing the jar on Android is not
supported.

## Install (desktop) / 安装(桌面)

Copy `betterStealthPath-dev.jar` (local dev build) into your Mindustry `mods/`
folder. Default hotkeys: `X` preview (turrets), `Y` preview (turrets+units),
`K` cycle target mode, `L` cycle threat filter, `N`/`M` auto modes, `J` risk
heatmap; rebind in Settings -> Controls. All behavior is opt-in via explicit
key presses; a first-use toast points at the keybind screen.

## Build / 构建

```
./gradlew build          # compiles, runs the JUnit self-test, checks bundle sync,
                         # produces build/libs/betterStealthPath-dev.jar and copies it
                         # to ../构建/betterStealthPath/betterStealthPath-dev.jar
./gradlew build -Pbundled=true   # aggregated form: no own settings category,
                                 # no own update check (see docs/release.md)
```

Requires the official Mindustry v7 (`v157`) core + arc jars in the local
Gradle cache (resolved automatically), or `-PmindustryJar=<path>` to point at
an existing jar. Bytecode target: Java 8. `minGameVersion: 154`.

## Status

Experimental — the master switch defaults to on but nothing happens without a
key press, and the first use shows a one-time guide toast (deliberate,
consistent with the README, unlike the original's mismatched messaging).

## License / Statement

Clean-room reimplementation. See `AGENTS.md` for the clean-room constraints
observed during development.
