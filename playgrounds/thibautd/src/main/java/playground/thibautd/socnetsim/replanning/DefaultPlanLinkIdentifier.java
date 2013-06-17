/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.thibautd.socnetsim.replanning;

import java.util.Collections;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.PassengerRoute;
import playground.thibautd.socnetsim.replanning.modules.RecomposeJointPlanAlgorithm.PlanLinkIdentifier;
import playground.thibautd.socnetsim.sharedvehicles.SharedVehicleUtils;

// /////////////////////////////////////////////////////////////////////////
// helpers
// /////////////////////////////////////////////////////////////////////////
public class DefaultPlanLinkIdentifier implements PlanLinkIdentifier {
	@Override
	public boolean areLinked(
			final Plan p1,
			final Plan p2) {
		return areLinkedByJointTrips( p1 , p2 ) || areLinkedByVehicles( p1 , p2 );
	}

	private static boolean areLinkedByVehicles(
			final Plan p1,
			final Plan p2) {
		return !Collections.disjoint(
			SharedVehicleUtils.getVehiclesInPlan( p1 , SharedVehicleUtils.DEFAULT_VEHICULAR_MODES ) ,
			SharedVehicleUtils.getVehiclesInPlan( p2 , SharedVehicleUtils.DEFAULT_VEHICULAR_MODES ) );
	}

	private static boolean areLinkedByJointTrips(
			final Plan p1,
			final Plan p2) {
		final boolean areLinked = containsCoTraveler( p1 , p2.getPerson().getId() );
		assert areLinked == containsCoTraveler( p2 , p1.getPerson().getId() ) :
			"inconsistent plans "+p1+" "+(areLinked ? "contains " : "does not contains ")+p2.getPerson().getId()+
			" and "+p2+" "+(!areLinked ? "contains " : "does not contains ")+p1.getPerson().getId();
		return areLinked;
	}

	private static boolean containsCoTraveler(
			final Plan plan,
			final Id cotraveler) {
		for ( Trip t : TripStructureUtils.getTrips( plan , EmptyStageActivityTypes.INSTANCE ) ) {
			for ( Leg l : t.getLegsOnly() ) {
				if ( l.getRoute() instanceof DriverRoute ) {
					if ( ((DriverRoute) l.getRoute()).getPassengersIds().contains( cotraveler ) ) {
						return true;
					}
				}
				else if ( l.getRoute() instanceof PassengerRoute ) {
					if ( ((PassengerRoute) l.getRoute()).getDriverId().equals( cotraveler ) ) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
