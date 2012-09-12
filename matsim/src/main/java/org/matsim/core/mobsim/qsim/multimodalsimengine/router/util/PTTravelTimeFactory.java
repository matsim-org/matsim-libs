/* *********************************************************************** *
 * project: org.matsim.*
 * PTTravelTimeFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.multimodalsimengine.router.util;

import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TravelTimeFactory;

public class PTTravelTimeFactory implements TravelTimeFactory {

	private final PlansCalcRouteConfigGroup plansCalcRouteConfigGroup;
	private final TravelTime travelTime;	// PT speed does not depend on a passenger, therefore not personalizable
	private final TravelTime walkTravelTimeFactory;
	
	public PTTravelTimeFactory(PlansCalcRouteConfigGroup plansCalcRouteConfigGroup,
			TravelTime travelTime, TravelTime walkTravelTimeFactory2) {
		this.plansCalcRouteConfigGroup = plansCalcRouteConfigGroup;
		this.travelTime = travelTime;
		this.walkTravelTimeFactory = walkTravelTimeFactory2;
	}
	
	@Override
	public TravelTime createTravelTime() {
		return new PTTravelTime(plansCalcRouteConfigGroup, travelTime, walkTravelTimeFactory);
	}
	
}
