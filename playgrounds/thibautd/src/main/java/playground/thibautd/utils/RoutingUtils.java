/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.utils;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

/**
 * @author thibautd
 */
public class RoutingUtils {
	private RoutingUtils() {}

	public static List<PlanElement> tripsToLegs(
			final Plan plan,
			final StageActivityTypes stageActivities,
			final MainModeIdentifier mainModeIdentifier) {
		final List<Trip> trips = TripStructureUtils.getTrips( plan , stageActivities );

		final List<PlanElement> structure = new ArrayList<PlanElement>( plan.getPlanElements() );

		for (Trip trip : trips) {
			final int origin = structure.indexOf( trip.getOriginActivity() );
			final int destination = structure.indexOf( trip.getDestinationActivity() );
			final List<PlanElement> tripInPlan = structure.subList( origin + 1 , destination );

			tripInPlan.clear();
			tripInPlan.add(
					new LegImpl(
						mainModeIdentifier.identifyMainMode(
							trip.getTripElements() ) ) );
		}

		return structure;
	}
}

