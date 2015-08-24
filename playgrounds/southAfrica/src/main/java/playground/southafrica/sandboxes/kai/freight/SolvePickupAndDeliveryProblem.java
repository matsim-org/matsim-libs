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

package playground.southafrica.sandboxes.kai.freight;

import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.box.SchrimpfFactory;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.VehicleRoutingProblem.Builder;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.util.Solutions;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.router.TimeAndSpaceTourRouter;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;

/**
 * This is an attempt to construct a second strategy which solves the pick-and-delivery problem using jsprig
 * 
 * @author nagel
 *
 */
public class SolvePickupAndDeliveryProblem implements GenericPlanStrategyModule<CarrierPlan>{

	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(SolvePickupAndDeliveryProblem.class);
	
	private Network network;
	
	public SolvePickupAndDeliveryProblem(Network network) {
		super();
		this.network = network;
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

		// build, out of matsim data structures, a jsprit problem:
		Builder builder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network) ;
		VehicleRoutingProblem problem = builder.build() ;
		
		// define an algorithm to solve the jsprit problem:
		VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
		
		// get multiple jsprit solutions from the algorithm, and get the best one of those:
		VehicleRoutingProblemSolution solution = Solutions.bestOf(algorithm.searchSolutions());
		
		// convert the jsprit solutions back into the matsim data structure
		CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution) ;

		// copy the new CarrierPlan into the already existing pointer which was the original function call argument
		// (this is more messy than it should be because of the way it is programmed on the matsim side) 
		carrierPlan.getScheduledTours().clear() ;
		for ( ScheduledTour tour : newPlan.getScheduledTours() ) {
			carrierPlan.getScheduledTours().add(tour) ;
		}
		carrierPlan.setScore(newPlan.getScore());
		
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
	}

	@Override
	public void finishReplanning() {
	}


}
