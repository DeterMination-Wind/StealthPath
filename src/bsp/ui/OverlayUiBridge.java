package bsp.ui;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflection bridge to MindustryX's OverlayUI (draggable/resizable managed
 * windows). Every access is guarded: when MindustryX is absent the HUD
 * fallback stays in place and this bridge is a silent no-op. The mod never
 * overrides the player's OverlayUI visibility/pin choices.
 */
public final class OverlayUiBridge{
    private static final String OVERLAY_UI_CLASS = "mindustryX.features.ui.OverlayUI";

    private Object instance;
    private Method registerWindow;
    private boolean resolved;
    private boolean unavailable;
    private float nextRetry;

    /** Attempts to bind OverlayUI; returns true when the windows got registered. */
    public boolean tryRegister(Table mode, Table damage, Table control){
        if(unavailable) return false;
        if(!resolved && !resolve()) return false;

        try{
            Object ui = instanceField.get(null);
            if(ui == null) return false;
            // attach OverlayUI first if the host has not done it yet
            if(!isAttached(ui)){
                initMethod.invoke(ui);
            }
            if(!isAttached(ui)){
                return false;
            }
            instance = ui;
            makeResizable(registerWindow.invoke(ui, "bsp-mode", mode));
            makeResizable(registerWindow.invoke(ui, "bsp-damage", damage));
            makeResizable(registerWindow.invoke(ui, "bsp-control", control));
            Log.info("[bsp] OverlayUI windows registered.");
            return true;
        }catch(Throwable t){
            Log.warn("[bsp] OverlayUI registration failed, HUD fallback in use: @", t.toString());
            return false;
        }
    }

    /**
     * Best-effort: X windows are created non-resizable by default; the mod's
     * windows opt into free resizing ("stretch to any size without springing
     * back"). Hosts without the setter keep their default behavior.
     */
    private void makeResizable(Object window){
        if(window == null) return;
        try{
            window.getClass().getMethod("setResizable", boolean.class).invoke(window, Boolean.TRUE);
        }catch(Throwable ignored){
            // host Window variant without a resizable setter — keep defaults
        }
    }

    private Field instanceField;
    private Field groupField;
    private Method initMethod, isAttachedMethod;

    /**
     * Whether OverlayUI's window group is attached to the scene. Detection is
     * version-tolerant: hosts that expose isAttached() use it, others are read
     * through their private group field, and when neither exists init() is
     * trusted (it re-adds the same group, which is idempotent in arc).
     */
    private boolean isAttached(Object ui){
        try{
            if(isAttachedMethod != null){
                return Boolean.TRUE.equals(isAttachedMethod.invoke(ui));
            }
            if(groupField != null){
                Object group = groupField.get(ui);
                return group instanceof Element && ((Element)group).hasParent();
            }
        }catch(Throwable t){
            Log.warn("[bsp] OverlayUI attach probe failed: @", t.toString());
        }
        return true;
    }

    private boolean resolve(){
        if(Time.time < nextRetry) return false;
        nextRetry = Time.time + 300f;
        try{
            Class<?> cls = Class.forName(OVERLAY_UI_CLASS, false, OverlayUiBridge.class.getClassLoader());
            instanceField = cls.getField("INSTANCE");
            initMethod = cls.getMethod("init");
            registerWindow = cls.getMethod("registerWindow", String.class, Table.class);
            try{
                isAttachedMethod = cls.getMethod("isAttached");
            }catch(NoSuchMethodException noProbe){
                isAttachedMethod = null;
            }
            try{
                groupField = cls.getDeclaredField("group");
                groupField.setAccessible(true);
            }catch(Throwable noField){
                groupField = null;
            }
            resolved = true;
            return true;
        }catch(ClassNotFoundException notInstalled){
            unavailable = true;
            return false;
        }catch(Throwable t){
            Log.warn("[bsp] OverlayUI resolve failed: @", t.toString());
            return false;
        }
    }

    public boolean registered(){
        return instance != null;
    }

    /** Runs from Trigger.update: lazily upgrades the HUD to OverlayUI windows. */
    public static void tick(HudController hud, OverlayUiBridge bridge){
        if(bridge.registered() || Vars.headless || Vars.ui == null) return;
        if(!hud.attachOverlay(bridge)){
            hud.attachToHud();
        }
    }
}
