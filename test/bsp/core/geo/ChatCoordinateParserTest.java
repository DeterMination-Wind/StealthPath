package bsp.core.geo;

import org.junit.Test;

import static org.junit.Assert.*;

public class ChatCoordinateParserTest{

    @Test
    public void parsesPlainCoordinates(){
        int[] c = ChatCoordinateParser.parse("attack (120, 88) now");
        assertArrayEquals(new int[]{120, 88}, c);
    }

    @Test
    public void parsesFullWidthCommaAndSpaces(){
        int[] c = ChatCoordinateParser.parse("目标（33，77）集合");
        assertArrayEquals(new int[]{33, 77}, c);
        int[] c2 = ChatCoordinateParser.parse("go ( 33 , 77 ) ");
        assertArrayEquals(new int[]{33, 77}, c2);
    }

    @Test
    public void negativeAndGarbageRejected(){
        assertNull(ChatCoordinateParser.parse("(-3, 5)"));
        assertNull(ChatCoordinateParser.parse("hello world"));
        assertNull(ChatCoordinateParser.parse(null));
        assertNull(ChatCoordinateParser.parse("99999, 3")); // too many digits, no parens
    }

    @Test
    public void clampsOutOfRangeIntoMap(){
        int[] c = ChatCoordinateParser.parseClamped("(5000, 9999)", 200, 200);
        assertArrayEquals(new int[]{199, 199}, c);
    }

    @Test
    public void firstCoordinateWins(){
        int[] c = ChatCoordinateParser.parse("(10,20) then (30,40)");
        assertArrayEquals(new int[]{10, 20}, c);
    }
}
