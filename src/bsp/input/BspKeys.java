package bsp.input;

import arc.Core;
import arc.input.KeyBind;
import arc.input.KeyCode;
import mindustry.Vars;

/**
 * Keybind registration and gated queries. An optional modifier key makes
 * every function key require modifier+key (anti-misclick). Text fields and
 * mobile clients swallow all input.
 */
public final class BspKeys{
    public static final String
        PREVIEW_TURRETS = "bsp.previewTurrets",
        PREVIEW_UNITS = "bsp.previewUnits",
        MODIFIER = "bsp.modifier",
        CYCLE_MODE = "bsp.cycleMode",
        CYCLE_FILTER = "bsp.cycleFilter",
        AUTO_MOUSE = "bsp.autoMouse",
        AUTO_CHAT = "bsp.autoChat",
        AUTO_MOVE = "bsp.autoMove",
        HEATMAP = "bsp.heatmap";

    private KeyBind previewTurrets, previewUnits, modifier, cycleMode, cycleFilter,
        autoMouse, autoChat, autoMove, heatmap;

    private static BspKeys instance;

    public static synchronized BspKeys get(){
        if(instance == null) instance = new BspKeys();
        return instance;
    }

    private BspKeys(){
        String category = "bsp";
        // The game loads persisted bindings once at startup, before mods are
        // constructed — so every bind we add must load its own saved value or
        // the player's rebinds would fall back to defaults on each restart.
        previewTurrets = loadable(KeyBind.add(PREVIEW_TURRETS, KeyCode.x, category));
        previewUnits = loadable(KeyBind.add(PREVIEW_UNITS, KeyCode.y, category));
        modifier = loadable(KeyBind.add(MODIFIER, KeyCode.unset, category));
        cycleMode = loadable(KeyBind.add(CYCLE_MODE, KeyCode.k, category));
        cycleFilter = loadable(KeyBind.add(CYCLE_FILTER, KeyCode.l, category));
        autoMouse = loadable(KeyBind.add(AUTO_MOUSE, KeyCode.n, category));
        autoChat = loadable(KeyBind.add(AUTO_CHAT, KeyCode.m, category));
        autoMove = loadable(KeyBind.add(AUTO_MOVE, KeyCode.mouseRight, category));
        heatmap = loadable(KeyBind.add(HEATMAP, KeyCode.j, category));
    }

    private static KeyBind loadable(KeyBind bind){
        try{
            bind.load();
        }catch(Throwable ignored){
            // no settings backend yet (early registration) — defaults stay active
        }
        return bind;
    }

    private boolean gate(){
        if(Vars.mobile) return false;
        if(Core.scene != null && Core.scene.hasField()) return false;
        if(modifier.value != null && modifier.value.key != KeyCode.unset && !Core.input.keyDown(modifier)) return false;
        return true;
    }

    public boolean previewTurretsDown(){ return gate() && Core.input.keyDown(previewTurrets); }
    public boolean previewTurretsTap(){ return gate() && Core.input.keyTap(previewTurrets); }
    public boolean previewUnitsDown(){ return gate() && Core.input.keyDown(previewUnits); }
    public boolean previewUnitsTap(){ return gate() && Core.input.keyTap(previewUnits); }
    public boolean cycleModeTap(){ return gate() && Core.input.keyTap(cycleMode); }
    public boolean cycleFilterTap(){ return gate() && Core.input.keyTap(cycleFilter); }
    public boolean autoMouseTap(){ return gate() && Core.input.keyTap(autoMouse); }
    public boolean autoChatTap(){ return gate() && Core.input.keyTap(autoChat); }
    public boolean autoMoveTap(){ return gate() && Core.input.keyTap(autoMove); }
    public boolean heatmapTap(){ return gate() && Core.input.keyTap(heatmap); }

    public boolean anyPreviewDown(){
        return previewTurretsDown() || previewUnitsDown();
    }
}
