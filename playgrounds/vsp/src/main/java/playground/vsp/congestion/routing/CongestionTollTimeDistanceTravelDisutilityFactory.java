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
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

import playground.vsp.congestion.handlers.TollHandler;


/**
 * @author ikaddoura
 *
 */
public final class CongestionTollTimeDistanceTravelDisutilityFactory implements TravelDisutilityFactory {

	private double sigma = 0. ;
	private Builder timeDistanceTravelDisutilityFactory;
	private final TollHandler tollHandler;
	private final PlanCalcScoreConfigGroup cnScoringGroup;

	public CongestionTollTimeDistanceTravelDisutilityFactory(Builder timeDistanceTravelDisutilityFactory, 
			TollHandler tollHandler, PlanCalcScoreConfigGroup cnScoringGroup) {
		this.tollHandler = tollHandler;
		this.timeDistanceTravelDisutilityFactory = timeDistanceTravelDisutilityFactory;
		this.cnScoringGroup = cnScoringGroup;
	}

	@Override
	public final TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		
		timeDistanceTravelDisutilityFactory.setSigma(sigma);
		
		return new CongestionTollTimeDistanceTravelDisutility(
				timeDistanceTravelDisutilityFactory.createTravelDisutility(timeCalculator),
				this.tollHandler,
				cnScoringGroup.getMarginalUtilityOfMoney(),
				this.sigma
			);
	}
	
	public void setSigma ( double val ) {
		this.sigma = val;
	}
}
