package bsp.core.path;

import bsp.core.model.GridPoint;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class AStarTest{

    private static AStar plain(int w, int h){
        return new AStar(w, h, (x, y) -> true, (x, y) -> 1f);
    }

    @Test
    public void straightLineCollapsesToTwoEnds(){
        List<GridPoint> p = plain(20, 20).findPath(new GridPoint(2, 2), new GridPoint(10, 2));
        assertNotNull(p);
        assertEquals(new GridPoint(2, 2), p.get(0));
        assertEquals(new GridPoint(10, 2), p.get(p.size() - 1));
        assertEquals(9, p.size());
    }

    @Test
    public void detoursAroundWall(){
        boolean[][] wall = new boolean[20][20];
        for(int y = 0; y < 15; y++) wall[10][y] = true;
        AStar a = new AStar(20, 20, (x, y) -> !wall[x][y], (x, y) -> 1f);
        List<GridPoint> p = a.findPath(new GridPoint(5, 7), new GridPoint(15, 7));
        assertNotNull(p);
        for(GridPoint g : p) assertFalse(wall[g.x][g.y]);
        assertTrue(p.size() > 10);
    }

    @Test
    public void noCornerCutting(){
        // two diagonal blockers around (5,5): moving (4,4)->(5,5) diagonally must not pass
        boolean[][] wall = new boolean[10][10];
        wall[5][4] = true;
        wall[4][5] = true;
        AStar a = new AStar(10, 10, (x, y) -> !wall[x][y], (x, y) -> 1f);
        List<GridPoint> p = a.findPath(new GridPoint(4, 4), new GridPoint(5, 5));
        assertNotNull(p);
        // path must not contain the (4,4)->(5,5) diagonal step
        for(int i = 1; i < p.size(); i++){
            int dx = Math.abs(p.get(i).x - p.get(i - 1).x);
            int dy = Math.abs(p.get(i).y - p.get(i - 1).y);
            assertFalse(dx == 1 && dy == 1);
        }
    }

    @Test
    public void unreachableReturnsNull(){
        boolean[][] wall = new boolean[20][20];
        for(int y = 0; y < 20; y++) wall[10][y] = true;
        AStar a = new AStar(20, 20, (x, y) -> !wall[x][y], (x, y) -> 1f);
        assertNull(a.findPath(new GridPoint(5, 5), new GridPoint(15, 5)));
    }

    @Test
    public void blockedEndpointReturnsNull(){
        AStar a = new AStar(10, 10, (x, y) -> !(x == 8 && y == 8), (x, y) -> 1f);
        assertNull(a.findPath(new GridPoint(1, 1), new GridPoint(8, 8)));
    }

    @Test
    public void prefersCheapCellsOverHotOnes(){
        // hot band blocks the middle rows; the cheap lane is y <= 1
        AStar a = new AStar(21, 5, (x, y) -> true, (x, y) -> (x == 10 && y >= 2) ? 60f : 1f);
        List<GridPoint> p = a.findPath(new GridPoint(5, 2), new GridPoint(15, 2));
        assertNotNull(p);
        boolean touchesHot = false;
        for(GridPoint g : p) if(g.x == 10 && g.y >= 2) touchesHot = true;
        assertFalse("cheap detour expected", touchesHot);
    }
}
