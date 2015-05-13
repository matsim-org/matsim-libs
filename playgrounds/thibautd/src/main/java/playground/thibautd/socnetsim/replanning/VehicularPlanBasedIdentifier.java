/* *********************************************************************** *
 * project: org.matsim.*
 * VehicularPlanBasedIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import playground.thibautd.socnetsim.sharedvehicles.SharedVehicleUtils;

/**
 * @author thibautd
 */
public final class VehicularPlanBasedIdentifier implements PlanLinkIdentifier {
	@Override
	public boolean areLinked(
			final Plan p1,
			final Plan p2) {
		return !Collections.disjoint(
			SharedVehicleUtils.getVehiclesInPlan( p1 , SharedVehicleUtils.DEFAULT_VEHICULAR_MODES ) ,
			SharedVehicleUtils.getVehiclesInPlan( p2 , SharedVehicleUtils.DEFAULT_VEHICULAR_MODES ) );
	}
}

