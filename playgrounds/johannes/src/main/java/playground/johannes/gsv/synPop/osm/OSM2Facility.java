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
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.socnetgen.util.MatsimCoordUtils;
import org.matsim.facilities.*;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;

import java.util.Collection;

/**
 * @author johannes
 * 
 */
public class OSM2Facility {

	private static GeometryFactory factory = new GeometryFactory();

	public static void createFacilities(ActivityFacilities facilities,
			ActivityFacilitiesFactory factory, Collection<OSMWay> ways, String actOption) {

		MathTransform tranform = null;
		try {
			tranform = CRS.findMathTransform(DefaultGeographicCRS.WGS84,
					CRSUtils.getCRS(31467));
		} catch (FactoryException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int cnt = 0;
		for (OSMWay way : ways) {
			try {
				Geometry geo = convert(way);
				// Coord coord =
				// MatsimCoordUtils.pointToCoord(geo.getCentroid());
				Point p = CRSUtils.transformPoint(geo.getCentroid(), tranform);
				Coord coord = MatsimCoordUtils.pointToCoord(p);
				ActivityFacility facility = factory.createActivityFacility(
						Id.create(String.valueOf(cnt), ActivityFacility.class), coord);
				ActivityOption option = factory.createActivityOption(actOption);
				facility.addActivityOption(option);
				facilities.addActivityFacility(facility);
				cnt++;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}

	}

	private static Geometry convert(OSMWay way) {
		Coordinate coords[] = new Coordinate[way.getNodes().size()];
		for (int i = 0; i < way.getNodes().size(); i++) {
			OSMNode node = way.getNodes().get(i);
			coords[i] = new Coordinate(node.getLongitude(), node.getLatitude());
		}
		LinearRing ring = factory.createLinearRing(coords);

		Polygon poly = factory.createPolygon(ring, null);

		return poly;
	}

	public static void main(String args[]) {
		String type = "shop";
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		parser.parse("/home/johannes/gsv/osm/germany-"+type+".osm");

		Collection<OSMWay> ways = parser.getWays().values();

		ActivityFacilitiesFactoryImpl ffactory = new ActivityFacilitiesFactoryImpl();
		ActivityFacilities facilities = FacilitiesUtils
				.createActivityFacilities();
		createFacilities(facilities, ffactory, ways, type);

		System.out.println(String.format("Created %s facilities.", facilities
				.getFacilities().size()));

		FacilitiesWriter writer = new FacilitiesWriter(facilities);
		writer.write("/home/johannes/gsv/osm/facilities.ger."+type+".xml");
	}
}
