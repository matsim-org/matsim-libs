/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingContextImpl.java
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

package org.matsim.core.router;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class RoutingContextImpl implements RoutingContext {

	private final TravelDisutility travelDisutility;
	private final TravelTime travelTime;

	public RoutingContextImpl(TravelDisutility travelDisutility, TravelTime travelTime) {
		this.travelDisutility = travelDisutility;
		this.travelTime = travelTime;
	}

	@Deprecated
	public RoutingContextImpl(TravelDisutilityFactory travelDisutilityFactory, TravelTime travelTime,
			PlanCalcScoreConfigGroup cnScoringGroup) {
		this.travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime, cnScoringGroup);
		this.travelTime = travelTime;
	}
	
	@Override
	public TravelDisutility getTravelDisutility() {
		return this.travelDisutility;
	}

	@Override
	public TravelTime getTravelTime() {
		return this.travelTime;
	}
}