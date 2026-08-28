package bsp.update;

import arc.Core;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;
import bsp.settings.BspSettings;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified-safe update check: polls the GitHub Release API (throttled to
 * once per 6 hours), shows a dialog listing assets, and offers to open the
 * release page either directly or through a configurable mirror prefix.
 * It never downloads or replaces the mod jar itself and never restarts the
 * game — installing an update stays an explicit player action.
 */
public final class UpdateChecker{
    private static final long THROTTLE_SECONDS = 6 * 3600;
    private static long lastCheckEpoch;

    private UpdateChecker(){}

    public static void onMainMenu(){
        if(bsp.BspBuildFlags.bundled()) return; // aggregator owns update checks
        if(!BspSettings.updateCheck()) return;
        if(Vars.state == null || !Vars.state.isMenu()) return;
        long now = System.currentTimeMillis() / 1000L;
        if(now - lastCheckEpoch < THROTTLE_SECONDS) return;
        lastCheckEpoch = now;

        String repo = BspSettings.updateRepo();
        if(repo == null || repo.trim().isEmpty()){
            Log.info("[bsp] update repo not configured; skipping update check");
            return;
        }

        String version = ownVersion();
        if(version == null || version.equals("0.0.0")){
            Log.info("[bsp] dev build (0.0.0); skipping update check");
            return;
        }

        Http.get("https://api.github.com/repos/" + repo.trim() + "/releases/latest", res -> {
                try{
                    JsonValue root = new JsonReader().parse(res.getResultAsString());
                    String tag = root.getString("tag_name", "");
                    String url = root.getString("html_url", "");
                    if(tag.isEmpty() || tag.equals(BspSettings.updateIgnored())) return;
                    if(tag.equals(version) || tag.equals("v" + version)) return;

                    List<String[]> assets = new ArrayList<String[]>();
                    for(JsonValue a : root.get("assets")){
                        assets.add(new String[]{
                            a.getString("name", "?"),
                            a.getString("browser_download_url", ""),
                            String.valueOf(a.getLong("size", 0L))
                        });
                    }
                    Core.app.post(() -> showDialog(tag, version, url, assets));
                }catch(Throwable t){
                    Log.warn("[bsp] update parse failed: @", t.toString());
                }
        }, err -> Log.info("[bsp] update check failed: @", String.valueOf(err)));
    }

    private static String ownVersion(){
        if(Vars.mods == null) return null;
        for(mindustry.mod.Mods.LoadedMod mod : Vars.mods.list()){
            if(mod.meta != null && mod.meta.name != null
                && (mod.meta.name.equals("betterStealthPath") || mod.meta.name.equals("betterStealthPath-dev"))){
                return mod.meta.version;
            }
        }
        return null;
    }

    private static void showDialog(String tag, String current, String url, List<String[]> assets){
        if(Vars.ui == null) return;
        if(!BspSettings.updateDialog()){
            Toasts2.show("bsp.update.light", tag);
            return;
        }

        BaseDialog dialog = new BaseDialog("@bsp.update.title");
        dialog.cont.add(Core.bundle.format("bsp.update.body", current, tag)).left().row();
        if(!assets.isEmpty()){
            dialog.cont.add("@bsp.update.assets").left().padTop(10f).row();
            for(String[] a : assets){
                dialog.cont.add("- " + a[0] + " (" + humanSize(a[2]) + ")").left().padLeft(10f).row();
            }
        }

        dialog.cont.add("@bsp.update.mirror.label").left().padTop(10f).row();
        dialog.cont.field(BspSettings.updateMirror(), m -> Core.settings.put("bsp.update.mirror", m.trim()))
            .growX().padBottom(10f).row();

        dialog.buttons.button("@bsp.update.open", () -> {
            if(url != null && !url.isEmpty()) Core.app.openURI(url);
            dialog.hide();
        }).size(160f, 48f);
        dialog.buttons.button("@bsp.update.mirror", () -> {
            String mirror = BspSettings.updateMirror();
            if(mirror == null || mirror.isEmpty()){
                Toasts2.show("bsp.update.nomirror");
                return;
            }
            String target = url;
            if(target != null && target.startsWith("https://github.com/")){
                target = mirror + target;
            }
            if(target != null) Core.app.openURI(target);
        }).size(160f, 48f);
        dialog.buttons.button("@bsp.update.ignore", () -> {
            Core.settings.put("bsp.update.ignored", tag);
            dialog.hide();
        }).size(160f, 48f);
        dialog.buttons.button("@bsp.update.later", dialog::hide).size(120f, 48f);
        dialog.show();
    }

    private static String humanSize(String bytes){
        try{
            long b = Long.parseLong(bytes);
            if(b > 1024 * 1024) return String.format("%.1fMB", b / 1048576.0);
            if(b > 1024) return (b / 1024) + "KB";
            return b + "B";
        }catch(NumberFormatException e){
            return "?";
        }
    }

    private static final class Toasts2{
        static void show(String key, Object... args){
            bsp.ui.Toasts.show(key, args);
        }
    }
}
