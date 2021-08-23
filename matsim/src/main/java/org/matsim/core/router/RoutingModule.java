/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingModule.java
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
package org.matsim.core.router;

import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * Defines classes responsible for routing for a given
 * (main)-mode.
 * It does not modify the plan.  
 *
 * @author thibautd
 */
public interface RoutingModule {
	/**
	 * Computes a route, as a sequence of plan elements. The plan elements can
	 * be only legs, or a sequence of legs and "dummy" activities. All activity
	 * types inserted as dummy activities must have a type which ends on
	 * "interaction".
	 * <br>
	 * <b>important:</b> if route computation relies on a shortest path algorithm
	 * using {@link TravelTime} and/or {@link TravelDisutility}
	 * estimators, this method is responsible for setting the person to the argument
	 * person in those estimators before running the shortest path algorithm.
	 * 
	 * The method parameters prior to MATSim 14 have been collected in a RoutingRequest object. 
	 * To retrofit older code, use DefaultRoutingRequest.of(...) to wrap your method arguments.
	 *
	 * @param request a {@link RoutingRequest} represents origin, destination, departure time, etc.
	 * @return a list of {@link PlanElement}, in proper order, representing the trip.
	 */
	public List<? extends PlanElement> calcRoute(RoutingRequest request);
	
	// NOTE: It makes some sense to _not_ have the vehicle as an argument here ... since that only makes sense for vehicular modes. kai, feb'19
	// NOTE: But now we have replaced the arguments with the RoutingRequest interface, which could now have a derived VehicularRoutingRequest if needed. shoerl, aug'21
}

