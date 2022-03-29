/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.controler;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

/**
 * Router routing carrierPlans in time and space.
 * 
 * @author sschroeder
 *
 */
public class TimeAndSpacePlanRouter {

	private LeastCostPathCalculator router;
	
	private Network network;
	
	private TravelTime travelTime;
	
	public TimeAndSpacePlanRouter(LeastCostPathCalculator router, Network network, TravelTime travelTime) {
		super();
		this.router = router;
		this.network = network;
		this.travelTime = travelTime;
	}

	/**
	 * Routes all scheduled tours within the plan in time and space.
	 * 
	 * @param plan
	 * @see TimeAndSpaceTourRouter
	 */
	public void run(CarrierPlan plan) {
		if(plan == null) throw new IllegalStateException("plan is missing.");
		for(ScheduledTour tour : plan.getScheduledTours()){
			new TimeAndSpaceTourRouter(router, network, travelTime).route(tour);
		}	
	}
	
	public void run(Carriers carriers){
		for(Carrier carrier : carriers.getCarriers().values()){
			CarrierPlan p = carrier.getSelectedPlan();
			if(p != null){
				run(p);
			}
		}
	}

}
