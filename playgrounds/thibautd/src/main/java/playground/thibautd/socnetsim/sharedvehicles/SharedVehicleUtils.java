/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleUtils.java
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
package playground.thibautd.socnetsim.sharedvehicles;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;

import playground.thibautd.socnetsim.population.JointPlan;

/**
 * @author thibautd
 */
public final class SharedVehicleUtils {
	private SharedVehicleUtils() {}

	public static Set<Id> getVehiclesInJointPlan(
			final JointPlan jointPlan,
			final String legMode) {
		final Set<Id> vehs = new HashSet<Id>();
		for ( Plan p : jointPlan.getIndividualPlans().values() ) {
			vehs.addAll( getVehiclesInPlan( p , legMode ) );
		}
		return vehs;
	}

	public static Set<Id> getVehiclesInPlan(
			final Plan plan,
			final String legMode) {
		final Set<Id> vehs = new HashSet<Id>();
		for ( PlanElement pe : plan.getPlanElements() ) {
			if ( !(pe instanceof Leg) ) continue;
			final Leg l = (Leg) pe;
			if ( !l.getMode().equals( legMode ) ) continue;
			if ( !(l.getRoute() instanceof NetworkRoute) ) continue;
			vehs.add( ((NetworkRoute) l.getRoute()).getVehicleId() );
		}
		return vehs;
	}
}

