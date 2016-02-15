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
import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTSFactoryFinder;

/**
 * @author johannes
 *
 */
public class OSMObjectBuilder {

	private static final Logger logger = Logger.getLogger(OSMObjectBuilder.class);
	
	private GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
	
	public OSMObject build(OSMWay way) {
		OSMObject obj = new OSMObject(way.getId());
		
		if(way.tags().keySet().contains("landuse")) {
			obj.setType(OSMObject.AREA);
		} else {
			obj.setType(OSMObject.BUILDING);
		
		}

		Geometry geo = buildGeometry(way);
		if(geo == null)
			return null;
		
		obj.setGeometry(geo);
		
		return obj;
	}
	
	public OSMObject build(OSMNode node) {
		if (!node.isNodeOfWay()) {
			OSMObject obj = new OSMObject(node.getId());
			obj.setType(OSMObject.POI);

			Point p = factory.createPoint(new Coordinate(node.getLongitude(),
					node.getLatitude()));
			obj.setGeometry(p);

			return obj;
		} else {
			return null;
		}
	}
	
	private Geometry buildGeometry(OSMWay way) {
		Coordinate coords[] = new Coordinate[way.getNodes().size()];
		for (int i = 0; i < way.getNodes().size(); i++) {
			OSMNode node = way.getNodes().get(i);
			coords[i] = new Coordinate(node.getLongitude(), node.getLatitude());
		}
		
		LinearRing ring;
		try {
			ring = factory.createLinearRing(coords);
		} catch (IllegalArgumentException e) {
			logger.trace("Failed to create polygon. Coords = " + coords.length);
			return null;
		}

		Polygon poly = factory.createPolygon(ring, null);
		
		if(poly.isEmpty()) {
			return null;
		}
		return poly;
	}
}
