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

/**
 * 
 */
package playground.johannes.gsv.demand.tasks;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import playground.johannes.gsv.demand.ActivityTypes;
import playground.johannes.gsv.demand.PopulationTask;

import java.util.*;

/**
 * @author johannes
 * 
 */
public class PersonStopDistribution implements PopulationTask {

	private static final Logger logger = Logger
			.getLogger(PersonStopDistribution.class);

	private final ZoneLayer<Double> zoneLayer;

	private final Map<Zone<Double>, Collection<Coord>> stopMap;

	private final int radius = 1000;

	private MathTransform transform;

	private MathTransform transformReverse;

	private final Random random;

//	private final TransitSchedule schedule;

	private final GeometryFactory geoFactory = new GeometryFactory();

	public PersonStopDistribution(TransitSchedule schedule,
			ZoneLayer<Double> zoneLayer, Random random) {
		this.zoneLayer = zoneLayer;
		this.random = random;
//		this.schedule = schedule;

		try {
			transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, CRSUtils.getCRS(31468));
			transformReverse = CRS.findMathTransform(CRSUtils.getCRS(31467), DefaultGeographicCRS.WGS84);
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		stopMap = new LinkedHashMap<Zone<Double>, Collection<Coord>>(zoneLayer.getZones().size());
		/*
		 * assign transit stop facilities to zones
		 */
		for (TransitStopFacility stop : schedule.getFacilities().values()) {
			double points[] = new double[] { stop.getCoord().getX(), stop.getCoord().getY() };
			
			try {
				transformReverse.transform(points, 0, points, 0, 1);
			} catch (TransformException e) {
				e.printStackTrace();
			}
			Point pointWGS84 = geoFactory.createPoint(new Coordinate(points[0], points[1]));
			pointWGS84.setSRID(4326);
			
			Zone<Double> zone = zoneLayer.getZone(pointWGS84);
			
			if (zone != null) {
				Collection<Coord> stops = stopMap.get(zone);
				if (stops == null) {
					stops = new LinkedHashSet<Coord>();
					stopMap.put(zone, stops);
				}
				stops.add(stop.getCoord());
			}
		}

	}

	@Override
	public void apply(Population pop) {

		int N = pop.getPersons().size();
		/*
		 * shuffle persons
		 */
		List<Person> persons = new ArrayList<Person>(pop.getPersons().values());
		Collections.shuffle(persons, random);

		ProgressLogger.init(N, 1, 10);
		int processed = 0;
		double fracSum = 0;
		List<Zone<Double>> zones = new ArrayList<Zone<Double>>();
		for (Zone<Double> zone : zoneLayer.getZones()) {
			Collection<Coord> stops = stopMap.get(zone);
			if(stops != null) {
				zones.add(zone);
				fracSum += zone.getAttribute();
			}
		}
		
		logger.info(String.format("%s out of %s zone with stops.", zones.size(), zoneLayer.getZones().size()));
		for(Zone<Double> zone : zones) {
			double fraction = zone.getAttribute()/fracSum;
			
			/*
			 * number of persons to create;
			 */
			int n = (int) Math.ceil(N * fraction);

			Collection<Coord> stops = stopMap.get(zone);
			if (stops == null) {
				throw new RuntimeException("Ooops");
//				logger.warn("Zone has no stops. Creating dummy stop.");
//
//				Point p = zone.getGeometry().getCentroid();
//				double points[] = new double[] { p.getCoordinate().x, p.getCoordinate().y };
//				logger.warn(String.format("Centroid x=%s,  y=%s.", points[0], points[1]));
//				
//				try {
//					transform.transform(points, 0, points, 0, 1);
//				} catch (TransformException e) {
//					e.printStackTrace();
//				}
//
//				stops = new ArrayList<Coord>(1);
//				stops.add(new CoordImpl(points[0], points[1]));
//
			}
			
			int nPerStop = (int) Math.ceil(n / (double) stops.size());

			for (Coord stop : stops) {
				for (int i = 0; i < nPerStop; i++) {
					/*
					 * check if all persons already processed
					 */
					if (processed >= N) {
						break;
					}

					Point pointWGS84 = null;
					double tmpRadius = radius;
					while (pointWGS84 == null) {
						double x = stop.getX() + random.nextDouble() * tmpRadius;
						double y = stop.getY() + random.nextDouble() * tmpRadius;

						double[] points = new double[] { x, y };
						try {
							transformReverse.transform(points, 0, points, 0, 1);
						} catch (TransformException e) {
							e.printStackTrace();
						}

						Point tmp = geoFactory.createPoint(new Coordinate(points[0], points[1]));
						if (zone.getGeometry().contains(tmp)) {
							pointWGS84 = tmp;
						} else {
							tmpRadius--; // decrease radius
							if(tmpRadius == 0)
								pointWGS84 = tmp;
						}
					}

					/*
					 * add home activity to first plan
					 */
					Person person = persons.get(processed);
					Plan p = person.getPlans().get(0);
					p.addActivity(pop.getFactory().createActivityFromCoord(
							ActivityTypes.HOME, new Coord(pointWGS84.getX(), pointWGS84.getY())));

					processed++;
					ProgressLogger.step();
				}
			}

			// } else {
			// // throw new RuntimeException("No transit stops in zone.");
			// logger.warn("No stops in zone.");
			// }
		}

		if (processed < N) {
			// i think this should never happen because we use ceil(N *
			// fraction)
			throw new RuntimeException(String.format("%s persons unprocessed.",
					N - processed));
		}

		ProgressLogger.termiante();
	}

}
