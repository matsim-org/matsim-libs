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

	private final TravelDisutilityFactory travelDisutilityFactory;
	private final TravelTime travelTime;
	private final PlanCalcScoreConfigGroup cnScoringGroup;
	
	public RoutingContextImpl(TravelDisutilityFactory travelDisutilityFactory, TravelTime travelTime,
			PlanCalcScoreConfigGroup cnScoringGroup) {
		this.travelDisutilityFactory = travelDisutilityFactory;
		this.travelTime = travelTime;
		this.cnScoringGroup = cnScoringGroup;
	}
	
	@Override
	public TravelDisutility getTravelDisutility() {
		return this.travelDisutilityFactory.createTravelDisutility(travelTime, cnScoringGroup);
	}

	@Override
	public TravelTime getTravelTime() {
		return this.travelTime;
	}
}