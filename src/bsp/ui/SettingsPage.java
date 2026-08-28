package bsp.ui;

import arc.Core;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import bsp.BspController;
import bsp.settings.BspSettings;
import bsp.settings.FilterMode;
import bsp.settings.TargetMode;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.ui.dialogs.BaseDialog;

/**
 * The mod settings category: Settings -> Game(-> Mods) -> Stealth Path+.
 * Single page, grouped, everything applies immediately. The advanced section
 * is gated behind the "pro mode" toggle; no implementation-detail options
 * (like algorithm choice) are exposed — the caution slider replaces them.
 */
public final class SettingsPage{

    public static void register(BspController controller){
        if(bsp.BspBuildFlags.bundled()) return; // aggregator owns settings when bundled

        Vars.ui.settings.addCategory("@bsp.settings", t -> {
            group(t, "bsp.settings.general");
            t.checkPref("bsp.enabled", true);
            t.checkPref("bsp.toasts", true);
            t.checkPref("bsp.debugLog", true);
            t.checkPref("bsp.proMode", false);
            t.checkPref("bsp.shortestOnly", false);
            t.checkPref("bsp.slowestBaseline", true);
            t.checkPref("bsp.floorSlowdown", true);
            t.checkPref("bsp.survivableLiquid", true);
            t.sliderPref("bsp.caution", 50, 0, 100, 5, v -> Core.bundle.get(
                v <= 20 ? "bsp.caution.reckless" : v >= 80 ? "bsp.caution.cautious" : "bsp.caution.balanced"));

            group(t, "bsp.settings.draw");
            t.checkPref("bsp.win.mode", true);
            t.checkPref("bsp.win.damage", true);
            t.checkPref("bsp.win.control", true);
            t.sliderPref("bsp.keepSeconds", 10, 0, 60, 1, v -> v == 0 ? Core.bundle.get("bsp.keep.forever") : v + "s");
            t.sliderPref("bsp.lineWidth", 2, 1, 6, 1, v -> String.valueOf(v));
            t.sliderPref("bsp.lineAlpha", 85, 0, 100, 5, v -> v + "%");
            t.checkPref("bsp.showEnds", true);
            t.sliderPref("bsp.startDotScale", 220, 0, 400, 10, v -> v + "%");
            t.sliderPref("bsp.endDotScale", 260, 0, 400, 10, v -> v + "%");
            t.checkPref("bsp.showDamageText", true);
            t.sliderPref("bsp.damageTextScale", 60, 20, 140, 5, v -> v + "%");
            t.checkPref("bsp.damageAtEnd", false);
            t.sliderPref("bsp.damageOffset", 100, 0, 300, 10, v -> v + "%");
            t.sliderPref("bsp.previewInterval", 10, 2, 100, 1, v -> String.format("%.2fs", v / 100f));

            group(t, "bsp.settings.colors");
            t.textPref("bsp.color.power", BspSettings.colorPower());
            t.textPref("bsp.color.mouse", BspSettings.colorMouse());
            t.textPref("bsp.color.safe", BspSettings.colorSafe());
            t.textPref("bsp.color.warn", BspSettings.colorWarn());
            t.textPref("bsp.color.fatal", BspSettings.colorFatal());

            group(t, "bsp.settings.auto");
            t.sliderPref("bsp.auto.safeThreshold", 10, 0, 200, 5, v -> String.valueOf(v));
            t.checkPref("bsp.autoMove", true);
            t.sliderPref("bsp.resendInterval", 50, 2, 400, 5, v -> String.format("%.2fs", v / 100f));
            t.sliderPref("bsp.packetInterval", 33, 0, 170, 1, v -> String.format("%.3fs", v / 1000f));
            t.sliderPref("bsp.arriveRadius", 2, 0, 8, 1, v -> v + "");
            t.sliderPref("bsp.spreadTicks", 1, 1, 60, 1, v -> v + "t");
            t.sliderPref("bsp.threatExpand", 6, 0, 20, 1, v -> v + "");

            group(t, "bsp.settings.targets");
            modeRow(t, controller);
            filterRow(t, controller);
            t.sliderPref("bsp.power.maxRoutes", 3, 1, 10, 1, v -> String.valueOf(v));
            t.sliderPref("bsp.power.minSize", 2, 2, 10, 1, v -> String.valueOf(v));
            t.checkPref("bsp.power.fromPlayer", false);
            t.sliderPref("bsp.coreCount", 1, 1, 12, 1, v -> String.valueOf(v));
            t.sliderPref("bsp.cluster.dist", 5, 1, 30, 1, v -> v + "");

            mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable adv = new mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable();
            adv.marginTop(8f);
            group(adv, "bsp.settings.advanced");
            adv.sliderPref("bsp.deepReserve", 15, 0, 30, 5, v -> String.format("%.1fs", v / 10f));
            adv.sliderPref("bsp.candidateRadius", 24, 6, 64, 1, v -> String.valueOf(v));
            adv.sliderPref("bsp.heatScale", 100, 50, 400, 10, v -> v + "%");
            adv.checkPref("bsp.hoverDps", false);
            adv.sliderPref("bsp.waypointCap", 12, 2, 60, 1, v -> String.valueOf(v));
            adv.checkPref("bsp.batchEnabled", true);
            adv.sliderPref("bsp.batchSizeMult", 100, 50, 200, 10, v -> v + "%");
            adv.sliderPref("bsp.batchDelayMult", 100, 0, 200, 10, v -> v + "%");
            adv.sliderPref("bsp.idleSlow", 8, 1, 30, 1, v -> v + "x");
            adv.sliderPref("bsp.power.linkDist", 8, 1, 20, 1, v -> v + "");
            adv.sliderPref("bsp.power.nearTurret", 12, 1, 40, 1, v -> v + "");
            adv.sliderPref("bsp.formationInflate", 125, 100, 300, 5, v -> v + "%");
            adv.checkPref("bsp.targetFromMouse", true);
            adv.checkPref("bsp.showTargetMarker", true);
            targetBlockButton(adv);

            adv.visible(() -> Core.settings.getBool("bsp.proMode", false));
            adv.update(() -> adv.visible = Core.settings.getBool("bsp.proMode", false));
            t.add(adv).growX().left().row();

            group(t, "bsp.settings.update");
            t.checkPref("bsp.update.check", true);
            t.checkPref("bsp.update.dialog", true);
            t.textPref("bsp.update.repo", BspSettings.updateRepo());
        });
    }

    private static void group(Table t, String key){
        t.add(Core.bundle.get(key)).growX().left().color(mindustry.graphics.Pal.accent).padTop(12f).row();
    }

    /** Five target-mode quick buttons; the current mode is accented. */
    private static void modeRow(Table t, final BspController controller){
        final Table row = new Table();
        final Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> {
            row.clear();
            for(final TargetMode m : TargetMode.values()){
                TextButton b = new TextButton(Core.bundle.get("bsp.mode." + m.name() + ".short"),
                    mindustry.ui.Styles.cleart);
                if(controller.mode == m) b.getLabel().setColor(mindustry.graphics.Pal.accent);
                b.clicked(() -> {
                    controller.mode = m;
                    rebuild[0].run();
                });
                row.add(b).size(74f, 32f).padRight(4f);
            }
        };
        rebuild[0].run();
        t.add("@bsp.settings.mode").left().padRight(8f);
        t.add(row).growX().left().row();
    }

    /** Threat-filter quick buttons (AUTO / ground / air / mixed). */
    private static void filterRow(Table t, final BspController controller){
        final Table row = new Table();
        final Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> {
            row.clear();
            for(final FilterMode f : FilterMode.values()){
                TextButton b = new TextButton(Core.bundle.get("bsp.filter." + f.name() + ".short"),
                    mindustry.ui.Styles.cleart);
                if(controller.filter == f) b.getLabel().setColor(mindustry.graphics.Pal.accent);
                b.clicked(() -> {
                    controller.filter = f;
                    rebuild[0].run();
                });
                row.add(b).size(74f, 32f).padRight(4f);
            }
        };
        rebuild[0].run();
        t.add("@bsp.settings.filter").left().padRight(8f);
        t.add(row).growX().left().row();
    }

    private static void targetBlockButton(Table t){
        TextButton btn = new TextButton(currentBlockLabel(), mindustry.ui.Styles.cleart);
        btn.clicked(() -> {
            BaseDialog dialog = new BaseDialog("@bsp.settings.pickblock");
            dialog.cont.pane(p -> {
                for(mindustry.world.Block block : Vars.content.blocks()){
                    if(block.buildVisibility == mindustry.world.meta.BuildVisibility.hidden) continue;
                    p.button(block.localizedName, mindustry.ui.Styles.cleart, () -> {
                        Core.settings.put("bsp.targetBlock", block.localizedName);
                        btn.setText(currentBlockLabel());
                        dialog.hide();
                    }).growX().row();
                }
            }).grow().maxHeight(400f);
            dialog.addCloseButton();
            dialog.show();
        });
        t.add("@bsp.settings.targetblock").left().padRight(8f);
        t.add(btn).growX().row();
    }

    private static String currentBlockLabel(){
        String s = BspSettings.targetBlock();
        return s == null || s.isEmpty() ? Core.bundle.get("bsp.settings.noblock") : s;
    }
}
