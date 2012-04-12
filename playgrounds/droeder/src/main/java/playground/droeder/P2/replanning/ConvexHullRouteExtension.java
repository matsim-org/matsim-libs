/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.P2.replanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.replanning.PPlanStrategy;
import playground.andreas.P2.replanning.PStrategy;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;

/**
 * @author droeder
 *
 */
public class ConvexHullRouteExtension extends PStrategy implements PPlanStrategy{
	
	private static final Logger log = Logger
			.getLogger(ConvexHullRouteExtension.class);
	public static final String STRATEGY_NAME = "ConvexHullRouteExtension";

	/**
	 * @param parameter
	 */
	public ConvexHullRouteExtension(ArrayList<String> parameter) {
		super(parameter);
	}

	/* (non-Javadoc)
	 * @see playground.andreas.P2.replanning.PPlanStrategy#run(playground.andreas.P2.pbox.Cooperative)
	 */
	@Override
	public PPlan run(Cooperative cooperative) {
		Map<Id, TransitStopFacility> currentlyUsedStops = this.getUsedStops(cooperative);
		Geometry hull = this.createConvexHulls(currentlyUsedStops);
		List<TransitStopFacility> newHullInteriorStops = 
			this.findNewHullInteriorStops(cooperative.getBestPlan().getStopsToBeServed(), currentlyUsedStops, hull);
		
		TransitStopFacility newStop = this.drawStop(newHullInteriorStops);
		if(newStop == null){
			log.error("can not create a new route for cooperative " + cooperative.getId() + ", because there is no unused stop in the convex hull of the old route...");
			return null;
		}else{
			PPlan plan = new PPlan(new IdImpl(cooperative.getCurrentIteration()));
//			plan.se
			
			return plan;
		}
	}


	/**
	 * @param cooperative
	 * @return
	 */
	private Map<Id, TransitStopFacility> getUsedStops(Cooperative cooperative) {
		Map<Id, TransitStopFacility> currentlyUsedStops = new HashMap<Id, TransitStopFacility>();
		
		for (TransitRoute route : cooperative.getBestPlan().getLine().getRoutes().values()) {
			for (TransitRouteStop stop : route.getStops()) {
				currentlyUsedStops.put(stop.getStopFacility().getId(), stop.getStopFacility());
			}
		}
		return currentlyUsedStops;
	}

	/**
	 * @param currentlyUsedStops
	 * @return
	 */
	private Geometry createConvexHulls(Map<Id, TransitStopFacility> currentlyUsedStops) {
		Point[] points = new Point[currentlyUsedStops.size()];

		int i = 0;
		for(TransitStopFacility facility : currentlyUsedStops.values()){
			points[i] = MGC.coord2Point(facility.getCoord());
			i++;
		}
		
		MultiPoint pointCloud = new MultiPoint(points, new GeometryFactory());
		
			
		return pointCloud.convexHull();
	}

	/**
	 * returns stops in the convex hull, which are not part of the current route, but of its convex hull
	 * 
	 * @param stopsToBeServed
	 * @param currentlyUsedStops
	 * @param hull
	 * @return
	 */
	private List<TransitStopFacility> findNewHullInteriorStops(ArrayList<TransitStopFacility> stopsToBeServed,
																Map<Id, TransitStopFacility> currentlyUsedStops, 
																Geometry hull) {
		List<TransitStopFacility> stopCandidates = new ArrayList<TransitStopFacility>();
		
		for(TransitStopFacility s: stopsToBeServed){
			if(!currentlyUsedStops.containsKey(s.getId())){
				if(hull.contains(MGC.coord2Point(s.getCoord()))){
					stopCandidates.add(s);
				}
			}
		}
		return stopCandidates;
	}
	
	/**
	 * @param newHullInteriorStops
	 * @return
	 */
	private TransitStopFacility drawStop(
			List<TransitStopFacility> newHullInteriorStops) {
		if(newHullInteriorStops.size() == 0){
			return null;
		}else{
			Double rnd;
			TransitStopFacility newStop = null;
			if(newHullInteriorStops.size() > 1){
				do{
					//draw a random stop, if more than one stop is in the convex hull
					for(TransitStopFacility f : newHullInteriorStops){
						rnd = MatsimRandom.getRandom().nextDouble();
						if(rnd < 0.1){
							newStop = f;
							break;
						}
					}
					
				}while(newStop == null);
			}else{
				newStop = newHullInteriorStops.get(0);
			}
			return newStop;
		}
	}

	/* (non-Javadoc)
	 * @see playground.andreas.P2.replanning.PPlanStrategy#getName()
	 */
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

}
