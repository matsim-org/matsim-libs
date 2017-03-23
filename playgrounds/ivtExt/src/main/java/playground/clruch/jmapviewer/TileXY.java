// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer;

/**
 * @author w
 *
 */
public class TileXY {
    /**
     * x index of the tile (horizontal)
     */
    private final double x;

    /**
     * y number of the tile (vertical)
     */
    private final double y;

    /**
     * Returns an instance of coordinates.
     *
     * @param x number of the tile
     * @param y number of the tile
     */
    public TileXY(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * @return x index of the tile as integer
     */
    public int getXIndex() {
        return x < 0 ? (int) Math.ceil(x) : (int) Math.floor(x);
    }

    /**
     * @return y index of the tile as integer
     */
    public int getYIndex() {
        return y < 0 ? (int) Math.ceil(y) : (int) Math.floor(y);
    }

    /**
     * @return x index as double, might be non integral, when the point is not topleft corner of the tile
     */
    public double getX() {
        return x;
    }

    /**
     * @return y index as double, might be non integral, when the point is not topleft corner of the tile
     */
    public double getY() {
        return y;
    }
}
