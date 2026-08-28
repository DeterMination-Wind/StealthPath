package bsp;

import arc.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Build-time form factor flag. {@code -Pbundled=true} injects
 * bsp/bundled.properties into the jar (see build.gradle). In bundled form the
 * mod skips its own settings category and update check — the aggregator owns
 * them. Standalone is the default.
 */
public final class BspBuildFlags{
    private static final boolean bundled = read();

    private static boolean read(){
        InputStream in = BspBuildFlags.class.getResourceAsStream("/bsp/bundled.properties");
        if(in == null) return false;
        try{
            Properties p = new Properties();
            p.load(in);
            return Boolean.parseBoolean(p.getProperty("bundled", "false"));
        }catch(IOException e){
            Log.warn("[bsp] failed to read bundled marker: @", e.toString());
            return false;
        }finally{
            try{ in.close(); }catch(IOException ignored){}
        }
    }

    public static boolean bundled(){
        return bundled;
    }
}
