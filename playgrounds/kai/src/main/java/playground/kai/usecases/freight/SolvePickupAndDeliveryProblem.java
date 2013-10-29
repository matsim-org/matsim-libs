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

package playground.kai.usecases.freight;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategyModule;
import org.matsim.contrib.freight.router.TimeAndSpaceTourRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

import util.Solutions;
import algorithms.SchrimpfFactory;
import basics.VehicleRoutingAlgorithm;
import basics.VehicleRoutingProblem;
import basics.VehicleRoutingProblem.Builder;
import basics.VehicleRoutingProblemSolution;

/**
 * This is an attempt to construct a second strategy which solves the pick-and-delivery problem using jsprig
 * 
 * @author nagel
 *
 */
public class SolvePickupAndDeliveryProblem implements CarrierReplanningStrategyModule{

	private static Logger logger = Logger.getLogger(SolvePickupAndDeliveryProblem.class);
	
	private LeastCostPathCalculator router;
	
	private Network network;
	
	private TravelTime travelTime;
	
	/**
	 * Constructs the module with a leastCostPathRouter, network and travelTime.
	 * 
	 * @param router
	 * @param network
	 * @param travelTime
	 * @see LeastCostPathCalculator, Network, TravelTime
	 */
	public SolvePickupAndDeliveryProblem(LeastCostPathCalculator router, Network network, TravelTime travelTime) {
		super();
		this.router = router;
		this.network = network;
		this.travelTime = travelTime;
	}

	/**
	 * Routes the carrierPlan in time and space.
	 * 
	 * @param carrierPlan
	 * @throws IllegalStateException if carrierPlan is null.
	 * @see TimeAndSpaceTourRouter
	 */
	@Override
	public void handlePlan(final CarrierPlan carrierPlan) {
		if(carrierPlan == null) throw new IllegalStateException("carrierPlan is null and cannot be handled.");
		handle(carrierPlan);
	}
	
	private void handle(final CarrierPlan carrierPlan) {
		
		Carrier carrier = carrierPlan.getCarrier() ;

		Builder builder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network) ;
		VehicleRoutingProblem problem = builder.build() ;
		
		VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
		
		Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
		
		VehicleRoutingProblemSolution solution = Solutions.getBest(solutions);
		
		CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution) ;

		carrierPlan.getScheduledTours().clear() ;
		for ( ScheduledTour tour : newPlan.getScheduledTours() ) {
			carrierPlan.getScheduledTours().add(tour) ;
		}
		carrierPlan.setScore(newPlan.getScore());
		
	}


}
