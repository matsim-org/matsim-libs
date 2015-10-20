/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultTravelCostCalculatorFactoryImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.vsp.congestion.routing;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.vsp.congestion.handlers.TollHandler;


/**
 * @author ikaddoura
 *
 */
public final class CongestionTollTimeDistanceTravelDisutilityFactory implements TravelDisutilityFactory {

	private double sigma = 0. ;
	private TravelTimeAndDistanceBasedTravelDisutilityFactory randomizedTimeDistanceTravelDisutilityFactory;
	private final TollHandler tollHandler;

	public CongestionTollTimeDistanceTravelDisutilityFactory(TravelTimeAndDistanceBasedTravelDisutilityFactory randomizedTimeDistanceTravelDisutilityFactory, TollHandler tollHandler) {
		this.tollHandler = tollHandler;
		this.randomizedTimeDistanceTravelDisutilityFactory = randomizedTimeDistanceTravelDisutilityFactory;
	}

	@Override
	public final TravelDisutility createTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
		
		randomizedTimeDistanceTravelDisutilityFactory.setSigma(sigma);
		
		return new CongestionTollTimeDistanceTravelDisutility(
				randomizedTimeDistanceTravelDisutilityFactory.createTravelDisutility(timeCalculator, cnScoringGroup),
				this.tollHandler,
				cnScoringGroup.getMarginalUtilityOfMoney(),
				this.sigma
			);
	}
	
	public void setSigma ( double val ) {
		this.sigma = val;
	}
}
