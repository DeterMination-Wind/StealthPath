package bsp.ui;

import arc.Core;
import mindustry.Vars;

/** Toast helper: i18n through Core.bundle, suppressed by the toast setting. */
public final class Toasts{
    private Toasts(){}

    public static void show(String key, Object... args){
        if(!bsp.settings.BspSettings.toasts()) return;
        if(Vars.ui == null) return;
        String text = Core.bundle.format(key, args);
        Vars.ui.showInfoToast(text, 2.4f);
    }

    public static void raw(String text){
        if(!bsp.settings.BspSettings.toasts()) return;
        if(Vars.ui == null) return;
        Vars.ui.showInfoToast(text, 2.4f);
    }
}
