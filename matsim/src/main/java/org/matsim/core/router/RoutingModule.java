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

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.Facility;

/**
 * Defines classes responsible for routing for a given
 * (main)-mode.
 * It does not modify the plan.  
 * <p/>
 * See {@link tutorial.programming.example12PluggableTripRouter.RunPluggableTripRouterExample} for an example how to use this interface.
 * 
 * @author thibautd
 */
public interface RoutingModule {
	/**
	 * Computes a route, as a sequence of plan elements. The plan elements can
	 * be only legs, or a sequence of legs and "dummy" activities. All activity
	 * types inserted as dummy activities must be notified by the
	 * {@link #getStageActivityTypes()} method.
	 * <br>
	 * <b>important:</b> if route computation relies on a shortest path algorithm
	 * using {@link TravelTime} and/or {@link TravelDisutility}
	 * estimators, this method is responsible for setting the person to the argument
	 * person in those estimators before running the shortest path algorithm.
	 *
	 * @param fromFacility a {@link Facility} representing the departure location
	 * @param toFacility a {@link Facility} representing the arrival location
	 * @param departureTime the departure time
	 * @param person the {@link Person} to route
	 * @return a list of {@link PlanElement}, in proper order, representing the trip.
	 */
	public List<? extends PlanElement> calcRoute(
			Facility<?> fromFacility,
			Facility<?> toFacility,
			double departureTime,
			Person person);

	/**
	 * Gives access to the activity types to consider as stages.
	 * </ul>
	 * @return a non-null {@link StageActivityTypes}. This should always return
	 * the same instance, or at least return instances which are equal for equals().
	 * Otherwise, replacement of modules in the {@link TripRouter} will not work.
	 */
	public StageActivityTypes getStageActivityTypes();
}

