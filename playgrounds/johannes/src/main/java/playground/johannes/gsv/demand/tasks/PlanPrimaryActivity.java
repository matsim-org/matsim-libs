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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import playground.johannes.coopsim.mental.choice.ChoiceSet;
import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.demand.PopulationTask;
import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.socialnetworks.gis.OrthodromicDistanceCalculator;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 *
 */
public class PlanPrimaryActivity implements PopulationTask {
	
	private static final Logger logger = Logger.getLogger(PlanPrimaryActivity.class);

	private final ZoneLayer<Double> attractivities;
	
	private final Map<Zone<Double>, ChoiceSet<Zone<Double>>> choiceSets;
	
	private final Random random = new XORShiftRandom();
	
	private final GeometryFactory geoFactory = new GeometryFactory();
	
//	private MathTransform transform;
	
	private MathTransform transformReverse;
	
	private final Map<Zone<Double>, List<Point>> stopMap;
	
	public PlanPrimaryActivity(ZoneLayer<Double> attractivites, TransitSchedule schedule) {
		this.attractivities = attractivites;
		
		choiceSets = new HashMap<Zone<Double>, ChoiceSet<Zone<Double>>>();
		
		try {
//			transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, CRSUtils.getCRS(31467));
			transformReverse = CRS.findMathTransform(CRSUtils.getCRS(31467), DefaultGeographicCRS.WGS84);
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		stopMap = new LinkedHashMap<Zone<Double>, List<Point>>(attractivites.getZones().size());
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
			Zone<Double> zone = attractivities.getZone(pointWGS84);

			if (zone != null) {
				List<Point> stops = stopMap.get(zone);
				if (stops == null) {
					stops = new ArrayList<Point>(100);
					stopMap.put(zone, stops);
				}
				stops.add(pointWGS84);
			}
		}
		
	}
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.demand.PopulationTask#apply(org.matsim.api.core.v01.population.Population)
	 */
	@Override
	public void apply(Population pop) {
		ProgressLogger.init(pop.getPersons().size(), 1, 10);
		for(Person p : pop.getPersons().values()) {
			Plan plan = p.getPlans().get(0);
			
			Activity homeAct = (Activity) plan.getPlanElements().get(0);
			Point centroid = MatsimCoordUtils.coordToPoint(homeAct.getCoord());
			centroid.setSRID(4326);
			Zone<Double> homeZone = attractivities.getZone(centroid);
			
			if(homeZone == null) {
				homeZone = attractivities.getZones().iterator().next(); //FIXME get random zone
			}
			
			ChoiceSet<Zone<Double>> choiceSet = choiceSets.get(homeZone);
			if(choiceSet == null) {
				choiceSet = new ChoiceSet<Zone<Double>>(random);
				for(Zone<Double> zone : attractivities.getZones()) {
					double d = OrthodromicDistanceCalculator.getInstance().distance(homeZone.getGeometry().getCentroid(), zone.getGeometry().getCentroid());
					double w = Math.pow(d, -1.4) * zone.getAttribute();
					choiceSet.addChoice(zone, w);
				}
				choiceSets.put(homeZone, choiceSet);
				
			}
			
			Zone<Double> target = choiceSet.randomWeightedChoice();
			List<Point> stops = stopMap.get(target);
			
			Point point = null;
			if(stops == null || stops.isEmpty()) {
				point = target.getGeometry().getCentroid();
				logger.info("No stops in zone, using centroid");
			} else {
				point = stops.get(random.nextInt(stops.size()));
			}
			
//			Envelope env = target.getGeometry().getEnvelopeInternal();
//	
//			double x = Double.NaN;
//			double y = Double.NaN;
//			boolean hit = false;
//			while (!hit) {
//				x = random.nextDouble() * env.getWidth()	+ env.getMinX();
//				y = random.nextDouble() * env.getHeight() + env.getMinY();
//				Point point = geoFactory.createPoint(new Coordinate(x, y));
//
//				if (target.getGeometry().contains(point)) {
//					hit = true;
//				}
//			}
			
			plan.addLeg(pop.getFactory().createLeg("undefined"));

			plan.addActivity(pop.getFactory().createActivityFromCoord("work", new Coord(point.getX(), point.getY())));
			
			ProgressLogger.step();
		}
		ProgressLogger.termiante();
	}

}
