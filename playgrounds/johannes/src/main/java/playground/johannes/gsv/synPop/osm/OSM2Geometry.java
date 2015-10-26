/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.johannes.gsv.synPop.osm;

import com.vividsolutions.jts.geom.*;

/**
 * @author johannes
 *
 */
public class OSM2Geometry {

	private GeometryFactory factory = new GeometryFactory();
	
	public Geometry convert(OSMWay way) {
		Coordinate coords[] = new Coordinate[way.getNodes().size()];
		for(int i = 0; i < way.getNodes().size(); i++) {
			OSMNode node = way.getNodes().get(i);
			coords[i] = new Coordinate(node.getLongitude(), node.getLatitude());
		}
		LinearRing ring = factory.createLinearRing(coords);
		
		Polygon poly = factory.createPolygon(ring, null);
		
		return poly;
	}
}
