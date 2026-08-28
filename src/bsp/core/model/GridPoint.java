package bsp.core.model;

/** Immutable tile-space point. */
public final class GridPoint{
    public final int x, y;

    public GridPoint(int x, int y){
        this.x = x;
        this.y = y;
    }

    public double dist(GridPoint o){
        return Math.hypot(x - o.x, y - o.y);
    }

    public double distSq(GridPoint o){
        int dx = x - o.x, dy = y - o.y;
        return (double)dx * dx + (double)dy * dy;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof GridPoint)) return false;
        GridPoint p = (GridPoint)o;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode(){
        return x * 31 + y;
    }

    @Override
    public String toString(){
        return "(" + x + "," + y + ")";
    }
}
