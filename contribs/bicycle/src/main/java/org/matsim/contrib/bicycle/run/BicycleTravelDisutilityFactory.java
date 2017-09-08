/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.run;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

/**
 * @author smetzler, dziemke
 */
public class BicycleTravelDisutilityFactory implements TravelDisutilityFactory {

	@Inject	BicycleConfigGroup bicycleConfigGroup;
	@Inject	PlanCalcScoreConfigGroup cnScoringGroup;
	@Inject	PlansCalcRouteConfigGroup plansCalcRouteConfigGroup;
	@Inject Network network; // TODO only needed as long as network mode filtering kicks out attributes; remove when possible, dz, sep'17
	
	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		return new BicycleTravelDisutility(bicycleConfigGroup, cnScoringGroup, plansCalcRouteConfigGroup, timeCalculator, network);
	}
}