// License: GPL. For details, see Readme.txt file.
package playground.clib.jmapviewer.interfaces;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.util.List;

/**
 * Interface to be implemented by polygons that can be displayed on the map.
 *
 * @author Vincent Privat
 */
public interface MapPolygon extends MapObject {

    /**
     * @return Latitude/Longitude of each point of polygon
     */
    List<? extends ICoordinate> getPoints();

    /**
     * Paints the map polygon on the map. The <code>points</code>
     * are specifying the coordinates within <code>g</code>
     *
     * @param g graphics
     * @param points list of points defining the polygon to draw
     */
    void paint(Graphics g, List<Point> points);

    /**
     * Paints the map polygon on the map. The <code>polygon</code>
     * is specifying the coordinates within <code>g</code>
     *
     * @param g graphics
     * @param polygon polygon to draw
     */
    void paint(Graphics g, Polygon polygon);
}
