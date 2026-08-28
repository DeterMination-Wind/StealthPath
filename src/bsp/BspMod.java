package bsp;

import arc.Events;
import arc.util.Log;
import bsp.render.RouteRenderer;
import bsp.settings.BspSettings;
import bsp.ui.HudController;
import bsp.ui.OverlayUiBridge;
import bsp.ui.SettingsPage;
import bsp.update.UpdateChecker;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.ClientChatEvent;
import mindustry.game.EventType.PlayerChatEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.mod.Mod;

/**
 * betterStealthPath entry point. Client-only: on headless servers or mobile
 * clients nothing registers beyond a log line. Everything user-visible is
 * opt-in through explicit key presses — no standing automation.
 */
public class BspMod extends Mod{

    private BspController controller;
    private RouteRenderer renderer;
    private HudController hud;
    private OverlayUiBridge overlayBridge;
    private boolean clientReady;

    public BspMod(){
        if(Vars.headless || Vars.mobile){
            Log.info("[bsp] headless/mobile environment, client features disabled");
            return;
        }
        if(BspBuildFlags.bundled()){
            Log.info("[bsp] running in bundled/aggregated form: no settings category, no own update check");
        }

        bsp.input.BspKeys.get(); // register keybinds early

        Events.on(ClientLoadEvent.class, e -> {
            controller = new BspController();
            renderer = new RouteRenderer();
            hud = new HudController(controller);
            overlayBridge = new OverlayUiBridge();
            hud.build();
            SettingsPage.register(controller);
            clientReady = true;
        });

        Events.on(WorldLoadEvent.class, e -> {
            if(controller != null) controller.resetWorld();
        });

        Events.on(PlayerChatEvent.class, e -> {
            if(controller != null && e.message != null) controller.onChatMessage(e.message);
        });
        Events.on(ClientChatEvent.class, e -> {
            if(controller != null && e.message != null) controller.onChatMessage(e.message);
        });

        Events.run(Trigger.update, () -> {
            if(!clientReady) return;
            // the update check is governed by its own setting, not by the
            // route master switch (spec 2.9)
            UpdateChecker.onMainMenu();
            if(!BspSettings.enabled()) return;
            try{
                controller.update();
                hud.update();
                OverlayUiBridge.tick(hud, overlayBridge);
            }catch(Throwable t){
                if(BspSettings.debugLog()){
                    Log.err("[bsp] update loop error", t);
                }
            }
        });

        Events.run(Trigger.draw, () -> {
            if(!clientReady) return;
            try{
                renderer.draw(controller);
            }catch(Throwable t){
                if(BspSettings.debugLog()){
                    Log.err("[bsp] render error", t);
                }
            }
        });

    }
}
