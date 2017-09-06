// License: GPL. For details, see Readme.txt file.
package playground.clib.jmapviewer.interfaces;

import java.awt.Graphics;
import java.awt.Point;

import playground.clib.jmapviewer.Coordinate;
import playground.clib.jmapviewer.JMapViewer;

/**
 * Interface to be implemented by all one dimensional elements that can be displayed on the map.
 *
 * @author Jan Peter Stotz
 * @see JMapViewer#addMapMarker(MapMarker)
 * @see JMapViewer#getMapMarkerList()
 */
public interface MapMarker extends MapObject, ICoordinate {

    enum STYLE {
        FIXED,
        VARIABLE
    }

    /**
     * @return Latitude and Longitude of the map marker position
     */
    Coordinate getCoordinate();

    /**
     * @return Latitude of the map marker position
     */
    @Override
    double getLat();

    /**
     * @return Longitude of the map marker position
     */
    @Override
    double getLon();

    /**
     * @return Radius of the map marker position
     */
    double getRadius();

    /**
     * @return Style of the map marker
     */
    STYLE getMarkerStyle();

    /**
     * Paints the map marker on the map. The <code>position</code> specifies the
     * coordinates within <code>g</code>
     *
     * @param g graphics
     * @param position coordinates
     * @param radius radius
     */
    void paint(Graphics g, Point position, int radius);
}
