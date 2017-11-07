/**
 * 
 */
package org.matsim.core.utils.geometry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * @author kainagel
 *
 */
public class GeometryUtils {
	private GeometryUtils() {} // do not instantiate

	/**
	 * Method to find all links in {@link Network} that intersect a given {@link Link}.  Convenience method that
	 * only uses MATSim objects.
	 * 
	 * @param link
	 * @param network
	 * @return
	 */
	public static List<Link> findIntersectingLinks( Link link, final Network network ) {
		LineString segment = GeometryUtils.createGeotoolsLineString(link) ;
		return GeometryUtils.findIntersectingLinks(segment, network) ;
	}

	/**
	 * Method to find all links in {@link Network} that intersect a given {@link LineString}.
	 * 
	 * @param lineString
	 * @param network
	 * @return
	 * 
	 * @see {@link GeometryUtilsTest#testIntersectingLinks()}
	 */
	public static List<Link> findIntersectingLinks(LineString lineString, final Network network) {
		// yy One could probably improve this method by using the (already existing) link quadtree to look only at
		// those links that are in the bounding box.  kai, oct'17
		
		// convert matsim links into geotools line strings:
		Map<Link,LineString> segments = new LinkedHashMap<>() ;
		for ( Link link : network.getLinks().values() ) {
			LineString theSegment = GeometryUtils.createGeotoolsLineString(link);
			segments.put( link, theSegment ) ;
		}
	
		// find the intersecting segments:
		List<Link> resultList = new ArrayList<>();
		for ( Entry<Link, LineString> entry : segments.entrySet() ) {
			if( lineString.intersects( entry.getValue() ) ){
				resultList.add( entry.getKey() ) ;
			}
		}
		return resultList;
	}

	/**
	 * Helper method for type conversion.
	 * 
	 * @param link
	 * @return
	 */
	public static LineString createGeotoolsLineString(Link link) {
		Coordinate fromCoord = CoordUtils.createGeotoolsCoordinate( link.getFromNode().getCoord() ) ;
		Coordinate toCoord = CoordUtils.createGeotoolsCoordinate( link.getToNode().getCoord() ) ;
		LineString theSegment = new GeometryFactory().createLineString(new Coordinate[]{ fromCoord, toCoord });
		return theSegment;
	}
	
}
