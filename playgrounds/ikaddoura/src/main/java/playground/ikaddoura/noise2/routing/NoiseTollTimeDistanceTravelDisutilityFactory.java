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
package playground.ikaddoura.noise2.routing;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.ikaddoura.noise2.data.NoiseContext;


/**
 * @author ikaddoura
 *
 */
public final class NoiseTollTimeDistanceTravelDisutilityFactory implements TravelDisutilityFactory {

	private double sigma = 0. ;
	private Builder randomizedTimeDistanceTravelDisutilityFactory;
	private final NoiseContext noiseContext;

	public NoiseTollTimeDistanceTravelDisutilityFactory(Builder randomizedTimeDistanceTravelDisutilityFactory, NoiseContext noiseContext) {
		this.noiseContext = noiseContext;
		this.randomizedTimeDistanceTravelDisutilityFactory = randomizedTimeDistanceTravelDisutilityFactory;
	}

	@Override
	public final TravelDisutility createTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
		
		randomizedTimeDistanceTravelDisutilityFactory.setSigma(sigma);
		
		return new NoiseTollTimeDistanceTravelDisutility(
				randomizedTimeDistanceTravelDisutilityFactory.createTravelDisutility(timeCalculator, cnScoringGroup),
				this.noiseContext,
				cnScoringGroup.getMarginalUtilityOfMoney(),
				this.sigma
			);
	}
	
	public void setSigma ( double val ) {
		this.sigma = val;
	}
}
