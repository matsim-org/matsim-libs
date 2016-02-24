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
package playground.ikaddoura.integrationCN;

import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.routing.NoiseTollTimeDistanceTravelDisutility;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.ikaddoura.router.VTTSTimeDistanceTravelDisutilityFactory;


/**
 * @author ikaddoura
 *
 */
public final class VTTSNoiseTollTimeDistanceTravelDisutilityFactory implements TravelDisutilityFactory {

	private double sigma = 0. ;
	private VTTSTimeDistanceTravelDisutilityFactory vttsTimeDistanceTravelDisutilityFactory;
	private final NoiseContext noiseContext;
	private final PlanCalcScoreConfigGroup cnScoringGroup;

	public VTTSNoiseTollTimeDistanceTravelDisutilityFactory(VTTSTimeDistanceTravelDisutilityFactory vttsTimeDistanceTravelDisutilityFactory, 
			NoiseContext noiseContext, PlanCalcScoreConfigGroup cnScoringGroup) {
		this.noiseContext = noiseContext;
		this.vttsTimeDistanceTravelDisutilityFactory = vttsTimeDistanceTravelDisutilityFactory;
		this.cnScoringGroup = cnScoringGroup;
	}

	@Override
	public final TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		
		vttsTimeDistanceTravelDisutilityFactory.setSigma(sigma);
		
		return new NoiseTollTimeDistanceTravelDisutility(
				vttsTimeDistanceTravelDisutilityFactory.createTravelDisutility(timeCalculator),
				this.noiseContext,
				cnScoringGroup.getMarginalUtilityOfMoney(),
				this.sigma
			);
	}
	
	public void setSigma ( double val ) {
		this.sigma = val;
	}
}
