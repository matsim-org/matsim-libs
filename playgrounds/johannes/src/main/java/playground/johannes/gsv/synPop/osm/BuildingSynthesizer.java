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
import com.vividsolutions.jts.index.quadtree.Quadtree;
import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author johannes
 * 
 */
public class BuildingSynthesizer {

	private static final Logger logger = Logger.getLogger(BuildingSynthesizer.class);

	private Random random = new XORShiftRandom();

	private GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);

	public void synthesize(Collection<OSMObject> objects, ActivityFacilities facilities, double defaultSize) {
		Quadtree quadTree = new Quadtree();

		logger.info("Inserting areas in quad tree...");
		ProgressLogger.init(objects.size(), 1, 10);
		for (OSMObject obj : objects) {
			if (obj.getType().equalsIgnoreCase(OSMObject.AREA)) {
				Envelope env = obj.getGeometry().getEnvelopeInternal();
				if(env != null) {
					quadTree.insert(env, obj); // exponent out of bounds exception?
				}
			}
			ProgressLogger.step();
		}

		Map<OSMObject, Set<OSMObject>> areaBuildingMap = new HashMap<OSMObject, Set<OSMObject>>();

		logger.info("Assigning buildings to areas...");
		ProgressLogger.init(objects.size(), 1, 10);
		for (OSMObject obj : objects) {
			if (obj.getType().equalsIgnoreCase(OSMObject.BUILDING)) {
				List<OSMObject> areas = quadTree.query(obj.getGeometry().getEnvelopeInternal());

				for (OSMObject area : areas) {
					if (area.getGeometry().contains(obj.getGeometry())) {
						Set<OSMObject> buildings = areaBuildingMap.get(area);
						if (buildings == null) {
							buildings = new HashSet<OSMObject>();
							areaBuildingMap.put(area, buildings);
						}
						buildings.add(obj);
					}
				}
			}
			ProgressLogger.step();
		}

		int idCounter = 0;

		logger.info("Creating buildings in areas...");
		ProgressLogger.init(areaBuildingMap.size(), 1, 10);
		for (Entry<OSMObject, Set<OSMObject>> entry : areaBuildingMap.entrySet()) {
			OSMObject area = entry.getKey();
			double A = area.getGeometry().getArea();
			double size = defaultSize * defaultSize;
			double n = A / size;

			if (entry.getValue().size() < n) {
				int n2 = (int) (n - entry.getValue().size());

				for (int i = 0; i < n2; i++) {
					Coord c = generateRandomCoordinate(area.getGeometry());
					ActivityFacility facility = facilities.getFactory().createActivityFacility(
							Id.create("new" + idCounter++, ActivityFacility.class), c);
					facilities.addActivityFacility(facility);
				}
			}

			for (OSMObject building : entry.getValue()) {
				Coord c = MatsimCoordUtils.pointToCoord(building.getGeometry().getCentroid());
				Id<ActivityFacility> id = Id.create(building.getId(), ActivityFacility.class);
				if (facilities.getFacilities().get(id) == null) {
					ActivityFacility facility = facilities.getFactory().createActivityFacility(id, c);
					facilities.addActivityFacility(facility);
					objects.remove(building);
				}

			}
			ProgressLogger.step();
		}

		Quadtree buildingTree = new Quadtree();

		logger.info("Processing buildings...");
		ProgressLogger.init(objects.size(), 1, 10);
		for (OSMObject building : objects) {
			if (building.getType().equalsIgnoreCase(OSMObject.BUILDING)) {
				try {
					Coord c = MatsimCoordUtils.pointToCoord(building.getGeometry().getCentroid());
					ActivityFacility facility = facilities.getFactory()
							.createActivityFacility(Id.create(building.getId(), ActivityFacility.class), c);
					facilities.addActivityFacility(facility);

					buildingTree.insert(building.getGeometry().getEnvelopeInternal(), building);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			ProgressLogger.step();
		}

		logger.info("Processing POIs...");
		ProgressLogger.init(objects.size(), 1, 10);
		for (OSMObject poi : objects) {
			if (poi.getType().equalsIgnoreCase(OSMObject.POI)) {
				List<OSMObject> result = buildingTree.query(poi.getGeometry().getEnvelopeInternal());
				boolean hit = false;
				for (OSMObject geo : result) {
					if (geo.getGeometry().contains(poi.getGeometry())) {
						hit = true;
						break;
					}
				}

				// check if in area
				if (!hit) {
					result = quadTree.query(poi.getGeometry().getEnvelopeInternal());
					hit = false;
					for (OSMObject geo : result) {
						if (geo.getGeometry().contains(poi.getGeometry())) {
							hit = true;
							break;
						}
					}
				}

				if (!hit) {
					Coord c = MatsimCoordUtils.pointToCoord(poi.getGeometry().getCentroid());
					ActivityFacility facility = facilities.getFactory().createActivityFacility(
							Id.create("poi" + poi.getId(), ActivityFacility.class), c);
					facilities.addActivityFacility(facility);
				}
			}
			ProgressLogger.step();
		}
	}

	private Coord generateRandomCoordinate(Geometry geometry) {
		Envelope env = geometry.getEnvelopeInternal();
		double deltaX = env.getMaxX() - env.getMinX();
		double deltaY = env.getMaxY() - env.getMinY();

		boolean hit = false;

		double x = 0;
		double y = 0;

		Point p = null;
		while (!hit) {
			x = env.getMinX() + random.nextDouble() * deltaX;
			y = env.getMinY() + random.nextDouble() * deltaY;

			p = factory.createPoint(new Coordinate(x, y));
			hit = geometry.contains(p);
		}

		return new Coord(x, y);
	}

	public static void main(String args[]) {
		XMLParser parser = new XMLParser();
		parser.setValidating(false);

		logger.info("Parsing osm file...");
		parser.parse(args[0]);

		double defaultSize = Double.parseDouble(args[2]);

		Collection<OSMWay> ways = parser.getWays().values();
		Collection<OSMNode> nodes = parser.getNodes().values();

		OSMObjectBuilder builder = new OSMObjectBuilder();
		Set<OSMObject> objects = new HashSet<OSMObject>();
		logger.info("Processing ways...");
		ProgressLogger.init(ways.size(), 1, 10);
		int failures = 0;
		for (OSMWay way : ways) {
			OSMObject obj = builder.build(way);
			if (obj != null)
				objects.add(obj);
			else
				failures++;
			ProgressLogger.step();
		}
		logger.info(String.format("Total built %s objects, %s failures.", objects.size(), failures));
		
		logger.info("Processing nodes...");
		ProgressLogger.init(nodes.size(), 1, 10);
		for (OSMNode node : nodes) {
			OSMObject obj = builder.build(node);
			if (obj != null)
				objects.add(obj);
			else
				failures++;
			ProgressLogger.step();
		}
		logger.info(String.format("Total built %s objects, %s failures.", objects.size(), failures));

		logger.info("Transforming objects...");
		transform(objects);

		logger.info("Building facilitites...");
		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();

		BuildingSynthesizer synt = new BuildingSynthesizer();
		synt.synthesize(objects, facilities, defaultSize);

		logger.info(String.format("Created %s facilities.", facilities.getFacilities().size()));

		FacilitiesWriter writer = new FacilitiesWriter(facilities);
		writer.write(args[1]);
	}

	private static void transform(Collection<OSMObject> objects) {
		MathTransform transform = null;
		try {
			transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, CRSUtils.getCRS(31467));
		} catch (FactoryException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for (OSMObject obj : objects) {
			for (Coordinate coord : obj.getGeometry().getCoordinates()) {
				double[] points = new double[] { coord.x, coord.y };
				try {
					transform.transform(points, 0, points, 0, 1);
				} catch (TransformException e) {
					e.printStackTrace();
				}

				coord.x = points[0];
				coord.y = points[1];
			}
		}
	}
}
