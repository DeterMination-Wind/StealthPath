package bsp.settings;

import arc.Core;

/**
 * Central settings accessor. Every user-facing behavior reads through here so
 * changes apply immediately on the next plan/draw (nothing needs a restart).
 */
public final class BspSettings{
    private BspSettings(){}

    public static boolean enabled(){ return Core.settings.getBool("bsp.enabled", true); }
    public static void enabled(boolean v){ Core.settings.put("bsp.enabled", v); }
    public static boolean toasts(){ return Core.settings.getBool("bsp.toasts", true); }
    public static boolean debugLog(){ return Core.settings.getBool("bsp.debugLog", true); }
    public static boolean proMode(){ return Core.settings.getBool("bsp.proMode", false); }
    public static boolean shortestOnly(){ return Core.settings.getBool("bsp.shortestOnly", false); }
    public static boolean slowestBaseline(){ return Core.settings.getBool("bsp.slowestBaseline", true); }
    public static boolean floorSlowdown(){ return Core.settings.getBool("bsp.floorSlowdown", true); }
    public static boolean survivableLiquid(){ return Core.settings.getBool("bsp.survivableLiquid", true); }

    /** Player-facing caution slider: 0 reckless .. 100 cautious (default neutral). */
    public static int caution(){ return Core.settings.getInt("bsp.caution", 50); }

    public static boolean winMode(){ return Core.settings.getBool("bsp.win.mode", true); }
    public static boolean winDamage(){ return Core.settings.getBool("bsp.win.damage", true); }
    public static boolean winControl(){ return Core.settings.getBool("bsp.win.control", true); }

    public static float keepSeconds(){ return Core.settings.getFloat("bsp.keepSeconds", 10f); }
    public static float lineWidth(){ return Core.settings.getFloat("bsp.lineWidth", 2f); }
    public static float lineAlpha(){ return Core.settings.getFloat("bsp.lineAlpha", 0.85f); }
    public static boolean showEnds(){ return Core.settings.getBool("bsp.showEnds", true); }
    public static float startDotScale(){ return Core.settings.getFloat("bsp.startDotScale", 2.2f); }
    public static float endDotScale(){ return Core.settings.getFloat("bsp.endDotScale", 2.6f); }
    public static boolean showDamageText(){ return Core.settings.getBool("bsp.showDamageText", true); }
    public static float damageTextScale(){ return Core.settings.getFloat("bsp.damageTextScale", 0.6f); }
    public static boolean damageAtEnd(){ return Core.settings.getBool("bsp.damageAtEnd", false); }
    public static float damageOffset(){ return Core.settings.getFloat("bsp.damageOffset", 1.0f); }
    public static float previewInterval(){ return Core.settings.getFloat("bsp.previewInterval", 0.10f); }

    public static String colorPower(){ return Core.settings.getString("bsp.color.power", "3c7bff"); }
    public static String colorMouse(){ return Core.settings.getString("bsp.color.mouse", "a27ce5"); }
    public static String colorSafe(){ return Core.settings.getString("bsp.color.safe", "34c759"); }
    public static String colorWarn(){ return Core.settings.getString("bsp.color.warn", "ffd60a"); }
    public static String colorFatal(){ return Core.settings.getString("bsp.color.fatal", "ff3b30"); }

    public static float safeThreshold(){ return Core.settings.getFloat("bsp.auto.safeThreshold", 10f); }
    public static boolean autoMove(){ return Core.settings.getBool("bsp.autoMove", true); }
    public static float resendInterval(){ return Core.settings.getFloat("bsp.resendInterval", 0.5f); }
    public static float packetInterval(){ return Core.settings.getFloat("bsp.packetInterval", 0.033f); }
    public static float arriveRadius(){ return Core.settings.getFloat("bsp.arriveRadius", 2f); }
    public static int spreadTicks(){ return Core.settings.getInt("bsp.spreadTicks", 1); }
    public static float threatExpand(){ return Core.settings.getFloat("bsp.threatExpand", 6f); }

    public static float clusterDist(){ return Core.settings.getFloat("bsp.cluster.dist", 5f); }
    public static int powerMaxRoutes(){ return Core.settings.getInt("bsp.power.maxRoutes", 3); }
    public static int powerMinSize(){ return Core.settings.getInt("bsp.power.minSize", 2); }
    public static boolean powerFromPlayer(){ return Core.settings.getBool("bsp.power.fromPlayer", false); }
    public static float powerLinkDist(){ return Core.settings.getFloat("bsp.power.linkDist", 8f); }
    public static float powerNearTurret(){ return Core.settings.getFloat("bsp.power.nearTurret", 12f); }
    public static int coreCount(){ return Core.settings.getInt("bsp.coreCount", 1); }

    public static float formationInflate(){ return Core.settings.getFloat("bsp.formationInflate", 1.25f); }
    public static float deepReserve(){ return Core.settings.getFloat("bsp.deepReserve", 1.5f); }
    public static int candidateRadius(){ return Core.settings.getInt("bsp.candidateRadius", 24); }
    public static float heatScale(){ return Core.settings.getFloat("bsp.heatScale", 1.0f); }
    public static boolean hoverDps(){ return Core.settings.getBool("bsp.hoverDps", false); }
    public static int waypointCap(){ return Core.settings.getInt("bsp.waypointCap", 12); }
    public static boolean batchEnabled(){ return Core.settings.getBool("bsp.batchEnabled", true); }
    public static float batchSizeMult(){ return Core.settings.getFloat("bsp.batchSizeMult", 1.0f); }
    public static float batchDelayMult(){ return Core.settings.getFloat("bsp.batchDelayMult", 1.0f); }
    public static int idleSlow(){ return Core.settings.getInt("bsp.idleSlow", 8); }

    public static String targetBlock(){ return Core.settings.getString("bsp.targetBlock", ""); }
    public static boolean targetFromMouse(){ return Core.settings.getBool("bsp.targetFromMouse", true); }
    public static boolean showTargetMarker(){ return Core.settings.getBool("bsp.showTargetMarker", true); }

    public static boolean updateCheck(){ return Core.settings.getBool("bsp.update.check", true); }
    public static boolean updateDialog(){ return Core.settings.getBool("bsp.update.dialog", true); }
    public static String updateRepo(){ return Core.settings.getString("bsp.update.repo", ""); }
    public static String updateIgnored(){ return Core.settings.getString("bsp.update.ignored", ""); }
    public static String updateMirror(){ return Core.settings.getString("bsp.update.mirror", ""); }
}
