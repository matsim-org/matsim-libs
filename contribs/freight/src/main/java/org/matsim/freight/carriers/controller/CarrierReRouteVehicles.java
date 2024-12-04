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

package org.matsim.freight.carriers.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.freight.carriers.CarrierPlan;
import org.matsim.freight.carriers.ScheduledTour;

/**
 * Strategy module to reRoute a carrierPlan.
 *
 * @author sschroeder
 *
 */
public class CarrierReRouteVehicles implements GenericPlanStrategyModule<CarrierPlan>{

	@SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger( CarrierReRouteVehicles.class );

	public static final class Factory {
		private final LeastCostPathCalculator router;
		private final Network network;
		private final TravelTime travelTime;
		private double probability = 1.;
		public Factory( LeastCostPathCalculator router, Network network, TravelTime travelTime ){
			this.router = router;
			this.network = network;
			this.travelTime = travelTime;
		}
		public CarrierReRouteVehicles build() {
			return new CarrierReRouteVehicles( router, network, travelTime, probability );
		}
		public Factory setProbability( double probability ){
			this.probability = probability;
			return this;
		}
	}

	private final LeastCostPathCalculator router;
	private final Network network;
	private final TravelTime travelTime;
	private final double probability ;
	private CarrierReRouteVehicles( LeastCostPathCalculator router, Network network, TravelTime travelTime, double probability ) {
		super();
		this.router = router;
		this.network = network;
		this.travelTime = travelTime;
		this.probability = probability;
	}

	/**
	 * Routes the carrierPlan in time and space.
	 *
	 * @param carrierPlan				the carrierPlan to be routed.
	 * @throws IllegalStateException 	if carrierPlan is null.
	 * @see CarrierTimeAndSpaceTourRouter
	 */
	@Override
	public void handlePlan(CarrierPlan carrierPlan) {
		if(carrierPlan == null) throw new IllegalStateException("carrierPlan is null and cannot be handled.");
		for(ScheduledTour tour : carrierPlan.getScheduledTours()){
			if(MatsimRandom.getRandom().nextDouble() < probability){
				new CarrierTimeAndSpaceTourRouter(router, network, travelTime).route(tour );
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
