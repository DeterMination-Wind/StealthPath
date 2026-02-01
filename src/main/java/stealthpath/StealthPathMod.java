package stealthpath;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.input.KeyCode;
import arc.input.KeyBind;
import arc.math.Mathf;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.Dialog;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.math.geom.Vec2;
import arc.util.Align;
import arc.util.Strings;
import arc.util.Time;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.gen.Unit;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Tile;
import mindustry.world.Block;
import mindustry.world.blocks.defense.ForceProjector;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.meta.BuildVisibility;

import java.util.Arrays;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mindustry.Vars.content;
import static mindustry.Vars.control;
import static mindustry.Vars.mobile;
import static mindustry.Vars.player;
import static mindustry.Vars.renderer;
import static mindustry.Vars.state;
import static mindustry.Vars.tilesize;
import static mindustry.Vars.ui;
import static mindustry.Vars.world;

public class StealthPathMod extends mindustry.mod.Mod{
    private static final String keyEnabled = "sp-enabled";
    private static final String keyTargetMode = "sp-target-mode";
    private static final String keyTargetBlock = "sp-target-block";
    private static final String keyPathDuration = "sp-path-duration";
    private static final String keyPathWidth = "sp-path-width";
    private static final String keyPreviewRefresh = "sp-preview-refresh";
    private static final String keyThreatMode = "sp-threat-mode";
    private static final String keyGenPathColor = "sp-arrow-color";
    private static final String keyMousePathColor = "sp-mouse-path-color";
    private static final String keyGenClusterMaxPaths = "sp-gencluster-maxpaths";
    private static final String keyGenClusterStartFromCore = "sp-gencluster-start-core";
    private static final String keyGenClusterMinSize = "sp-gencluster-minsize";
    private static final String keyAutoSafeDamageThreshold = "sp-auto-safe-damage-threshold";
    private static final String keyAutoColorDead = "sp-auto-color-dead";
    private static final String keyAutoColorWarn = "sp-auto-color-warn";
    private static final String keyAutoColorSafe = "sp-auto-color-safe";

    private static final int targetModeCore = 0;
    private static final int targetModeNearest = 1;
    private static final int targetModeBlock = 2;
    private static final int targetModeGenCluster = 3;
    private static final int targetModeCoreMouse = 4;

    private static final int threatModeUnset = -1;
    private static final int threatModeGround = 0;
    private static final int threatModeAir = 1;
    private static final int threatModeBoth = 2;

    private static final String excludedGeneratorCombustion = "combustion-generator";
    private static final String excludedGeneratorTurbine = "turbine-condenser";
    private static final int genClusterLinkDistTiles = 8;
    private static final int genClusterNearTurretDistTiles = 12;
    private static final int genClusterMinDrawTiles = 12;
    private static final int genClusterFallbackBacktrackTiles = 16;

    private static boolean keybindsRegistered;
    private static KeyBind keybindTurrets;
    private static KeyBind keybindAll;
    private static KeyBind keybindModifier;
    private static KeyBind keybindCycleMode;
    private static KeyBind keybindThreatMode;
    private static KeyBind keybindAutoMouse;
    private static KeyBind keybindAutoAttack;

    private final Seq<RenderPath> drawPaths = new Seq<>();
    private float drawUntil = 0f;
    private final Color genPathColor = new Color(0.235f, 0.48f, 1f, 1f);
    private final Color mousePathColor = new Color(0.635f, 0.486f, 0.898f, 1f);
    private final Color autoDeadColor = new Color(1f, 0.23f, 0.19f, 1f);
    private final Color autoWarnColor = new Color(1f, 0.84f, 0.0f, 1f);
    private final Color autoSafeColor = new Color(0.2f, 0.78f, 0.35f, 1f);
    private float lastDamage = 0f;
    private boolean lastIncludeUnits = false;
    private final Seq<Building> tmpBuildings = new Seq<>();
    private final Seq<Unit> tmpUnits = new Seq<>();

    private int lastCycleBaseMode = targetModeCore;

    private boolean liveRefreshWasDown = false;
    private int liveLastMode = -1;
    private int liveLastThreatMode = Integer.MIN_VALUE;
    private int liveLastStartPacked = Integer.MIN_VALUE;
    private int liveLastMousePacked = Integer.MIN_VALUE;
    private boolean liveLastIncludeUnits = false;
    private float liveNextCompute = 0f;

    private static final int autoModeOff = 0;
    private static final int autoModeMouse = 1;
    private static final int autoModeAttack = 2;

    private int autoMode = autoModeOff;
    private int autoLastStartPacked = Integer.MIN_VALUE;
    private int autoLastGoalPacked = Integer.MIN_VALUE;
    private int autoLastThreatMode = Integer.MIN_VALUE;
    private float autoNextCompute = 0f;

    private int attackTargetX = -1;
    private int attackTargetY = -1;
    private int attackTargetPacked = -1;
    private int bufferedTargetX = -1;
    private int bufferedTargetY = -1;
    private int bufferedTargetPacked = -1;

    private float autoNextNoUnitsToast = 0f;

    // Cached passability (expensive to recompute every frame).
    private int passableCacheKey = Integer.MIN_VALUE;
    private int passableCacheWidth = -1;
    private int passableCacheHeight = -1;
    private boolean[] passableCache;
    private int passableCacheRevision = 0;
    private int passableCacheUsedRevision = -1;

    private static final Pattern coordPattern = Pattern.compile("\\((-?\\d+)\\s*,\\s*(-?\\d+)\\)");

    public StealthPathMod(){
        Events.on(ClientLoadEvent.class, e -> {
            ensureDefaults();
            registerKeybinds();
            registerSettings();
            refreshGenPathColor();
            refreshMousePathColor();
            refreshAutoColors();
            registerTriggers();
        });

        Events.on(ClientChatEvent.class, e -> onChatMessage(e.message));
        Events.on(PlayerChatEvent.class, e -> onChatMessage(e.message));
        Events.on(WorldLoadEvent.class, e -> {
            clearPaths();
            bufferedTargetPacked = -1;
            bufferedTargetX = -1;
            bufferedTargetY = -1;
            attackTargetPacked = -1;
            attackTargetX = -1;
            attackTargetY = -1;
            invalidatePassableCache();
        });

        Events.on(BlockBuildEndEvent.class, e -> invalidatePassableCache());
        Events.on(BlockDestroyEvent.class, e -> invalidatePassableCache());
    }

    private void invalidatePassableCache(){
        passableCacheRevision++;
    }

    private void ensureDefaults(){
        Core.settings.defaults(keyEnabled, true);
        Core.settings.defaults(keyTargetMode, targetModeCore);
        Core.settings.defaults(keyTargetBlock, "");
        Core.settings.defaults(keyPathDuration, 10);
        Core.settings.defaults(keyPathWidth, 2);
        Core.settings.defaults(keyPreviewRefresh, 6);
        Core.settings.defaults(keyThreatMode, threatModeUnset);
        Core.settings.defaults(keyGenPathColor, "3c7bff");
        Core.settings.defaults(keyMousePathColor, "a27ce5");
        Core.settings.defaults(keyGenClusterMaxPaths, 3);
        Core.settings.defaults(keyGenClusterStartFromCore, false);
        Core.settings.defaults(keyGenClusterMinSize, 2);
        Core.settings.defaults(keyAutoSafeDamageThreshold, 10);
        Core.settings.defaults(keyAutoColorDead, "ff3b30");
        Core.settings.defaults(keyAutoColorWarn, "ffd60a");
        Core.settings.defaults(keyAutoColorSafe, "34c759");
    }

    private void registerKeybinds(){
        if(keybindsRegistered) return;
        keybindsRegistered = true;

        keybindTurrets = KeyBind.add("sp_path_turrets", KeyCode.x, "stealthpath");
        keybindAll = KeyBind.add("sp_path_all", KeyCode.y, "stealthpath");
        keybindModifier = KeyBind.add("sp_modifier", KeyCode.unset, "stealthpath");
        keybindCycleMode = KeyBind.add("sp_mode_cycle", KeyCode.k, "stealthpath");
        keybindThreatMode = KeyBind.add("sp_threat_cycle", KeyCode.l, "stealthpath");
        keybindAutoMouse = KeyBind.add("sp_auto_mouse", KeyCode.n, "stealthpath");
        keybindAutoAttack = KeyBind.add("sp_auto_attack", KeyCode.m, "stealthpath");
    }

    private void registerSettings(){
        if(ui == null || ui.settings == null) return;

        ui.settings.addCategory("@sp.category", table -> {
            table.checkPref(keyEnabled, true);
            table.sliderPref(keyPathDuration, 10, 0, 60, 5, v -> v == 0 ? "inf" : v + "s");
            table.sliderPref(keyPathWidth, 2, 1, 6, 1, v -> String.valueOf(v));
            table.sliderPref(keyPreviewRefresh, 6, 1, 60, 1, v -> Strings.autoFixed(v / 60f, 2) + "s");
            table.textPref(keyGenPathColor, "3c7bff", v -> refreshGenPathColor());
            table.textPref(keyMousePathColor, "a27ce5", v -> refreshMousePathColor());

            table.row();
            table.sliderPref(keyAutoSafeDamageThreshold, 10, 0, 200, 1, v -> String.valueOf(v));
            table.textPref(keyAutoColorSafe, "34c759", v -> refreshAutoColors());
            table.textPref(keyAutoColorWarn, "ffd60a", v -> refreshAutoColors());
            table.textPref(keyAutoColorDead, "ff3b30", v -> refreshAutoColors());

            table.row();
            table.sliderPref(keyGenClusterMaxPaths, 3, 1, 10, 1, v -> String.valueOf(v));
            table.sliderPref(keyGenClusterMinSize, 2, 2, 10, 1, v -> String.valueOf(v));
            table.checkPref(keyGenClusterStartFromCore, false);

            table.row();
            addTargetRow(table);
        });
    }

    private void addTargetRow(Table table){
        table.table(Tex.button, t -> {
            t.left().margin(10f);
            t.add("@sp.setting.target.mode").left().width(170f);

            TextButton modeButton = t.button("", Styles.flatt, this::cycleTargetMode)
                .growX()
                .height(40f)
                .padLeft(8f)
                .get();

            modeButton.update(() -> modeButton.setText(targetModeDisplay(Core.settings.getInt(keyTargetMode, targetModeCore))));
        }).growX().padTop(6f);

        table.row();
        table.table(Tex.button, t -> {
            t.left().margin(10f);
            t.add("@sp.setting.target.block").left().width(170f);

            TextButton selectButton = t.button("", Styles.flatt, () -> showBlockSelectDialog(block -> Core.settings.put(keyTargetBlock, block == null ? "" : block.name)))
                .growX()
                .height(40f)
                .padLeft(8f)
                .get();

            selectButton.update(() -> {
                Block block = selectedTargetBlock();
                boolean enabled = Core.settings.getInt(keyTargetMode, targetModeCore) == targetModeBlock;
                selectButton.setDisabled(!enabled);
                selectButton.setText(block == null ? Core.bundle.get("sp.setting.target.block.none") : block.localizedName);
            });
        }).growX().padTop(6f);
    }

    private void cycleTargetMode(){
        int cur = Core.settings.getInt(keyTargetMode, targetModeCore);
        int next = (cur + 1) % 5;
        Core.settings.put(keyTargetMode, next);
    }

    private String targetModeDisplay(int mode){
        switch(mode){
            case targetModeCore:
                return Core.bundle.get("sp.setting.target.mode.core");
            case targetModeNearest:
                return Core.bundle.get("sp.setting.target.mode.nearest");
            case targetModeBlock:
                return Core.bundle.get("sp.setting.target.mode.block");
            case targetModeGenCluster:
                return Core.bundle.get("sp.setting.target.mode.gencluster");
            case targetModeCoreMouse:
                return Core.bundle.get("sp.setting.target.mode.coremouse");
            default:
                return Core.bundle.get("sp.setting.target.mode.core");
        }
    }

    private void showBlockSelectDialog(Cons<Block> consumer){
        BaseDialog dialog = new BaseDialog("@sp.setting.target.block");
        dialog.addCloseButton();

        String[] searchText = {""};

        Table list = new Table();
        list.top().left();
        list.defaults().growX().height(54f).pad(2f);

        ScrollPane pane = new ScrollPane(list);
        pane.setFadeScrollBars(false);

        Runnable rebuild = () -> {
            list.clearChildren();

            list.button("@sp.setting.target.block.none", Styles.flatt, () -> {
                dialog.hide();
                consumer.get(null);
            }).row();

            String query = searchText[0] == null ? "" : searchText[0].trim().toLowerCase(Locale.ROOT);

            for(Block block : content.blocks()){
                if(block == null) continue;
                if(block.category == null) continue;
                if(!block.isVisible()) continue;
                if(block.buildVisibility == BuildVisibility.hidden) continue;
                if(!block.hasBuilding()) continue;

                if(!query.isEmpty()){
                    String name = block.name.toLowerCase(Locale.ROOT);
                    String localized = Strings.stripColors(block.localizedName).toLowerCase(Locale.ROOT);
                    if(!name.contains(query) && !localized.contains(query)){
                        continue;
                    }
                }

                list.button(b -> {
                    b.left();
                    b.image(block.uiIcon).size(32f).padRight(8f);
                    b.add(block.localizedName).left().growX().wrap();
                    b.add(block.name).color(Color.gray).padLeft(8f).right();
                }, Styles.flatt, () -> {
                    dialog.hide();
                    consumer.get(block);
                }).row();
            }
        };

        dialog.cont.table(t -> {
            t.left();
            t.image(Icon.zoom).padRight(8f);
            t.field("", text -> {
                searchText[0] = text;
                rebuild.run();
            }).growX().get().setMessageText("@players.search");
        }).growX().padBottom(6f);

        dialog.cont.row();
        dialog.cont.add(pane).grow().minHeight(320f);

        dialog.shown(rebuild);
        dialog.show();
    }

    private Block selectedTargetBlock(){
        String name = Core.settings.getString(keyTargetBlock, "");
        if(name == null || name.trim().isEmpty()) return null;
        return content.block(name);
    }

    private void registerTriggers(){
        Events.run(Trigger.update, this::update);
        Events.run(Trigger.draw, this::draw);
    }

    private void update(){
        if(mobile) return;
        if(!Core.settings.getBool(keyEnabled, true)) return;
        if(!state.isGame() || world == null || world.isGenerating() || player == null || player.unit() == null) return;

        if(Core.scene.hasKeyboard() || Core.scene.hasDialog()) return;

        Unit unit = player.unit();

        if(drawUntil > 0f && Time.time > drawUntil){
            clearPaths();
        }

        if(keybindTurrets == null || keybindAll == null || keybindModifier == null || keybindCycleMode == null || keybindThreatMode == null || keybindAutoMouse == null || keybindAutoAttack == null){
            registerKeybinds();
        }

        if(Core.settings.getInt(keyThreatMode, threatModeUnset) == threatModeUnset){
            Core.settings.put(keyThreatMode, unit.isFlying() ? threatModeAir : threatModeGround);
        }

        boolean modifierRequired = keybindModifier != null && keybindModifier.value != null && keybindModifier.value.key != null && keybindModifier.value.key != KeyCode.unset;
        boolean modifierDown = !modifierRequired || Core.input.keyDown(keybindModifier);

        boolean cycleTap = modifierDown && keybindCycleMode != null && Core.input.keyTap(keybindCycleMode);
        boolean threatTap = modifierDown && keybindThreatMode != null && Core.input.keyTap(keybindThreatMode);
        boolean autoMouseTap = modifierDown && keybindAutoMouse != null && Core.input.keyTap(keybindAutoMouse);
        boolean autoAttackTap = modifierDown && keybindAutoAttack != null && Core.input.keyTap(keybindAutoAttack);

        boolean turretsTap = modifierDown && keybindTurrets != null && Core.input.keyTap(keybindTurrets);
        boolean allTap = modifierDown && keybindAll != null && Core.input.keyTap(keybindAll);

        boolean turretsDown = modifierDown && keybindTurrets != null && Core.input.keyDown(keybindTurrets);
        boolean allDown = modifierDown && keybindAll != null && Core.input.keyDown(keybindAll);
        boolean previewDown = turretsDown || allDown;
        boolean previewIncludeUnits = allDown;

        if(cycleTap){
            cycleDisplayMode();
        }

        if(threatTap){
            cycleThreatMode();
        }

        if(autoMouseTap){
            toggleAutoMode(autoModeMouse);
        }else if(autoAttackTap){
            toggleAutoMode(autoModeAttack);
        }

        if(previewDown){
            lastIncludeUnits = previewIncludeUnits;
            liveRefresh(unit, previewIncludeUnits);
        }else if(liveRefreshWasDown){
            // released: restore normal timeout behavior
            int seconds = Core.settings.getInt(keyPathDuration, 10);
            drawUntil = seconds <= 0 ? Float.POSITIVE_INFINITY : Time.time + seconds * 60f;
            liveRefreshWasDown = false;
            liveNextCompute = 0f;
        }

        if(turretsTap){
            lastIncludeUnits = false;
            computePath(false, true);
        }else if(allTap){
            lastIncludeUnits = true;
            computePath(true, true);
        }

        autoHandleRightClickMove();
        autoUpdate();
    }

    private void autoHandleRightClickMove(){
        if(autoMode == autoModeOff) return;
        if(!state.isGame() || world == null || player == null) return;
        if(!Core.input.keyTap(KeyCode.mouseRight)) return;

        // Only act when the player has an RTS selection; otherwise, let the default right-click behavior work.
        if(control == null || control.input == null || control.input.selectedUnits == null || !control.input.selectedUnits.any()) return;

        ControlledCluster cluster = computeControlledCluster();
        if(cluster == null) return;

        int goalX, goalY;
        if(autoMode == autoModeMouse){
            goalX = clamp(worldToTile(Core.input.mouseWorldX()), 0, world.width() - 1);
            goalY = clamp(worldToTile(Core.input.mouseWorldY()), 0, world.height() - 1);
        }else{
            if(bufferedTargetPacked == -1) return;
            goalX = bufferedTargetX;
            goalY = bufferedTargetY;
        }

        ThreatMap map = buildThreatMap(cluster.moveUnit, true, cluster.moveFlying, cluster.threatsAir, cluster.threatsGround, cluster.maxHitRadiusWorld);
        ShiftedPath best = findBestShiftedClusterPath(cluster, map, goalX, goalY);
        if(best == null || best.path == null || best.path.isEmpty()) return;

        issueRtsMoveAlongPath(best.path, map.width);
    }

    private void toggleAutoMode(int requested){
        if(autoMode == requested){
            autoMode = autoModeOff;
            autoNextCompute = 0f;
            drawUntil = 0f;
            clearPaths();
            showToast(requested == autoModeMouse ? "@sp.toast.auto.mouse.off" : "@sp.toast.auto.attack.off", 2.5f);
        }else{
            autoMode = requested;
            autoNextCompute = 0f;
            autoLastStartPacked = Integer.MIN_VALUE;
            autoLastGoalPacked = Integer.MIN_VALUE;
            autoLastThreatMode = Integer.MIN_VALUE;
            drawUntil = Float.POSITIVE_INFINITY;

            if(autoMode == autoModeAttack && bufferedTargetPacked == -1){
                showToast("@sp.toast.auto.attack.wait", 3f);
            }else{
                showToast(autoMode == autoModeMouse ? "@sp.toast.auto.mouse.on" : "@sp.toast.auto.attack.on", 2.5f);
            }
        }
    }

    private void autoUpdate(){
        if(autoMode == autoModeOff) return;
        if(!state.isGame() || world == null || player == null) return;

        float baseInterval = Math.max(1f, Core.settings.getInt(keyPreviewRefresh, 6));
        if(Time.time < autoNextCompute) return;

        ControlledCluster cluster = computeControlledCluster();
        if(cluster == null){
            if(Time.time >= autoNextNoUnitsToast){
                showToast("@sp.toast.auto.no-units", 2.5f);
                autoNextNoUnitsToast = Time.time + 90f;
            }
            autoNextCompute = Time.time + baseInterval * 2f;
            return;
        }

        int goalX, goalY;
        if(autoMode == autoModeMouse){
            goalX = clamp(worldToTile(Core.input.mouseWorldX()), 0, world.width() - 1);
            goalY = clamp(worldToTile(Core.input.mouseWorldY()), 0, world.height() - 1);
        }else{
            if(bufferedTargetPacked == -1){
                autoNextCompute = Time.time + baseInterval * 2f;
                return;
            }
            goalX = bufferedTargetX;
            goalY = bufferedTargetY;
        }

        int startX = worldToTile(cluster.x);
        int startY = worldToTile(cluster.y);
        int startPacked = startX + startY * world.width();

        int goalPacked = goalX + goalY * world.width();
        boolean unchanged = startPacked == autoLastStartPacked && goalPacked == autoLastGoalPacked && cluster.threatMode == autoLastThreatMode;

        ThreatMap map = buildThreatMap(cluster.moveUnit, true, cluster.moveFlying, cluster.threatsAir, cluster.threatsGround, cluster.maxHitRadiusWorld);
        computeClusterToGoal(cluster, map, goalX, goalY, false);
        drawUntil = Float.POSITIVE_INFINITY;

        autoLastStartPacked = startPacked;
        autoLastGoalPacked = goalPacked;
        autoLastThreatMode = cluster.threatMode;

        // If the goal/start didn't move, slow down updates to reduce CPU usage.
        float slowInterval = Math.min(240f, baseInterval * 8f);
        autoNextCompute = Time.time + (unchanged ? slowInterval : baseInterval);
    }

    private ControlledCluster computeControlledCluster(){
        if(player == null) return null;
        Unit playerUnit = player.unit();
        if(playerUnit == null) return null;

        tmpUnits.clear();

        if(control != null && control.input != null && control.input.selectedUnits != null && control.input.selectedUnits.any()){
            for(int i = 0; i < control.input.selectedUnits.size; i++){
                Unit u = control.input.selectedUnits.get(i);
                if(u == null) continue;
                if(u.team != player.team()) continue;
                if(!u.isAdded() || u.dead()) continue;
                tmpUnits.add(u);
            }
        }

        // Auto modes require a real controlled group (not just the player alone).
        if(tmpUnits.isEmpty()) return null;
        if(tmpUnits.size == 1 && tmpUnits.first() == playerUnit) return null;

        float sx = 0f, sy = 0f;
        int flying = 0;

        boolean hasGround = false;
        boolean hasAir = false;

        float minSpeedGround = Float.POSITIVE_INFINITY;
        float minHpGround = Float.POSITIVE_INFINITY;
        float minSpeedAir = Float.POSITIVE_INFINITY;
        float minHpAir = Float.POSITIVE_INFINITY;
        float maxHitRadiusWorld = 0f;

        Unit first = tmpUnits.first();
        float leftX = first.x, leftY = first.y;
        float rightX = first.x, rightY = first.y;
        float topX = first.x, topY = first.y;
        float bottomX = first.x, bottomY = first.y;

        for(int i = 0; i < tmpUnits.size; i++){
            Unit u = tmpUnits.get(i);
            sx += u.x;
            sy += u.y;

            if(u.x < leftX){
                leftX = u.x;
                leftY = u.y;
            }
            if(u.x > rightX){
                rightX = u.x;
                rightY = u.y;
            }
            if(u.y > topY){
                topX = u.x;
                topY = u.y;
            }
            if(u.y < bottomY){
                bottomX = u.x;
                bottomY = u.y;
            }

            maxHitRadiusWorld = Math.max(maxHitRadiusWorld, u.hitSize / 2f);

            if(u.isFlying()){
                flying++;
                hasAir = true;
                minSpeedAir = Math.min(minSpeedAir, u.speed());
                minHpAir = Math.min(minHpAir, u.health());
            }else{
                hasGround = true;
                minSpeedGround = Math.min(minSpeedGround, u.speed());
                minHpGround = Math.min(minHpGround, u.health());
            }
        }

        float cx = sx / tmpUnits.size;
        float cy = sy / tmpUnits.size;

        int threatMode;
        boolean moveFlying;
        boolean threatsAir, threatsGround;

        if(flying == tmpUnits.size){
            threatMode = threatModeAir;
            moveFlying = true;
            threatsAir = true;
            threatsGround = false;
        }else if(flying == 0){
            threatMode = threatModeGround;
            moveFlying = false;
            threatsAir = false;
            threatsGround = true;
        }else{
            threatMode = threatModeBoth;
            // Mixed: keep paths compatible with ground units.
            moveFlying = false;
            threatsAir = true;
            threatsGround = true;
        }

        Unit moveUnit = null;
        for(int i = 0; i < tmpUnits.size; i++){
            Unit u = tmpUnits.get(i);
            if(u == null) continue;
            if(moveUnit == null){
                moveUnit = u;
            }else if(!moveFlying && moveUnit.isFlying() && !u.isFlying()){
                // Prefer a ground unit for ground movement constraints.
                moveUnit = u;
            }
        }

        float moveSpeed;
        if(moveFlying){
            moveSpeed = hasAir ? minSpeedAir : moveUnit.speed();
        }else{
            moveSpeed = hasGround ? minSpeedGround : moveUnit.speed();
        }

        if(!Float.isFinite(minSpeedGround)) minSpeedGround = Float.NaN;
        if(!Float.isFinite(minHpGround)) minHpGround = Float.NaN;
        if(!Float.isFinite(minSpeedAir)) minSpeedAir = Float.NaN;
        if(!Float.isFinite(minHpAir)) minHpAir = Float.NaN;

        return new ControlledCluster(cx, cy, moveUnit, moveSpeed, threatMode, moveFlying, threatsAir, threatsGround,
            hasGround, minSpeedGround, minHpGround,
            hasAir, minSpeedAir, minHpAir,
            leftX, leftY, rightX, rightY, topX, topY, bottomX, bottomY,
            maxHitRadiusWorld);
    }

    private boolean includeUnitsFromLast(){
        return lastIncludeUnits;
    }

    private void cycleDisplayMode(){
        int current = Core.settings.getInt(keyTargetMode, targetModeCore);
        int next;

        if(current == targetModeGenCluster){
            next = targetModeCoreMouse;
        }else if(current == targetModeCoreMouse){
            next = lastCycleBaseMode;
            if(next == targetModeGenCluster || next == targetModeCoreMouse){
                next = targetModeCore;
            }
        }else{
            lastCycleBaseMode = current;
            next = targetModeGenCluster;
        }

        Core.settings.put(keyTargetMode, next);
        showToast(targetModeDisplay(next), 2f);
    }

    private void liveRefresh(Unit unit, boolean includeUnits){
        // throttle heavy work: only recompute on meaningful changes or at a low rate when units are included.
        int mode = Core.settings.getInt(keyTargetMode, targetModeCore);
        int threatMode = Core.settings.getInt(keyThreatMode, threatModeGround);

        float interval = Math.max(1f, Core.settings.getInt(keyPreviewRefresh, 6));

        int startX = worldToTile(unit.x);
        int startY = worldToTile(unit.y);
        int startPacked = startX + startY * world.width();

        int mouseX = clamp(worldToTile(Core.input.mouseWorldX()), 0, world.width() - 1);
        int mouseY = clamp(worldToTile(Core.input.mouseWorldY()), 0, world.height() - 1);
        int mousePacked = mouseX + mouseY * world.width();

        boolean changed = !liveRefreshWasDown
            || mode != liveLastMode
            || threatMode != liveLastThreatMode
            || includeUnits != liveLastIncludeUnits
            || startPacked != liveLastStartPacked
            || (mode == targetModeCoreMouse && mousePacked != liveLastMousePacked);

        if(includeUnits && Time.time >= liveNextCompute){
            changed = true;
        }

        if(!changed) return;
        if(Time.time < liveNextCompute) return;

        computePath(includeUnits, false);
        drawUntil = Float.POSITIVE_INFINITY;

        liveRefreshWasDown = true;
        liveLastMode = mode;
        liveLastThreatMode = threatMode;
        liveLastIncludeUnits = includeUnits;
        liveLastStartPacked = startPacked;
        liveLastMousePacked = mousePacked;

        liveNextCompute = Time.time + interval;
    }

    private void cycleThreatMode(){
        int cur = Core.settings.getInt(keyThreatMode, threatModeGround);
        int next;
        if(cur == threatModeGround){
            next = threatModeAir;
        }else if(cur == threatModeAir){
            next = threatModeBoth;
        }else{
            next = threatModeGround;
        }

        Core.settings.put(keyThreatMode, next);
        showToast(threatModeToast(next), 2f);
    }

    private static String threatModeToast(int mode){
        switch(mode){
            case threatModeAir:
                return "@sp.toast.threatmode.air";
            case threatModeBoth:
                return "@sp.toast.threatmode.both";
            case threatModeGround:
            default:
                return "@sp.toast.threatmode.ground";
        }
    }

    private void draw(){
        if(!Core.settings.getBool(keyEnabled, true)) return;
        if(!state.isGame() || drawPaths.isEmpty()) return;

        Draw.draw(Layer.overlayUI + 0.01f, () -> {
            float baseWidth = Core.settings.getInt(keyPathWidth, 2);
            float stroke = baseWidth / Math.max(0.0001f, renderer.getDisplayScale());

            Lines.stroke(stroke);

            float prevFontScale = Fonts.outline.getScaleX();
            float textScale = 0.6f / Math.max(0.0001f, renderer.getDisplayScale());
            Fonts.outline.getData().setScale(textScale);

            for(int p = 0; p < drawPaths.size; p++){
                RenderPath path = drawPaths.get(p);
                if(path == null || path.points == null || path.points.isEmpty()) continue;

                Draw.color(path.color, 0.85f);

                Seq<Pos> pts = path.points;
                for(int i = 0; i < pts.size - 1; i++){
                    Pos a = pts.get(i);
                    Pos b = pts.get(i + 1);
                    if(a == null || b == null) continue;
                    Lines.line(a.x, a.y, b.x, b.y, false);
                }

                Pos start = pts.first();
                Pos end = pts.peek();
                if(start != null) Fill.circle(start.x, start.y, stroke * 2.2f);
                if(end != null) Fill.circle(end.x, end.y, stroke * 2.6f);

                if(path.damageText != null){
                    int labelIndex = Math.min(pts.size - 1, Math.max(0, pts.size / 2));
                    Pos labelPos = pts.get(labelIndex);
                    if(labelPos != null){
                        float offset = Math.max(stroke * 10f, tilesize * 0.45f);
                        Draw.color(path.color);
                        Fonts.outline.draw(path.damageText, labelPos.x, labelPos.y + offset, Align.center);
                    }
                }
            }

            Fonts.outline.getData().setScale(prevFontScale);
            Draw.reset();
        });
    }

    private void clearPaths(){
        drawPaths.clear();
        drawUntil = 0f;
        lastDamage = 0f;
    }

    private void computePath(boolean includeUnits, boolean showToasts){
        clearPaths();

        if(!state.isGame() || world == null || player == null || player.unit() == null){
            if(showToasts) showToast("@sp.toast.no-path", 2.5f);
            return;
        }

        Unit unit = player.unit();

        int threatMode = Core.settings.getInt(keyThreatMode, threatModeGround);
        boolean moveFlying = threatMode == threatModeAir;
        boolean threatsAir = threatMode == threatModeAir || threatMode == threatModeBoth;
        boolean threatsGround = threatMode == threatModeGround || threatMode == threatModeBoth;

        int mode = Core.settings.getInt(keyTargetMode, targetModeCore);
        if(mode == targetModeGenCluster){
            ThreatMap map = buildThreatMap(unit, includeUnits, moveFlying, threatsAir, threatsGround, unit.hitSize / 2f);
            computeGenClusterPaths(unit, map, includeUnits, moveFlying, threatsAir, threatsGround, showToasts);
            return;
        }
        if(mode == targetModeCoreMouse){
            ThreatMap map = buildThreatMap(unit, includeUnits, moveFlying, threatsAir, threatsGround, unit.hitSize / 2f);
            computePlayerToMousePath(unit, map, moveFlying, showToasts);
            return;
        }

        ThreatMap map = buildThreatMap(unit, includeUnits, moveFlying, threatsAir, threatsGround, unit.hitSize / 2f);

        int startX = worldToTile(unit.x);
        int startY = worldToTile(unit.y);
        if(!inBounds(map, startX, startY)){
            if(showToasts) showToast("@sp.toast.no-path", 2.5f);
            return;
        }

        if(!map.passable[startX + startY * map.width]){
            int startIdx = findNearestPassable(map, startX, startY, 8);
            if(startIdx == -1){
                if(showToasts) showToast("@sp.toast.no-path", 2.5f);
                return;
            }
            startX = startIdx % map.width;
            startY = startIdx / map.width;
        }

        Building target = findTarget(mode);
        if(target == null){
            if(showToasts) showToast("@sp.toast.no-target", 2.5f);
            return;
        }

        IntSeq goals = buildGoalTiles(map, unit, target, moveFlying);
        if(goals.isEmpty()){
            if(showToasts) showToast("@sp.toast.no-path", 2.5f);
            return;
        }

        boolean[] goalMask = new boolean[map.size];
        for(int i = 0; i < goals.size; i++){
            goalMask[goals.items[i]] = true;
        }

        float speed = unit.speed();
        PathResult safe = findPath(map, startX, startY, goals, goalMask, true, speed);
        PathResult result = safe != null ? safe : findPath(map, startX, startY, goals, goalMask, false, speed);

        if(result == null){
            if(showToasts) showToast("@sp.toast.no-path", 2.5f);
            return;
        }

        IntSeq compact = compactPath(result.path, map.width);
        float dmg = estimateDamage(map, result.path, unit);
        drawPaths.add(new RenderPath(toWorldPoints(compact, map.width, unit, target), safe != null ? Pal.heal : Pal.remove, dmg));
        lastDamage = dmg;

        int seconds = Core.settings.getInt(keyPathDuration, 10);
        drawUntil = seconds <= 0 ? Float.POSITIVE_INFINITY : Time.time + seconds * 60f;

        if(showToasts) showToast(Core.bundle.format("sp.toast.path", result.path.size, Strings.autoFixed(lastDamage, 1)), 3f);
    }

    private Seq<Building> anchorBuildings(){
        tmpBuildings.clear();
        if(world == null) return tmpBuildings;

        for(int y = 0; y < world.height(); y++){
            for(int x = 0; x < world.width(); x++){
                Tile tile = world.tile(x, y);
                if(tile == null) continue;
                Building b = tile.build;
                if(b == null) continue;
                if(b.tileX() != x || b.tileY() != y) continue;
                tmpBuildings.add(b);
            }
        }

        return tmpBuildings;
    }

    private Building findTarget(int mode){
        Block wanted = selectedTargetBlock();
        if(mode == targetModeBlock && wanted == null){
            // If no block is selected (or the saved block name is invalid), fall back to nearest enemy building.
            mode = targetModeNearest;
        }

        float px = player.unit().x;
        float py = player.unit().y;

        Seq<Building> builds = anchorBuildings();

        Building best = null;
        float bestDst2 = Float.POSITIVE_INFINITY;
        Building bestDerelict = null;
        float bestDerelictDst2 = Float.POSITIVE_INFINITY;

        for(int i = 0; i < builds.size; i++){
            Building b = builds.get(i);
            if(b == null) continue;
            if(b.team == player.team()) continue;

            boolean isDerelict = b.team == Team.derelict;

            if(mode == targetModeCore){
                if(!(b.block instanceof CoreBlock)) continue;
            }else if(mode == targetModeBlock){
                if(b.block != wanted) continue;
            }

            float dst2 = Mathf.dst2(px, py, b.x, b.y);
            if(isDerelict){
                if(dst2 < bestDerelictDst2){
                    bestDerelictDst2 = dst2;
                    bestDerelict = b;
                }
            }else{
                if(dst2 < bestDst2){
                    bestDst2 = dst2;
                    best = b;
                }
            }
        }

        if(best != null) return best;
        if(mode == targetModeNearest) return bestDerelict;

        // fallback: any enemy building
        bestDerelict = null;
        bestDerelictDst2 = Float.POSITIVE_INFINITY;
        for(int i = 0; i < builds.size; i++){
            Building b = builds.get(i);
            if(b == null) continue;
            if(b.team == player.team()) continue;

            boolean isDerelict = b.team == Team.derelict;

            float dst2 = Mathf.dst2(px, py, b.x, b.y);
            if(isDerelict){
                if(dst2 < bestDerelictDst2){
                    bestDerelictDst2 = dst2;
                    bestDerelict = b;
                }
            }else{
                if(dst2 < bestDst2){
                    bestDst2 = dst2;
                    best = b;
                }
            }
        }

        return best != null ? best : bestDerelict;
    }

    private void computeGenClusterPaths(Unit unit, ThreatMap map, boolean includeUnits, boolean moveFlying, boolean threatsAir, boolean threatsGround, boolean showToasts){
        int minSize = Math.max(2, Core.settings.getInt(keyGenClusterMinSize, 2));
        int maxPaths = Math.max(1, Core.settings.getInt(keyGenClusterMaxPaths, 3));
        boolean startFromPlayer = Core.settings.getBool(keyGenClusterStartFromCore, false);

        float startWorldX = unit.x;
        float startWorldY = unit.y;

        int startX = worldToTile(startWorldX);
        int startY = worldToTile(startWorldY);
        if(!inBounds(map, startX, startY)){
            if(showToasts) showToast("@sp.toast.no-path", 2.5f);
            return;
        }

        if(!map.passable[startX + startY * map.width]){
            int startIdx = findNearestPassable(map, startX, startY, 8);
            if(startIdx == -1){
                if(showToasts) showToast("@sp.toast.no-path", 2.5f);
                return;
            }
            startX = startIdx % map.width;
            startY = startIdx / map.width;
        }

        Seq<Seq<Building>> clusters = findEnemyGeneratorClusters(minSize);
        if(clusters.isEmpty()){
            if(showToasts) showToast("@sp.toast.no-target", 2.5f);
            return;
        }

        Seq<Building> turrets = collectEnemyTurretBuildings(unit, threatsAir, threatsGround);

        Seq<ClusterPath> candidates = new Seq<>();
        float speed = unit.speed();

        for(int i = 0; i < clusters.size; i++){
            Seq<Building> cluster = clusters.get(i);
            if(cluster == null || cluster.size < minSize) continue;

            Building target = pickClusterRepresentative(cluster);
            if(target == null) continue;

            IntSeq goals = buildGoalTiles(map, unit, target, moveFlying);
            if(goals.isEmpty()) continue;

            boolean[] goalMask = new boolean[map.size];
            for(int g = 0; g < goals.size; g++){
                goalMask[goals.items[g]] = true;
            }

            PathResult safe = findPath(map, startX, startY, goals, goalMask, true, speed);
            PathResult result = safe != null ? safe : findPath(map, startX, startY, goals, goalMask, false, speed);
            if(result == null) continue;

            float damage = estimateDamage(map, result.path, unit);
            candidates.add(new ClusterPath(target, result.path, safe != null, damage));
        }

        if(candidates.isEmpty()){
            if(showToasts) showToast("@sp.toast.no-path", 2.5f);
            return;
        }

        candidates.sort((a, b) -> {
            if(Math.abs(a.damage - b.damage) > 0.0001f){
                return a.damage < b.damage ? -1 : 1;
            }
            return Integer.compare(a.path.size, b.path.size);
        });

        int take = Math.min(maxPaths, candidates.size);
        float bestDamage = Float.POSITIVE_INFINITY;

        for(int i = 0; i < take; i++){
            ClusterPath cp = candidates.get(i);
            if(cp == null || cp.path == null || cp.path.isEmpty()) continue;

            IntSeq segment;
            if(startFromPlayer){
                segment = cp.path;
            }else{
                int startIndex = findPathStartIndexNearTurrets(cp.path, map.width, turrets);
                startIndex = Math.max(0, Math.min(cp.path.size - 1, startIndex));
                segment = new IntSeq();
                for(int s = startIndex; s < cp.path.size; s++){
                    segment.add(cp.path.items[s]);
                }
            }

            float dmg = estimateDamage(map, segment, unit);
            if(dmg < bestDamage) bestDamage = dmg;

            IntSeq compact = compactPath(segment, map.width);
            Seq<Pos> points = startFromPlayer
                ? toWorldPointsFromTilesWithStart(compact, map.width, startWorldX, startWorldY, cp.target)
                : toWorldPointsFromTiles(compact, map.width, cp.target);

            drawPaths.add(new RenderPath(points, genPathColor, dmg));
        }

        lastDamage = Float.isFinite(bestDamage) ? bestDamage : 0f;

        int seconds = Core.settings.getInt(keyPathDuration, 10);
        drawUntil = seconds <= 0 ? Float.POSITIVE_INFINITY : Time.time + seconds * 60f;

        if(showToasts) showToast(Core.bundle.format("sp.toast.gencluster.multi", take, Strings.autoFixed(lastDamage, 1)), 3f);
    }

    private void computePlayerToMousePath(Unit unit, ThreatMap map, boolean moveFlying, boolean showToasts){
        int startX = worldToTile(unit.x);
        int startY = worldToTile(unit.y);
        if(!inBounds(map, startX, startY)){
            if(showToasts) showToast("@sp.toast.no-path", 2.5f);
            return;
        }

        if(!map.passable[startX + startY * map.width]){
            int startIdx = findNearestPassable(map, startX, startY, 8);
            if(startIdx == -1){
                if(showToasts) showToast("@sp.toast.no-path", 2.5f);
                return;
            }
            startX = startIdx % map.width;
            startY = startIdx / map.width;
        }

        int goalX = clamp(worldToTile(Core.input.mouseWorldX()), 0, map.width - 1);
        int goalY = clamp(worldToTile(Core.input.mouseWorldY()), 0, map.height - 1);
        int goalIdx = findNearestPassable(map, goalX, goalY, 6);
        if(goalIdx == -1){
            if(showToasts) showToast("@sp.toast.no-path", 2.5f);
            return;
        }

        IntSeq goals = new IntSeq();
        goals.add(goalIdx);

        boolean[] goalMask = new boolean[map.size];
        goalMask[goalIdx] = true;

        float speed = unit.speed();
        PathResult safe = findPath(map, startX, startY, goals, goalMask, true, speed);
        PathResult result = safe != null ? safe : findPath(map, startX, startY, goals, goalMask, false, speed);

        if(result == null){
            if(showToasts) showToast("@sp.toast.no-path", 2.5f);
            return;
        }

        IntSeq compact = compactPath(result.path, map.width);
        float dmg = estimateDamage(map, result.path, unit);
        drawPaths.add(new RenderPath(toWorldPointsFromTilesWithStart(compact, map.width, unit.x, unit.y, null), mousePathColor, dmg));

        lastDamage = dmg;

        int seconds = Core.settings.getInt(keyPathDuration, 10);
        drawUntil = seconds <= 0 ? Float.POSITIVE_INFINITY : Time.time + seconds * 60f;

        if(showToasts) showToast(Core.bundle.format("sp.toast.path", result.path.size, Strings.autoFixed(lastDamage, 1)), 3f);
    }

    private void computeClusterToGoal(ControlledCluster cluster, ThreatMap map, int goalX, int goalY, boolean showToasts){
        clearPaths();

        ShiftedPath sp = findBestShiftedClusterPath(cluster, map, goalX, goalY);
        if(sp == null || sp.path == null || sp.path.isEmpty()){
            if(showToasts) showToast("@sp.toast.no-path", 2.5f);
            return;
        }

        IntSeq compact = compactPath(sp.path, map.width);

        Color c = autoPathColor(cluster, sp.dmgGround, sp.dmgAir, sp.maxDmg);
        drawPaths.add(new RenderPath(toWorldPointsFromTilesWithStart(compact, map.width, cluster.x + sp.dx, cluster.y + sp.dy, null), c, sp.maxDmg));

        lastDamage = sp.maxDmg;

        int seconds = Core.settings.getInt(keyPathDuration, 10);
        drawUntil = seconds <= 0 ? Float.POSITIVE_INFINITY : Time.time + seconds * 60f;

        if(showToasts) showToast(Core.bundle.format("sp.toast.path", sp.path.size, Strings.autoFixed(lastDamage, 1)), 3f);
    }

    private ShiftedPath findBestShiftedClusterPath(ControlledCluster cluster, ThreatMap map, int goalX, int goalY){
        if(cluster == null || map == null) return null;

        goalX = clamp(goalX, 0, map.width - 1);
        goalY = clamp(goalY, 0, map.height - 1);
        int goalIdx = findNearestPassable(map, goalX, goalY, 6);
        if(goalIdx == -1) return null;

        IntSeq goals = new IntSeq();
        goals.add(goalIdx);
        boolean[] goalMask = new boolean[map.size];
        goalMask[goalIdx] = true;

        int startX = clamp(worldToTile(cluster.x), 0, map.width - 1);
        int startY = clamp(worldToTile(cluster.y), 0, map.height - 1);
        if(!inBounds(map, startX, startY)) return null;
        if(!map.passable[startX + startY * map.width]){
            int startIdx = findNearestPassable(map, startX, startY, 10);
            if(startIdx == -1) return null;
            startX = startIdx % map.width;
            startY = startIdx / map.width;
        }

        float speed = Math.max(0.0001f, cluster.speed);
        PathResult safe = findPath(map, startX, startY, goals, goalMask, true, speed);
        PathResult base = safe != null ? safe : findPath(map, startX, startY, goals, goalMask, false, speed);
        if(base == null || base.path == null || base.path.isEmpty()) return null;

        // Plan from center first, then try small offsets to keep the whole formation's collision volume safe.
        float offset = Mathf.clamp(cluster.maxHitRadiusWorld, tilesize * 0.5f, tilesize * 3f);
        float[] dxs = new float[]{0f, -offset, offset, 0f, 0f};
        float[] dys = new float[]{0f, 0f, 0f, offset, -offset};

        ShiftedPath best = null;

        for(int i = 0; i < dxs.length; i++){
            float dx = dxs[i];
            float dy = dys[i];

            IntSeq shifted = shiftTilePath(map, base.path, dx, dy);
            if(shifted == null || shifted.isEmpty()) continue;

            float dmgGround = cluster.hasGround ? estimateDamage(map, shifted, cluster.minSpeedGround) : Float.NaN;
            float dmgAir = cluster.hasAir ? estimateDamage(map, shifted, cluster.minSpeedAir) : Float.NaN;

            float maxDmg = 0f;
            if(cluster.hasGround && Float.isFinite(dmgGround)) maxDmg = Math.max(maxDmg, dmgGround);
            if(cluster.hasAir && Float.isFinite(dmgAir)) maxDmg = Math.max(maxDmg, dmgAir);

            if(best == null
                || maxDmg + 0.0001f < best.maxDmg
                || (Math.abs(maxDmg - best.maxDmg) <= 0.0001f && shifted.size < best.path.size)){
                best = new ShiftedPath(shifted, dx, dy, dmgGround, dmgAir, maxDmg);
            }
        }

        return best;
    }

    private static IntSeq shiftTilePath(ThreatMap map, IntSeq base, float dx, float dy){
        if(map == null || base == null || base.isEmpty()) return null;

        IntSeq out = new IntSeq(base.size);
        int last = Integer.MIN_VALUE;

        for(int i = 0; i < base.size; i++){
            int idx = base.items[i];
            int tx = idx % map.width;
            int ty = idx / map.width;

            float wx = tileToWorld(tx) + tilesize / 2f + dx;
            float wy = tileToWorld(ty) + tilesize / 2f + dy;

            int ntx = clamp(worldToTile(wx), 0, map.width - 1);
            int nty = clamp(worldToTile(wy), 0, map.height - 1);
            int nidx = ntx + nty * map.width;

            if(!map.passable[nidx]) return null;

            if(nidx != last){
                out.add(nidx);
                last = nidx;
            }
        }

        return out;
    }

    private void issueRtsMoveAlongPath(IntSeq tilePath, int width){
        if(tilePath == null || tilePath.isEmpty()) return;
        if(player == null || player.unit() == null) return;
        if(control == null || control.input == null || control.input.selectedUnits == null || !control.input.selectedUnits.any()) return;

        int[] idsTmp = new int[control.input.selectedUnits.size];
        int count = 0;
        for(int i = 0; i < control.input.selectedUnits.size; i++){
            Unit u = control.input.selectedUnits.get(i);
            if(u == null) continue;
            if(u.team != player.team()) continue;
            if(!u.isAdded() || u.dead()) continue;
            idsTmp[count++] = u.id;
        }
        if(count <= 0) return;

        int[] ids = Arrays.copyOf(idsTmp, count);

        IntSeq compact = compactPath(tilePath, width);
        if(compact.isEmpty()) compact = tilePath;

        int maxWaypoints = 12;
        int step = Math.max(1, compact.size / maxWaypoints);

        Seq<Vec2> waypoints = new Seq<>();
        for(int i = 0; i < compact.size; i += step){
            int idx = compact.items[i];
            int tx = idx % width;
            int ty = idx / width;
            waypoints.add(new Vec2(tileToWorld(tx) + tilesize / 2f, tileToWorld(ty) + tilesize / 2f));
        }

        int lastIdx = compact.items[compact.size - 1];
        int lastTx = lastIdx % width;
        int lastTy = lastIdx / width;
        Vec2 end = new Vec2(tileToWorld(lastTx) + tilesize / 2f, tileToWorld(lastTy) + tilesize / 2f);
        if(waypoints.isEmpty() || waypoints.peek().dst2(end) > 0.1f){
            waypoints.add(end);
        }

        if(waypoints.isEmpty()) return;

        for(int i = 0; i < waypoints.size; i++){
            boolean queue = i != 0;
            Call.commandUnits(player, ids, null, null, waypoints.get(i), queue, true);
        }
    }

    private static int findNearestPassable(ThreatMap map, int x, int y, int radius){
        if(inBounds(map, x, y)){
            int idx = x + y * map.width;
            if(map.passable[idx]) return idx;
        }

        int best = -1;
        float bestDst2 = Float.POSITIVE_INFINITY;

        for(int dy = -radius; dy <= radius; dy++){
            for(int dx = -radius; dx <= radius; dx++){
                int nx = x + dx;
                int ny = y + dy;
                if(!inBounds(map, nx, ny)) continue;
                int nidx = nx + ny * map.width;
                if(!map.passable[nidx]) continue;

                float d2 = dx * dx + dy * dy;
                if(d2 < bestDst2){
                    bestDst2 = d2;
                    best = nidx;
                }
            }
        }

        return best;
    }

    private Seq<Seq<Building>> findEnemyGeneratorClusters(int minSize){
        Seq<Building> gens = new Seq<>();
        Seq<Building> derelictGens = new Seq<>();

        Seq<Building> builds = anchorBuildings();
        for(int i = 0; i < builds.size; i++){
            Building b = builds.get(i);
            if(b == null) continue;
            if(b.team == player.team()) continue;
            if(!(b.block instanceof PowerGenerator)) continue;
            if(isExcludedGenerator(b.block)) continue;

            if(b.team == Team.derelict){
                derelictGens.add(b);
            }else{
                gens.add(b);
            }
        }

        Seq<Seq<Building>> clusters = new Seq<>();
        if(gens.isEmpty()){
            gens = derelictGens;
        }
        if(gens.isEmpty()) return clusters;

        boolean[] visited = new boolean[gens.size];
        float maxDst2 = tilesize * genClusterLinkDistTiles;
        maxDst2 *= maxDst2;

        for(int i = 0; i < gens.size; i++){
            if(visited[i]) continue;
            visited[i] = true;

            Seq<Building> cluster = new Seq<>();
            cluster.add(gens.get(i));

            boolean changed = true;
            while(changed){
                changed = false;
                for(int a = 0; a < cluster.size; a++){
                    Building ba = cluster.get(a);
                    for(int j = 0; j < gens.size; j++){
                        if(visited[j]) continue;
                        Building bb = gens.get(j);
                        if(bb == null) continue;
                        if(Mathf.dst2(ba.x, ba.y, bb.x, bb.y) <= maxDst2){
                            visited[j] = true;
                            cluster.add(bb);
                            changed = true;
                        }
                    }
                }
            }

            if(cluster.size >= minSize){
                clusters.add(cluster);
            }
        }

        return clusters;
    }

    private static boolean isExcludedGenerator(Block block){
        if(block == null || block.name == null) return false;
        return excludedGeneratorCombustion.equals(block.name) || excludedGeneratorTurbine.equals(block.name);
    }

    private static Building pickClusterRepresentative(Seq<Building> cluster){
        if(cluster == null || cluster.isEmpty()) return null;

        float sx = 0f, sy = 0f;
        int count = 0;
        for(int i = 0; i < cluster.size; i++){
            Building b = cluster.get(i);
            if(b == null) continue;
            sx += b.x;
            sy += b.y;
            count++;
        }
        if(count <= 0) return null;

        float cx = sx / count;
        float cy = sy / count;

        Building best = null;
        float bestDst2 = Float.POSITIVE_INFINITY;
        for(int i = 0; i < cluster.size; i++){
            Building b = cluster.get(i);
            if(b == null) continue;
            float dst2 = Mathf.dst2(cx, cy, b.x, b.y);
            if(dst2 < bestDst2){
                bestDst2 = dst2;
                best = b;
            }
        }

        return best;
    }

    private Seq<Building> collectEnemyTurretBuildings(Unit playerUnit, boolean threatsAir, boolean threatsGround){
        Seq<Building> out = new Seq<>();
        Seq<Building> derelictOut = new Seq<>();

        Seq<Building> builds = anchorBuildings();
        for(int i = 0; i < builds.size; i++){
            Building b = builds.get(i);
            if(b == null) continue;
            if(b.team == player.team()) continue;
            if(!(b.block instanceof Turret)) continue;
            if(!(b instanceof Turret.TurretBuild)) continue;

            Turret turret = (Turret)b.block;
            Turret.TurretBuild tb = (Turret.TurretBuild)b;

            if(turret.targetHealing) continue;
            if(!((threatsAir && turret.targetAir) || (threatsGround && turret.targetGround))) continue;

            if(tb.estimateDps() <= 0.0001f) continue;

            if(b.team == Team.derelict){
                derelictOut.add(b);
            }else{
                out.add(b);
            }
        }

        return out.isEmpty() ? derelictOut : out;
    }

    private static int findPathStartIndexNearTurrets(IntSeq path, int width, Seq<Building> turrets){
        if(path == null || path.isEmpty()) return 0;
        if(turrets == null || turrets.isEmpty()) return 0;

        float near = tilesize * genClusterNearTurretDistTiles;
        float near2 = near * near;

        int earliestNear = -1;
        float bestDst2 = Float.POSITIVE_INFINITY;
        int closestIndex = Math.max(0, path.size - 1);

        for(int i = 0; i < path.size; i++){
            int tile = path.items[i];
            float wx = tileToWorld(tile % width);
            float wy = tileToWorld(tile / width);

            float minDst2 = Float.POSITIVE_INFINITY;
            for(int t = 0; t < turrets.size; t++){
                Building turret = turrets.get(t);
                if(turret == null) continue;
                float dst2 = Mathf.dst2(wx, wy, turret.x, turret.y);
                if(dst2 < minDst2){
                    minDst2 = dst2;
                }
            }

            if(minDst2 < bestDst2){
                bestDst2 = minDst2;
                closestIndex = i;
            }

            if(earliestNear == -1 && minDst2 <= near2){
                earliestNear = i;
            }
        }

        int start = earliestNear != -1 ? earliestNear : Math.max(0, closestIndex - genClusterFallbackBacktrackTiles);

        int minLen = Math.min(genClusterMinDrawTiles, path.size);
        if(path.size - start < minLen){
            start = Math.max(0, path.size - minLen);
        }

        return start;
    }

    private void refreshGenPathColor(){
        String value = Core.settings.getString(keyGenPathColor, "3c7bff");
        if(!tryParseHexColor(value, genPathColor)){
            genPathColor.set(0.235f, 0.48f, 1f, 1f);
        }
    }

    private void refreshMousePathColor(){
        String value = Core.settings.getString(keyMousePathColor, "a27ce5");
        if(!tryParseHexColor(value, mousePathColor)){
            mousePathColor.set(0.635f, 0.486f, 0.898f, 1f);
        }
    }

    private static boolean tryParseHexColor(String text, Color out){
        if(text == null) return false;
        String value = text.trim();
        if(value.startsWith("#")){
            value = value.substring(1);
        }

        int len = value.length();
        if(len != 6 && len != 8) return false;

        try{
            long parsed = Long.parseLong(value, 16);
            if(len == 6){
                float r = ((parsed >> 16) & 0xff) / 255f;
                float g = ((parsed >> 8) & 0xff) / 255f;
                float b = (parsed & 0xff) / 255f;
                out.set(r, g, b, 1f);
            }else{
                float r = ((parsed >> 24) & 0xff) / 255f;
                float g = ((parsed >> 16) & 0xff) / 255f;
                float b = ((parsed >> 8) & 0xff) / 255f;
                float a = (parsed & 0xff) / 255f;
                out.set(r, g, b, a);
            }
            return true;
        }catch(Throwable ignored){
            return false;
        }
    }

    private static Seq<Pos> toWorldPoints(IntSeq tilePath, int width, Unit unit, Building target){
        Seq<Pos> out = new Seq<>();
        out.add(new Pos(unit.x, unit.y));

        for(int i = 0; i < tilePath.size; i++){
            int idx = tilePath.items[i];
            int tx = idx % width;
            int ty = idx / width;
            out.add(new Pos(tileToWorld(tx), tileToWorld(ty)));
        }

        out.add(new Pos(target.x, target.y));
        return out;
    }

    private static Seq<Pos> toWorldPointsFromTiles(IntSeq tilePath, int width, Building target){
        Seq<Pos> out = new Seq<>();
        if(tilePath == null || tilePath.isEmpty()) return out;

        for(int i = 0; i < tilePath.size; i++){
            int idx = tilePath.items[i];
            int tx = idx % width;
            int ty = idx / width;
            out.add(new Pos(tileToWorld(tx), tileToWorld(ty)));
        }

        if(target != null){
            out.add(new Pos(target.x, target.y));
        }

        return out;
    }

    private static Seq<Pos> toWorldPointsFromTilesWithStart(IntSeq tilePath, int width, float startWorldX, float startWorldY, Building target){
        Seq<Pos> out = new Seq<>();
        out.add(new Pos(startWorldX, startWorldY));

        if(tilePath != null && !tilePath.isEmpty()){
            for(int i = 0; i < tilePath.size; i++){
                int idx = tilePath.items[i];
                int tx = idx % width;
                int ty = idx / width;
                out.add(new Pos(tileToWorld(tx), tileToWorld(ty)));
            }
        }

        if(target != null){
            out.add(new Pos(target.x, target.y));
        }

        return out;
    }

    private static float tileToWorld(int tile){
        return tile * tilesize;
    }

    private static int worldToTile(float world){
        return Math.round(world / tilesize);
    }

    private static boolean inBounds(ThreatMap map, int x, int y){
        return x >= 0 && y >= 0 && x < map.width && y < map.height;
    }

    private ThreatMap buildThreatMap(Unit unit, boolean includeUnits, boolean moveFlying, boolean threatsAir, boolean threatsGround, float clearanceWorld){
        ThreatMap map = new ThreatMap(world.width(), world.height());

        fillPassable(map, unit, moveFlying, clearanceWorld);
        applyShieldNoGoZones(map, clearanceWorld);

        Seq<Threat> threats = collectThreats(unit, includeUnits, threatsAir, threatsGround);
        if(threats.isEmpty()){
            return map;
        }

        for(int i = 0; i < threats.size; i++){
            Threat t = threats.get(i);
            if(t.dps <= 0.0001f || t.range <= 0.0001f) continue;

            float r = t.range;
            float r2 = r * r;
            float mr = Math.max(0f, t.minRange);
            float mr2 = mr * mr;

            int minX = clamp((int)Math.ceil((t.x - r) / tilesize), 0, map.width - 1);
            int maxX = clamp((int)Math.floor((t.x + r) / tilesize), 0, map.width - 1);
            int minY = clamp((int)Math.ceil((t.y - r) / tilesize), 0, map.height - 1);
            int maxY = clamp((int)Math.floor((t.y + r) / tilesize), 0, map.height - 1);

            for(int ty = minY; ty <= maxY; ty++){
                float wy = tileToWorld(ty);
                for(int tx = minX; tx <= maxX; tx++){
                    float wx = tileToWorld(tx);
                    float dx = wx - t.x;
                    float dy = wy - t.y;
                    float d2 = dx * dx + dy * dy;
                    if(d2 <= r2 && d2 >= mr2){
                        map.risk[tx + ty * map.width] += t.dps;
                    }
                }
            }
        }

        return map;
    }

    private void applyShieldNoGoZones(ThreatMap map, float clearanceWorld){
        if(map == null || map.passable == null) return;
        if(!state.isGame() || world == null || player == null) return;

        float inflate = Math.max(0f, clearanceWorld);

        Seq<Building> builds = anchorBuildings();
        for(int i = 0; i < builds.size; i++){
            Building b = builds.get(i);
            if(b == null) continue;
            if(b.team == player.team()) continue;
            if(!(b.block instanceof ForceProjector)) continue;
            if(!(b instanceof ForceProjector.ForceBuild)) continue;
            ForceProjector.ForceBuild fb = (ForceProjector.ForceBuild)b;

            float r = fb.realRadius() + inflate;
            if(r <= 0.001f) continue;

            float r2 = r * r;

            int minX = clamp((int)Math.ceil((b.x - r) / tilesize), 0, map.width - 1);
            int maxX = clamp((int)Math.floor((b.x + r) / tilesize), 0, map.width - 1);
            int minY = clamp((int)Math.ceil((b.y - r) / tilesize), 0, map.height - 1);
            int maxY = clamp((int)Math.floor((b.y + r) / tilesize), 0, map.height - 1);

            for(int ty = minY; ty <= maxY; ty++){
                float wy = tileToWorld(ty);
                for(int tx = minX; tx <= maxX; tx++){
                    float wx = tileToWorld(tx);
                    float dx = wx - b.x;
                    float dy = wy - b.y;
                    float d2 = dx * dx + dy * dy;
                    if(d2 <= r2){
                        map.passable[tx + ty * map.width] = false;
                    }
                }
            }
        }
    }

    private void fillPassable(ThreatMap map, Unit unit, boolean treatFlying, float clearanceWorld){
        if(map == null) return;
        if(treatFlying){
            Arrays.fill(map.passable, true);
            return;
        }

        if(world == null || unit == null || unit.type == null){
            Arrays.fill(map.passable, false);
            return;
        }

        int clearanceTiles = clearanceTiles(clearanceWorld);

        int key = unit.type.id;
        key = key * 31 + (unit.type.allowLegStep ? 1 : 0);
        key = key * 31 + (unit.type.naval ? 1 : 0);
        key = key * 31 + clearanceTiles;
        key = key * 31 + map.width;
        key = key * 31 + map.height;

        boolean canUseCache = passableCache != null
            && passableCacheKey == key
            && passableCacheWidth == map.width
            && passableCacheHeight == map.height
            && passableCacheUsedRevision == passableCacheRevision;

        if(canUseCache){
            System.arraycopy(passableCache, 0, map.passable, 0, map.size);
            return;
        }

        if(passableCache == null || passableCache.length != map.size){
            passableCache = new boolean[map.size];
        }

        for(int y = 0; y < map.height; y++){
            for(int x = 0; x < map.width; x++){
                int idx = x + y * map.width;
                boolean p = passableFor(unit, world.tile(x, y), false);
                if(p && clearanceTiles > 0){
                    for(int dy = -clearanceTiles; dy <= clearanceTiles && p; dy++){
                        for(int dx = -clearanceTiles; dx <= clearanceTiles; dx++){
                            int nx = x + dx;
                            int ny = y + dy;
                            if(nx < 0 || ny < 0 || nx >= map.width || ny >= map.height){
                                p = false;
                                break;
                            }
                            if(!passableFor(unit, world.tile(nx, ny), false)){
                                p = false;
                                break;
                            }
                        }
                    }
                }
                map.passable[idx] = p;
                passableCache[idx] = p;
            }
        }

        passableCacheKey = key;
        passableCacheWidth = map.width;
        passableCacheHeight = map.height;
        passableCacheUsedRevision = passableCacheRevision;
    }

    private static int clearanceTiles(float clearanceWorld){
        if(!Float.isFinite(clearanceWorld) || clearanceWorld <= 0.001f) return 0;
        return Math.max(0, Mathf.ceil(clearanceWorld / tilesize));
    }

    private static boolean passableFor(Unit unit, Tile tile, boolean treatFlying){
        if(tile == null) return false;
        if(treatFlying) return true;

        if(unit.type.allowLegStep){
            return !tile.legSolid();
        }

        if(unit.type.naval){
            return !tile.solid() && tile.floor().isLiquid;
        }

        return !tile.solid();
    }

    private Seq<Threat> collectThreats(Unit playerUnit, boolean includeUnits, boolean threatsAir, boolean threatsGround){
        Seq<Threat> out = new Seq<>();
        Seq<Threat> derelictOut = new Seq<>();

        Seq<Building> builds = anchorBuildings();
        for(int i = 0; i < builds.size; i++){
            Building b = builds.get(i);
            if(b == null) continue;
            if(b.team == player.team()) continue;
            if(!(b.block instanceof Turret)) continue;
            if(!(b instanceof Turret.TurretBuild)) continue;

            Turret turret = (Turret)b.block;
            Turret.TurretBuild tb = (Turret.TurretBuild)b;

            if(turret.targetHealing) continue;
            if(!((threatsAir && turret.targetAir) || (threatsGround && turret.targetGround))) continue;

            float dps = tb.estimateDps();
            if(dps <= 0.0001f) continue;

            Threat t = new Threat(b.x, b.y, tb.range(), tb.minRange(), dps);
            if(b.team == Team.derelict){
                derelictOut.add(t);
            }else{
                out.add(t);
            }
        }

        if(includeUnits){
            for(int i = 0; i < Groups.unit.size(); i++){
                Unit u = Groups.unit.index(i);
                if(u == null) continue;
                if(u.team == player.team()) continue;
                if(!u.isAdded() || u.dead()) continue;
                if(!((threatsAir && u.type.targetAir) || (threatsGround && u.type.targetGround))) continue;

                float range = u.range();
                if(range <= 0.0001f) continue;

                float dps = u.type.estimateDps();
                if(dps <= 0.0001f) continue;

                Threat t = new Threat(u.x, u.y, range, 0f, dps);
                if(u.team == Team.derelict){
                    derelictOut.add(t);
                }else{
                    out.add(t);
                }
            }
        }

        if(out.isEmpty()){
            out.addAll(derelictOut);
        }

        return out;
    }

    private static IntSeq buildGoalTiles(ThreatMap map, Unit unit, Building target, boolean moveFlying){
        IntSeq out = new IntSeq();
        if(target == null) return out;

        if(moveFlying){
            int tx = worldToTile(target.x);
            int ty = worldToTile(target.y);
            if(inBounds(map, tx, ty)){
                out.add(tx + ty * map.width);
            }
            return out;
        }

        int bx = target.tileX();
        int by = target.tileY();
        int size = Math.max(1, target.block.size);

        // Ring around building footprint.
        for(int x = bx - 1; x <= bx + size; x++){
            addGoalIfPassable(map, out, x, by - 1);
            addGoalIfPassable(map, out, x, by + size);
        }
        for(int y = by; y <= by + size - 1; y++){
            addGoalIfPassable(map, out, bx - 1, y);
            addGoalIfPassable(map, out, bx + size, y);
        }

        return out;
    }

    private static void addGoalIfPassable(ThreatMap map, IntSeq out, int x, int y){
        if(!inBounds(map, x, y)) return;
        int idx = x + y * map.width;
        if(!map.passable[idx]) return;
        out.add(idx);
    }

    private static int clamp(int value, int min, int max){
        return Math.max(min, Math.min(max, value));
    }

    private static PathResult findPath(ThreatMap map, int startX, int startY, IntSeq goals, boolean[] goalMask, boolean safeOnly, float speed){
        int startIdx = startX + startY * map.width;
        if(!map.passable[startIdx]) return null;
        if(safeOnly && map.risk[startIdx] > 0.0001f) return null;

        float[] best = new float[map.size];
        Arrays.fill(best, Float.POSITIVE_INFINITY);
        int[] parent = new int[map.size];
        Arrays.fill(parent, -1);
        boolean[] closed = new boolean[map.size];

        PriorityQueue<Node> open = new PriorityQueue<>();

        best[startIdx] = 0f;
        open.add(new Node(startIdx, heuristic(map, startX, startY, goals, safeOnly), 0f));

        while(!open.isEmpty()){
            Node cur = open.poll();
            int idx = cur.idx;

            if(closed[idx]) continue;
            if(cur.g != best[idx]) continue;
            closed[idx] = true;

            if(goalMask[idx]){
                return new PathResult(reconstruct(parent, idx));
            }

            int x = idx % map.width;
            int y = idx / map.width;

            for(int dy = -1; dy <= 1; dy++){
                for(int dx = -1; dx <= 1; dx++){
                    if(dx == 0 && dy == 0) continue;
                    int nx = x + dx;
                    int ny = y + dy;
                    if(nx < 0 || ny < 0 || nx >= map.width || ny >= map.height) continue;

                    int nidx = nx + ny * map.width;
                    if(closed[nidx]) continue;
                    if(!map.passable[nidx]) continue;
                    if(safeOnly && map.risk[nidx] > 0.0001f) continue;

                    // No cutting corners through blocked tiles.
                    if(dx != 0 && dy != 0){
                        int aidx = (x + dx) + y * map.width;
                        int bidx = x + (y + dy) * map.width;
                        if(!map.passable[aidx] || !map.passable[bidx]) continue;
                        if(safeOnly && (map.risk[aidx] > 0.0001f || map.risk[bidx] > 0.0001f)) continue;
                    }

                    float step = (dx == 0 || dy == 0) ? 1f : Mathf.sqrt2;

                    float ng;
                    if(safeOnly){
                        ng = best[idx] + step;
                    }else{
                        float distWorld = tilesize * step;
                        float avgRisk = (map.risk[idx] + map.risk[nidx]) * 0.5f;
                        float v = Math.max(0.0001f, speed);

                        // damage = DPS * seconds; seconds = dist / (speed * 60).
                        float dmg = avgRisk * (distWorld / (v * 60f));
                        float tie = step * 0.001f;
                        ng = best[idx] + dmg + tie;
                    }

                    if(ng >= best[nidx]) continue;

                    best[nidx] = ng;
                    parent[nidx] = idx;

                    float h = heuristic(map, nx, ny, goals, safeOnly);
                    open.add(new Node(nidx, ng + h, ng));
                }
            }
        }

        return null;
    }

    private static float heuristic(ThreatMap map, int x, int y, IntSeq goals, boolean safeOnly){
        float best = Float.POSITIVE_INFINITY;
        for(int i = 0; i < goals.size; i++){
            int idx = goals.items[i];
            int gx = idx % map.width;
            int gy = idx / map.width;
            float dst = Mathf.dst(x, y, gx, gy);
            if(dst < best) best = dst;
        }

        if(!Float.isFinite(best)) return 0f;
        return safeOnly ? best : best * 0.001f;
    }

    private static IntSeq reconstruct(int[] parent, int endIdx){
        IntSeq out = new IntSeq();
        int cur = endIdx;
        while(cur != -1){
            out.add(cur);
            cur = parent[cur];
        }
        out.reverse();
        return out;
    }

    private static IntSeq compactPath(IntSeq path, int width){
        if(path == null || path.size <= 2) return path;

        IntSeq out = new IntSeq();
        out.add(path.items[0]);

        int prev = path.items[0];
        int prevDx = 0, prevDy = 0;

        for(int i = 1; i < path.size; i++){
            int cur = path.items[i];
            int px = prev % width;
            int py = prev / width;
            int cx = cur % width;
            int cy = cur / width;

            int dx = Integer.compare(cx, px);
            int dy = Integer.compare(cy, py);

            if(i == 1){
                prevDx = dx;
                prevDy = dy;
            }else if(dx != prevDx || dy != prevDy){
                out.add(prev);
                prevDx = dx;
                prevDy = dy;
            }

            prev = cur;
        }

        out.add(path.peek());
        return out;
    }

    private static float estimateDamage(ThreatMap map, IntSeq tilePath, Unit unit){
        if(tilePath == null || tilePath.size <= 1) return 0f;

        float speed = Math.max(0.0001f, unit.speed());
        return estimateDamage(map, tilePath, speed);
    }

    private static float estimateDamage(ThreatMap map, IntSeq tilePath, float speed){
        if(tilePath == null || tilePath.size <= 1) return 0f;

        speed = Math.max(0.0001f, speed);
        float dmg = 0f;

        for(int i = 0; i < tilePath.size - 1; i++){
            int a = tilePath.items[i];
            int b = tilePath.items[i + 1];

            int ax = a % map.width;
            int ay = a / map.width;
            int bx = b % map.width;
            int by = b / map.width;

            int dx = Math.abs(bx - ax);
            int dy = Math.abs(by - ay);
            float step = (dx + dy == 1) ? 1f : Mathf.sqrt2;

            float distWorld = tilesize * step;
            float avgRisk = (map.risk[a] + map.risk[b]) * 0.5f;
            dmg += avgRisk * (distWorld / (speed * 60f));
        }

        return dmg;
    }

    private void showToast(String keyOrText, float seconds){
        if(ui == null) return;
        String text = keyOrText.startsWith("@") ? Core.bundle.get(keyOrText.substring(1)) : keyOrText;
        ui.showInfoToast(text, seconds);
    }

    private void onChatMessage(String message){
        if(message == null) return;
        String text = message.trim();
        if(text.isEmpty()) return;

        if(world == null) return;

        Matcher m = coordPattern.matcher(text);
        if(!m.find()) return;

        try{
            int x = Integer.parseInt(m.group(1));
            int y = Integer.parseInt(m.group(2));

            x = clamp(x, 0, world.width() - 1);
            y = clamp(y, 0, world.height() - 1);

            bufferedTargetX = x;
            bufferedTargetY = y;
            bufferedTargetPacked = x + y * world.width();

            // Preserve old <Attack> fields for compatibility, but buffer drives M mode now.
            attackTargetX = x;
            attackTargetY = y;
            attackTargetPacked = bufferedTargetPacked;

            if(autoMode == autoModeAttack){
                autoNextCompute = 0f;
            }
        }catch(Throwable ignored){
            // ignore
        }
    }

    private void refreshAutoColors(){
        String safe = Core.settings.getString(keyAutoColorSafe, "34c759");
        String warn = Core.settings.getString(keyAutoColorWarn, "ffd60a");
        String dead = Core.settings.getString(keyAutoColorDead, "ff3b30");

        if(!tryParseHexColor(safe, autoSafeColor)){
            autoSafeColor.set(0.2f, 0.78f, 0.35f, 1f);
        }
        if(!tryParseHexColor(warn, autoWarnColor)){
            autoWarnColor.set(1f, 0.84f, 0.0f, 1f);
        }
        if(!tryParseHexColor(dead, autoDeadColor)){
            autoDeadColor.set(1f, 0.23f, 0.19f, 1f);
        }
    }

    private Color autoPathColor(ControlledCluster cluster, float dmgGround, float dmgAir, float maxDmg){
        float safeThresh = Math.max(0f, Core.settings.getInt(keyAutoSafeDamageThreshold, 10));
        if(maxDmg < safeThresh){
            return autoSafeColor;
        }

        boolean groundDead = cluster.hasGround && Float.isFinite(dmgGround) && Float.isFinite(cluster.minHpGround) && dmgGround >= cluster.minHpGround;
        boolean airDead = cluster.hasAir && Float.isFinite(dmgAir) && Float.isFinite(cluster.minHpAir) && dmgAir >= cluster.minHpAir;

        boolean anySurvive = (cluster.hasGround && !groundDead) || (cluster.hasAir && !airDead);
        return anySurvive ? autoWarnColor : autoDeadColor;
    }

    private static class Pos{
        final float x, y;

        Pos(float x, float y){
            this.x = x;
            this.y = y;
        }
    }

    private static class RenderPath{
        final Seq<Pos> points;
        final Color color;
        final String damageText;

        RenderPath(Seq<Pos> points, Color color, float damage){
            this.points = points == null ? new Seq<Pos>() : points;
            this.color = color == null ? Color.white : color;
            this.damageText = Float.isFinite(damage) ? Strings.autoFixed(Math.max(0f, damage), 1) : null;
        }
    }

    private static class Threat{
        final float x, y;
        final float range;
        final float minRange;
        final float dps;

        Threat(float x, float y, float range, float minRange, float dps){
            this.x = x;
            this.y = y;
            this.range = Math.max(0f, range);
            this.minRange = Math.max(0f, minRange);
            this.dps = Math.max(0f, dps);
        }
    }

    private static class ThreatMap{
        final int width, height, size;
        final boolean[] passable;
        final float[] risk;

        ThreatMap(int width, int height){
            this.width = width;
            this.height = height;
            this.size = width * height;
            this.passable = new boolean[size];
            this.risk = new float[size];
        }
    }

    private static class PathResult{
        final IntSeq path;

        PathResult(IntSeq path){
            this.path = path;
        }
    }

    private static class Node implements Comparable<Node>{
        final int idx;
        final float f;
        final float g;

        Node(int idx, float f, float g){
            this.idx = idx;
            this.f = f;
            this.g = g;
        }

        @Override
        public int compareTo(Node other){
            return Float.compare(this.f, other.f);
        }
    }

    private static class ClusterPath{
        final Building target;
        final IntSeq path;
        final boolean safe;
        final float damage;

        ClusterPath(Building target, IntSeq path, boolean safe, float damage){
            this.target = target;
            this.path = path;
            this.safe = safe;
            this.damage = damage;
        }
    }

    private static class ShiftedPath{
        final IntSeq path;
        final float dx, dy;
        final float dmgGround, dmgAir;
        final float maxDmg;

        ShiftedPath(IntSeq path, float dx, float dy, float dmgGround, float dmgAir, float maxDmg){
            this.path = path;
            this.dx = dx;
            this.dy = dy;
            this.dmgGround = dmgGround;
            this.dmgAir = dmgAir;
            this.maxDmg = maxDmg;
        }
    }

    private static class ControlledCluster{
        final float x, y;
        final Unit moveUnit;
        final float speed;
        final int threatMode;
        final boolean moveFlying;
        final boolean threatsAir, threatsGround;
        final boolean hasGround;
        final float minSpeedGround;
        final float minHpGround;
        final boolean hasAir;
        final float minSpeedAir;
        final float minHpAir;

        final float leftX, leftY;
        final float rightX, rightY;
        final float topX, topY;
        final float bottomX, bottomY;

        final float maxHitRadiusWorld;

        ControlledCluster(float x, float y, Unit moveUnit, float speed, int threatMode, boolean moveFlying, boolean threatsAir, boolean threatsGround,
                          boolean hasGround, float minSpeedGround, float minHpGround,
                          boolean hasAir, float minSpeedAir, float minHpAir,
                          float leftX, float leftY, float rightX, float rightY, float topX, float topY, float bottomX, float bottomY,
                          float maxHitRadiusWorld){
            this.x = x;
            this.y = y;
            this.moveUnit = moveUnit;
            this.speed = speed;
            this.threatMode = threatMode;
            this.moveFlying = moveFlying;
            this.threatsAir = threatsAir;
            this.threatsGround = threatsGround;
            this.hasGround = hasGround;
            this.minSpeedGround = minSpeedGround;
            this.minHpGround = minHpGround;
            this.hasAir = hasAir;
            this.minSpeedAir = minSpeedAir;
            this.minHpAir = minHpAir;

            this.leftX = leftX;
            this.leftY = leftY;
            this.rightX = rightX;
            this.rightY = rightY;
            this.topX = topX;
            this.topY = topY;
            this.bottomX = bottomX;
            this.bottomY = bottomY;
            this.maxHitRadiusWorld = maxHitRadiusWorld;
        }
    }
}
