package bsp.render;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Lines;
import arc.math.geom.Vec2;
import arc.util.Time;
import bsp.BspController;
import bsp.core.geo.GridUtils;
import bsp.core.model.GridPoint;
import bsp.core.threat.ThreatGrid;
import bsp.settings.BspSettings;
import bsp.state.ShownRoute;
import bsp.ui.UiState;
import mindustry.Vars;
import mindustry.graphics.Layer;

import java.util.ArrayList;
import java.util.List;

/**
 * World-space rendering: route polylines (turning points only), endpoints,
 * damage labels, the optional heatmap overlay and the draggable target marker.
 */
public final class RouteRenderer{

    public void draw(BspController controller){
        if(!BspSettings.enabled() || Vars.state == null || Vars.state.isMenu()) return;
        if(Vars.mobile) return;

        Draw.draw(Layer.max, () -> render(controller));
    }

    private void render(BspController controller){
        float alpha = BspSettings.lineAlpha();

        if(controller.heatmap){
            drawHeatmap(controller);
        }

        for(ShownRoute route : controller.routes){
            Color color = routeColor(route);
            Color c = color.cpy().a(alpha);

            Draw.color(c);
            Lines.stroke(BspSettings.lineWidth());

            List<GridPoint> pts = route.waypoints;
            if(pts.size() == 1){
                Fill.circle(wx(pts.get(0).x), wy(pts.get(0).y), BspSettings.lineWidth());
            }else{
                for(int i = 1; i < pts.size(); i++){
                    Lines.line(wx(pts.get(i - 1).x), wy(pts.get(i - 1).y), wx(pts.get(i).x), wy(pts.get(i).y));
                }
            }

            if(BspSettings.showEnds()){
                float startR = BspSettings.lineWidth() * BspSettings.startDotScale();
                float endR = BspSettings.lineWidth() * BspSettings.endDotScale();
                Fill.circle(wx(route.start.x), wy(route.start.y), startR);
                GridPoint last = pts.get(pts.size() - 1);
                Fill.circle(wx(last.x), wy(last.y), endR);
            }

            if(BspSettings.showDamageText()){
                drawDamageLabel(route, pts, alpha);
            }
        }

        drawTargetMarker();
        Draw.reset();
    }

    private void drawDamageLabel(ShownRoute route, List<GridPoint> pts, float alpha){
        String label = "~-~" + Math.round(route.harm);
        if(route.liquidRound >= 2){
            label += " !";
        }
        float x, y;
        if(BspSettings.damageAtEnd() || pts.size() < 2){
            GridPoint p = pts.get(pts.size() - 1);
            x = wx(p.x);
            y = wy(p.y) + 6f * BspSettings.damageOffset();
        }else{
            GridPoint a = pts.get(pts.size() / 2 - 1), b = pts.get(pts.size() / 2);
            x = (wx(a.x) + wx(b.x)) / 2f;
            y = (wy(a.y) + wy(b.y)) / 2f + 6f * BspSettings.damageOffset();
        }

        Font font = mindustry.ui.Fonts.outline;
        float oldScale = font.getScaleX();
        font.getData().setScale(oldScale * BspSettings.damageTextScale());
        font.setColor(routeColor(route).a(Math.min(1f, alpha + 0.1f)));
        font.draw(label, x, y);
        font.getData().setScale(oldScale);
    }

    private Color routeColor(ShownRoute route){
        switch(route.kind){
            case POWER: return Color.valueOf(BspSettings.colorPower());
            case MOUSE: return Color.valueOf(BspSettings.colorMouse());
            case AUTO:
                switch(route.band){
                    case SAFE: return Color.valueOf(BspSettings.colorSafe());
                    case WARNING: return Color.valueOf(BspSettings.colorWarn());
                    default: return Color.valueOf(BspSettings.colorFatal());
                }
            default:
                // manual routes are also risk-graded: readable at a glance
                switch(route.band){
                    case SAFE: return Color.valueOf(BspSettings.colorSafe());
                    case WARNING: return Color.valueOf(BspSettings.colorWarn());
                    default: return Color.valueOf(BspSettings.colorFatal());
                }
        }
    }

    private void drawHeatmap(BspController controller){
        ThreatGrid grid = controller.threatGrid();
        if(grid == null) return;
        float max = grid.max();
        if(max <= 0f) return;

        Vec2 tl = Core.input == null ? new Vec2() : Core.input.mouseWorld(0, Core.graphics.getHeight());
        int x0 = Math.max(0, GridUtils.worldToTile(tl.x) - 2);
        int y1 = Math.min(grid.height - 1, GridUtils.worldToTile(tl.y) + 2);
        int visible = (int)(Core.graphics.getWidth() / (Core.camera.width / grid.width)) + 8;

        float scale = BspSettings.heatScale();
        arc.graphics.g2d.Font font = mindustry.ui.Fonts.outline;
        float oldScale = font.getScaleX();
        font.getData().setScale(0.5f * Math.max(0.5f, scale));
        font.setColor(new Color(1f, 1f, 1f, 0.8f));
        for(int y = Math.max(0, y1 - visible - 8); y <= y1; y++){
            for(int x = x0; x < Math.min(grid.width, x0 + visible); x++){
                float v = grid.get(x, y);
                if(v <= 0f) continue;
                float t = Math.min(1f, v / max);
                Draw.color(new Color(1f - t * 0.8f, 1f - t, 0.15f, 0.12f + 0.45f * t * scale));
                Fill.rect(x * 8f + 4f, y * 8f + 4f, 8f, 8f);
                // numeric overlay on the hottest cells only (debug view)
                if(t > 0.3f){
                    font.draw(String.valueOf(Math.round(v)), x * 8f + 4f, y * 8f + 7f, 1);
                }
            }
        }
        font.getData().setScale(oldScale);
        Draw.reset();
    }

    /** Circle + crosshair marker for the draggable target point. */
    private void drawTargetMarker(){
        if(BspSettings.targetFromMouse()) return;
        if(!BspSettings.showTargetMarker()) return;
        GridPoint p = UiState.targetPoint();
        if(p == null) return;
        float x = wx(p.x), y = wy(p.y);
        Draw.color(Color.white, 0.9f);
        Lines.stroke(1.5f);
        Lines.circle(x, y, 10f);
        Lines.line(x - 14f, y, x - 5f, y);
        Lines.line(x + 5f, y, x + 14f, y);
        Lines.line(x, y - 14f, x, y - 5f);
        Lines.line(x, y + 5f, x, y + 14f);
    }

    private static float wx(int tile){
        return GridUtils.tileToWorldCenter(tile);
    }

    private static float wy(int tile){
        return GridUtils.tileToWorldCenter(tile);
    }

    /** Top threat contributors under the mouse, for the hover DPS window. */
    public static List<String> hoverDetails(BspController controller, boolean includeUnits){
        List<String> out = new ArrayList<String>();
        if(!BspSettings.hoverDps()) return out;
        if(Vars.control == null || Vars.control.input == null || Core.input == null || Vars.player == null) return out;
        arc.math.geom.Vec2 w = Core.input.mouseWorld(Core.input.mouseX(), Core.input.mouseY());
        int tx = GridUtils.worldToTile(w.x), ty = GridUtils.worldToTile(w.y);
        if(Vars.world == null || tx < 0 || ty < 0 || tx >= Vars.world.width() || ty >= Vars.world.height()) return out;

        // throttle: recompute at most every 0.25s
        if(Time.time - lastHover < 0.25f) return lastHoverOut;
        lastHover = Time.time;
        lastHoverOut = out;

        GridPoint tile = new GridPoint(tx, ty);
        bsp.core.model.FormationProfile f = bsp.world.WorldScanner.formation(
            bsp.BspController.selectedUnits(), BspSettings.slowestBaseline());
        List<WorldSource> cands = new ArrayList<WorldSource>();
        for(bsp.world.WorldScanner.ShapedSource s : controller.scanner().threatSources(includeUnits)){
            float v = ThreatGrid.sourceThreatAt(s.source, f, tile);
            if(v > 0.01f){
                cands.add(new WorldSource(s.source.label, v));
            }
        }
        cands.sort((a, b) -> Float.compare(b.value, a.value));
        float total = 0f;
        for(WorldSource c : cands) total += c.value;
        out.add(Core.bundle.format("bsp.hover.total", total));
        int n = Math.min(3, cands.size());
        for(int i = 0; i < n; i++){
            out.add(Core.bundle.format("bsp.hover.line", cands.get(i).label, cands.get(i).value));
        }
        return out;
    }

    private static float lastHover = -999f;
    private static List<String> lastHoverOut = new ArrayList<String>();

    private static final class WorldSource{
        final String label;
        final float value;

        WorldSource(String label, float value){
            this.label = label;
            this.value = value;
        }
    }
}
