package net.cmr.alchemycompany.world;

public class TilePoint {

    private int x, y;

    public TilePoint() {
        this.x = 0;
        this.y = 0;
    }
    public TilePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TilePoint other = (TilePoint) obj;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(x);
        result = 31 * result + Integer.hashCode(y);
        return result;
    }

    @Override
    public String toString() {
        return "TilePoint{" + "x=" + x + ", y=" + y + '}';
    }

}
