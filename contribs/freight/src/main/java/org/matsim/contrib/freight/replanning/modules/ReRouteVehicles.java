/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.freight.replanning.modules;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.router.TimeAndSpaceTourRouter;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

/**
 * Strategy module to reRoute a carrierPlan.
 * 
 * @author sschroeder
 *
 */
public class ReRouteVehicles implements GenericPlanStrategyModule<CarrierPlan>{

	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(ReRouteVehicles.class);
	
	private LeastCostPathCalculator router;
	
	private Network network;
	
	private TravelTime travelTime;

    private double probability = 1.;
	
	/**
	 * Constructs the module with a leastCostPathRouter, network and travelTime.
	 * 
	 * @param router
	 * @param network
	 * @param travelTime
	 * @see LeastCostPathCalculator, Network, TravelTime
	 */
	public ReRouteVehicles(LeastCostPathCalculator router, Network network, TravelTime travelTime) {
		super();
		this.router = router;
		this.network = network;
		this.travelTime = travelTime;
	}

    public ReRouteVehicles(LeastCostPathCalculator router, Network network, TravelTime travelTime, double probability) {
        super();
        this.router = router;
        this.network = network;
        this.travelTime = travelTime;
        this.probability = probability;
    }

	/**
	 * Routes the carrierPlan in time and space.
	 * 
	 * @param carrierPlan
	 * @throws IllegalStateException if carrierPlan is null.
	 * @see TimeAndSpaceTourRouter
	 */
	@Override
	public void handlePlan(CarrierPlan carrierPlan) {
		if(carrierPlan == null) throw new IllegalStateException("carrierPlan is null and cannot be handled.");
		route(carrierPlan);
	}
	
	private void route(CarrierPlan carrierPlan) {
		for(ScheduledTour tour : carrierPlan.getScheduledTours()){
            if(MatsimRandom.getRandom().nextDouble() < probability){
                new TimeAndSpaceTourRouter(router, network, travelTime).route(tour);
            }
		}
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
	}

	@Override
	public void finishReplanning() {
	}


}
