package bsp.core.geo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses "(x, y)" tile coordinates out of chat messages (own or teammate
 * pings) for the auto mode "cluster -> chat coordinate" target.
 */
public final class ChatCoordinateParser{
    // accepts ASCII and full-width parentheses/commas, optional spaces
    private static final Pattern P = Pattern.compile("[\\(\\uFF08]\\s*(\\d{1,4})\\s*[,\\uFF0C]\\s*(\\d{1,4})\\s*[\\)\\uFF09]");

    private ChatCoordinateParser(){}

    /** Returns {x, y} in tile coordinates, or null when the message has none. */
    public static int[] parse(String message){
        if(message == null) return null;
        Matcher m = P.matcher(message);
        if(!m.find()) return null;
        try{
            int x = Integer.parseInt(m.group(1));
            int y = Integer.parseInt(m.group(2));
            return new int[]{x, y};
        }catch(NumberFormatException e){
            return null;
        }
    }

    /** Parses and clamps into the map; returns null when no coordinate found. */
    public static int[] parseClamped(String message, int width, int height){
        int[] c = parse(message);
        if(c == null) return null;
        c[0] = GridUtils.clampToMap(c[0], width);
        c[1] = GridUtils.clampToMap(c[1], height);
        return c;
    }
}
