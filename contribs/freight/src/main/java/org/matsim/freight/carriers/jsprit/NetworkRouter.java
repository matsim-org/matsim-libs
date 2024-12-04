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
package org.matsim.freight.carriers.jsprit;

import org.matsim.freight.carriers.CarrierPlan;
import org.matsim.freight.carriers.ScheduledTour;
import org.matsim.freight.carriers.controller.CarrierTimeAndSpaceTourRouter;

/**
 * Router that routes {@link CarrierPlan}.
 *
 * @author stefan schröder
 *
 */
public class NetworkRouter {

	/**
	 * Routes the {@link CarrierPlan} with the router defined in {@link NetworkBasedTransportCosts}.
	 *
	 * <p>Note that this changes the plan, i.e. it adds routes to the input-plan.
	 *
	 * @param {@link CarrierPlan}
	 * @param {@link NetworkBasedTransportCosts}
	 */
	public static void routePlan(CarrierPlan plan, VRPTransportCosts freightTransportCosts){
		if( plan == null) throw new IllegalStateException("plan is missing.");
		for( ScheduledTour tour : plan.getScheduledTours()){
			new CarrierTimeAndSpaceTourRouter( freightTransportCosts.getRouter(), freightTransportCosts.getNetwork(), freightTransportCosts.getTravelTime()).route(tour );
		}
	}

}
