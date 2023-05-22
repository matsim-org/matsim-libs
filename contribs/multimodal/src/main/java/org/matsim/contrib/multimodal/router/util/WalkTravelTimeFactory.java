/* *********************************************************************** *
 * project: org.matsim.*
 * WalkTravelTimeFactory.java
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

package org.matsim.contrib.multimodal.router.util;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.util.TravelTime;

import jakarta.inject.Provider;
import java.util.Map;

public class WalkTravelTimeFactory implements Provider<TravelTime> {

	private final PlansCalcRouteConfigGroup plansCalcRouteConfigGroup;
	private final Map<Id<Link>, Double> linkSlopes;	// slope information in %

	public WalkTravelTimeFactory(PlansCalcRouteConfigGroup plansCalcRouteConfigGroup) {
		this(plansCalcRouteConfigGroup, null);
	}

	public WalkTravelTimeFactory(PlansCalcRouteConfigGroup plansCalcRouteConfigGroup,
			Map<Id<Link>, Double> linkSlopes) {
		this.plansCalcRouteConfigGroup = plansCalcRouteConfigGroup;
		this.linkSlopes = linkSlopes;

		if (plansCalcRouteConfigGroup.getTeleportedModeSpeeds().get(TransportMode.walk) == null) {
			throw new RuntimeException("No speed was found for mode walk! Aborting.");
		}
	}

	@Override
	public TravelTime get() {
		return new WalkTravelTime(this.plansCalcRouteConfigGroup, this.linkSlopes);
	}

}
