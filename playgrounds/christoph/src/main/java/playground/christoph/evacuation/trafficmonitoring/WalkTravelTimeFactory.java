/* *********************************************************************** *
 * project: org.matsim.*
 * WalkTravelTimeFactory.java
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

package playground.christoph.evacuation.trafficmonitoring;

import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TravelTimeFactory;

public class WalkTravelTimeFactory implements TravelTimeFactory {

	private final PlansCalcRouteConfigGroup plansCalcRouteConfigGroup;
	
	public WalkTravelTimeFactory(PlansCalcRouteConfigGroup plansCalcRouteConfigGroup) {
		this.plansCalcRouteConfigGroup = plansCalcRouteConfigGroup;
	}
	
	@Override
	public TravelTime createTravelTime() {
		return new WalkTravelTime(plansCalcRouteConfigGroup);
	}
	
}
