/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityMover.java
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
package playground.johannes.socialnetworks.sim.locationChoice;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;

/**
 * @author illenberger
 *
 */
public class ActivityMover {

	private final PopulationFactory factory;
	
	private NetworkFactoryImpl netFactory;
	
	private NetworkLegRouter legRouter;
	
	private ActivityFacilities facilities;
	
	public ActivityMover(PopulationFactory factory, LeastCostPathCalculator router, Network network, ActivityFacilities facilities) {
		this.factory = factory;
		netFactory = new NetworkFactoryImpl((NetworkImpl) network);
		legRouter = new NetworkLegRouter(network, router, netFactory);
		this.facilities = facilities;
	}
	
	public void moveActivity(Plan plan, int idx, Id newFacility, double desiredArrivalTime, double desiredDuration) {
		Activity act = (Activity) plan.getPlanElements().get(idx);
		
		ActivityFacility facility = facilities.getFacilities().get(newFacility);
		Activity newAct = factory.createActivityFromLinkId(act.getType(), facility.getLinkId());
		((ActivityImpl)newAct).setFacilityId(newFacility);
		
		if(Double.isInfinite(act.getEndTime())) {
			act.setEndTime(act.getStartTime() + ((ActivityImpl) act).getMaximumDuration());
		}
		
		Activity prev = (Activity) plan.getPlanElements().get(idx - 2);
		Leg toLeg = (Leg)plan.getPlanElements().get(idx - 1);
		Leg fromLeg = (Leg)plan.getPlanElements().get(idx + 1);
		Activity next = (Activity) plan.getPlanElements().get(idx + 2);
		
		newAct.setStartTime(desiredArrivalTime);
		newAct.setEndTime(desiredArrivalTime + desiredDuration);
		
		calcRoute(plan.getPerson(), prev, newAct, toLeg);
		calcRoute(plan.getPerson(), newAct, next, fromLeg);
		
		double newEndTime = desiredArrivalTime - toLeg.getTravelTime();
		newEndTime = Math.max(0, newEndTime);
		
		prev.setEndTime(newEndTime);
		toLeg.setDepartureTime(newEndTime);
		
		next.setStartTime(newAct.getEndTime() + fromLeg.getTravelTime());
		
		plan.getPlanElements().set(idx, newAct);
	}
	
	private double calcRoute(Person person, Activity prev, Activity next, Leg leg) {
		
		legRouter.routeLeg(person, leg, prev, next, prev.getEndTime());

		return Double.NaN;
	}
}
