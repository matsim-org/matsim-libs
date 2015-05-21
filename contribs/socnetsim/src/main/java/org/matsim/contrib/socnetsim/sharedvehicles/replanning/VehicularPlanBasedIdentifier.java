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
package org.matsim.contrib.socnetsim.sharedvehicles.replanning;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import org.matsim.contrib.socnetsim.sharedvehicles.SharedVehicleUtils;

import java.util.Collections;

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

