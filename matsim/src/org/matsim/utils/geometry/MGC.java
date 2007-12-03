/**
 * 
 */
package org.matsim.utils.geometry;

import org.matsim.utils.geometry.shared.Coord;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * Converter factory for various conversion from Geotools to Matsim and
 * vice versa 
 * 
 * @author laemmel
 *
 */
public class MGC {
	
	
	public static final GeometryFactory geoFac = new GeometryFactory();
	
	/**
	 * Converts a Matsim <code>CoordI</code> into a Geotools <code>Coordinate</code>
	 * @param Matsim coordinate
	 * @return Geotools coordinate
	 */
	public static final Coordinate coord2Coordinate(CoordI coord){
		return new Coordinate(coord.getX(),coord.getY());
	}

	/**
	 * Converts a Geotools <code>Coordinate</code> into a Matsim <code>CoordI</code> 
	 * @param Matsim coordinate
	 * @return Geotools coordinate
	 */
	public static final CoordI Coordinate2Coord(Coordinate coord){
		return new Coord(coord.x,coord.y);
	}

	/**
	 * Converts a Matsim <code>CoordI</code> into a Geotools <code>Point</code> 
	 * @param Matsim coordinate
	 * @return Geotools point
	 */
	public static final Point coord2Point(CoordI coord){
		return new Point(new CoordinateArraySequence(new Coordinate [] {coord2Coordinate(coord)}), geoFac);
	}
	
	/**
	 * Converts a Geotools <code>Point</code> into a Matsim <code>CoordI</code> 
	 * @param Geotools point
	 * @return Matsim coordinate
	 */
	public static final CoordI point2Coord(Point point){
		return new Coord(point.getX(),point.getY());
	}
	
}
