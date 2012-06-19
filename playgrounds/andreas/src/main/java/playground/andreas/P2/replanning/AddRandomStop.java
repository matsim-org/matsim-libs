/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.andreas.P2.replanning;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;

/**
 * 
 * Limits demand to stops with demand.
 * 
 * @author aneumann
 *
 */
public class AddRandomStop extends PStrategy implements PPlanStrategy{
	
	private final static Logger log = Logger.getLogger(AddRandomStop.class);
	
	public static final String STRATEGY_NAME = "AddRandomStop";

	public AddRandomStop(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 0){
			log.error("Too many parameter. Will ignore: " + parameter);
		}
		log.info("enabled");
	}
	
	@Override
	public PPlan run(Cooperative cooperative) {
		
		Set<Id> stopsInService = new TreeSet<Id>();
		for (TransitRoute route : cooperative.getBestPlan().getLine().getRoutes().values()) {
			for (TransitRouteStop stop : route.getStops()) {
				stopsInService.add(stop.getStopFacility().getId());
			}
		}
		
		TransitStopFacility newStop;
		do {
			newStop  = cooperative.getRouteProvider().getRandomTransitStop(cooperative.getCurrentIteration());
		} while (stopsInService.contains(newStop.getId()));
		
		TransitStopFacility nearestStop = null;
		double distanceToNearestStop = Double.MAX_VALUE;
		
		for (TransitStopFacility stop : cooperative.getBestPlan().getStopsToBeServed()) {
			double distance = Math.sqrt(Math.pow(stop.getCoord().getX() - newStop.getCoord().getX(), 2) + Math.pow(stop.getCoord().getY() - newStop.getCoord().getY(), 2));
			if (distance < distanceToNearestStop) {
				nearestStop = stop;
			}
		}
		
		ArrayList<TransitStopFacility> newStopsToBeServed = new ArrayList<TransitStopFacility>();
		
		for (TransitStopFacility stop : cooperative.getBestPlan().getStopsToBeServed()) {
			newStopsToBeServed.add(stop);
			if (stop.getId() == nearestStop) {
				newStopsToBeServed.add(newStop);
			}
		}
						
		// profitable route, change startTime
		PPlan newPlan = new PPlan(new IdImpl(cooperative.getCurrentIteration()), this.getName());
		newPlan.setStopsToBeServed(newStopsToBeServed);
		newPlan.setStartTime(cooperative.getBestPlan().getStartTime());
		newPlan.setEndTime(cooperative.getBestPlan().getEndTime());
		
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan.getStartTime(), newPlan.getEndTime(), 1, newPlan.getStopsToBeServed(), new IdImpl(cooperative.getCurrentIteration())));
		
		return newPlan;
	}

	@Override
	public String getName() {
		return AddRandomStop.STRATEGY_NAME;
	}
}