package bsp.ui;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.scene.Element;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import bsp.BspController;
import bsp.core.geo.GridUtils;
import bsp.core.model.GridPoint;
import bsp.core.model.RouteResult;
import bsp.render.RouteRenderer;
import bsp.settings.BspSettings;
import bsp.state.ShownRoute;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.ui.Styles;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * HUD fallback (no MindustryX): mode / damage / control blocks stacked in the
 * top-left corner, feature-equivalent to the OverlayUI windows. The three
 * window toggles govern both this fallback and the OverlayUI variant.
 */
public final class HudController{

    private Table modeTable, damageTable, controlTable;
    private Label modeLabel, filterLabel, damageLabel, hoverLabel;
    private TargetHandle targetHandle;
    private final BspController controller;

    public HudController(BspController controller){
        this.controller = controller;
    }

    public void build(){
        modeTable = window(BspSettings::winMode, t -> {
            modeLabel = new Label("");
            filterLabel = new Label("");
            t.add(modeLabel).row();
            t.add(filterLabel);
        });

        damageTable = window(BspSettings::winDamage, t -> {
            damageLabel = new Label("");
            hoverLabel = new Label("");
            t.add(damageLabel).row();
            t.add(hoverLabel);
        });

        controlTable = window(BspSettings::winControl, t -> {
            t.button("@bsp.ui.preview", Styles.cleart, () -> controller.plan(false)).size(92f, 32f);
            t.button("@bsp.ui.previewUnits", Styles.cleart, () -> controller.plan(true)).size(92f, 32f).row();
            t.button("@bsp.ui.autoMouse", Styles.cleart, () -> toggleAutoMouse()).size(92f, 32f);
            t.button("@bsp.ui.autoChat", Styles.cleart, () -> toggleAutoChat()).size(92f, 32f).row();
            t.button("@bsp.ui.cycleMode", Styles.cleart, () -> cycleMode()).size(92f, 32f);
            t.button("@bsp.ui.cycleFilter", Styles.cleart, () -> cycleFilter()).size(92f, 32f).row();
            // honest self-declaration of what this mod does (spec 9.10)
            Label disclosure = new Label(Core.bundle.get("bsp.ui.disclosure"));
            disclosure.setColor(Color.gray);
            t.add(disclosure).colspan(2).growX().wrap();
        });

        targetHandle = new TargetHandle(controller);
        Vars.ui.hudGroup.addChild(targetHandle);

        relayout();
        update();
    }

    private void toggleAutoMouse(){
        controller.autoMouse = !controller.autoMouse;
        Toasts.show(controller.autoMouse ? "bsp.toast.auto.on" : "bsp.toast.auto.off");
    }

    private void toggleAutoChat(){
        controller.autoChat = !controller.autoChat;
        Toasts.show(controller.autoChat ? "bsp.toast.auto.on" : "bsp.toast.auto.off");
    }

    private void cycleMode(){
        controller.mode = controller.mode.next();
        Toasts.show("bsp.toast.mode." + controller.mode.name());
    }

    private void cycleFilter(){
        controller.filter = controller.filter.next();
        Toasts.show("bsp.toast.filter." + controller.filter.name());
    }

    private Table root;

    /** (Re)attaches the three blocks to the hudGroup top-left stack. */
    public void relayout(){
        if(modeTable == null) return;
        if(root != null && root.hasParent()) root.remove();
        root = new Table();
        root.top().left();
        root.setPosition(8f, 68f, Align.topLeft);
        root.add(modeTable).row();
        root.add(damageTable).left().row();
        root.add(controlTable).left();
        Vars.ui.hudGroup.addChild(root);
    }

    public void detachFromHud(){
        if(root != null && root.hasParent()) root.remove();
    }

    public void attachToHud(){
        if(root != null && !root.hasParent()) Vars.ui.hudGroup.addChild(root);
    }

    /** Tries to promote the three blocks into OverlayUI windows. */
    public boolean attachOverlay(OverlayUiBridge bridge){
        detachFromHud();
        return bridge.tryRegister(modeTable, damageTable, controlTable);
    }

    private Table window(BooleanSupplier visible, java.util.function.Consumer<Table> fill){
        Table t = new Table(Tex.buttonEdge3);
        t.margin(8f);
        fill.accept(t);
        t.visible = visible.getAsBoolean();
        t.update(() -> t.visible = visible.getAsBoolean());
        return t;
    }

    /** Per-frame refresh of labels (cheap string work only when visible). */
    public void update(){
        if(modeTable == null) return;
        if(modeTable.visible){
            modeLabel.setText(Core.bundle.get("bsp.mode." + controller.mode.name()));
            filterLabel.setText(Core.bundle.get("bsp.filter." + controller.filter.name()));
        }
        if(damageTable.visible){
            double harm = Double.MAX_VALUE;
            ShownRoute best = null;
            for(ShownRoute r : controller.routes){
                if(r.harm < harm){
                    harm = r.harm;
                    best = r;
                }
            }
            if(best == null){
                damageLabel.setText(Core.bundle.get("bsp.ui.noroute"));
                damageLabel.setColor(Color.white);
            }else{
                damageLabel.setText(Core.bundle.format("bsp.ui.damage", Math.round(best.harm)));
                String color = best.band == RouteResult.RiskBand.SAFE ? BspSettings.colorSafe()
                    : best.band == RouteResult.RiskBand.WARNING ? BspSettings.colorWarn()
                    : BspSettings.colorFatal();
                damageLabel.setColor(Color.valueOf(color));
            }
            if(BspSettings.hoverDps()){
                List<String> lines = RouteRenderer.hoverDetails(controller, true);
                StringBuilder sb = new StringBuilder();
                for(String l : lines) sb.append(l).append('\n');
                hoverLabel.setText(sb.toString().trim());
                hoverLabel.visible = true;
            }else{
                hoverLabel.visible = false;
            }
        }
    }

    /** Drag handle that positions the world target point (target-point mode). */
    static final class TargetHandle extends Element{
        private final BspController controller;
        private float sx = -1f, sy = -1f;
        private boolean dragging;

        TargetHandle(BspController controller){
            this.controller = controller;
        }

        @Override
        public void act(float delta){
            super.act(delta);
            visible = !BspSettings.targetFromMouse() && BspSettings.showTargetMarker()
                && BspSettings.enabled() && Vars.state != null && !Vars.state.isMenu() && !Vars.mobile;
            if(!visible) return;

            if(sx < 0f){
                sx = Core.graphics.getWidth() * 0.6f;
                sy = Core.graphics.getHeight() * 0.5f;
                push();
            }

            float mx = Core.input.mouseX(), my = Core.input.mouseY();
            if(!dragging && Core.input.keyTap(arc.input.KeyCode.mouseLeft)
                && Math.abs(mx - sx) < 26f && Math.abs(my - sy) < 26f){
                dragging = true;
            }
            if(dragging){
                if(!Core.input.keyDown(arc.input.KeyCode.mouseLeft)){
                    dragging = false;
                }else{
                    sx = mx;
                    sy = my;
                    push();
                }
            }
        }

        private void push(){
            if(Vars.control == null || Vars.control.input == null) return;
            arc.math.geom.Vec2 w = Core.input.mouseWorld(sx, sy);
            UiState.targetPoint(new GridPoint(
                GridUtils.worldToTile(w.x), GridUtils.worldToTile(w.y)));
        }

        @Override
        public void draw(){
            Draw.color(Color.white, 0.85f);
            Lines.stroke(2f);
            Lines.circle(sx, sy, 10f);
            Lines.line(sx - 14f, sy, sx + 14f, sy);
            Lines.line(sx, sy - 14f, sx, sy + 14f);
            Draw.reset();
        }
    }
}
