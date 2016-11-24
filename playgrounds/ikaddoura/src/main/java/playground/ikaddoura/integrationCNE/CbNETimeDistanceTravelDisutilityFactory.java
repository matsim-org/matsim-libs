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
package playground.ikaddoura.integrationCNE;

import java.util.Set;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.vsp.airPollution.exposure.EmissionResponsibilityCostModule;
import playground.vsp.congestion.handlers.TollHandler;


/**
 * 
 * Accounts for Congestion, Noise and Exhaust Emissions.
 * 
 * @author ikaddoura
 *
 */
public final class CbNETimeDistanceTravelDisutilityFactory implements TravelDisutilityFactory {

	private double sigma = 0. ;
	private RandomizingTimeDistanceTravelDisutilityFactory randomizedTimeDistanceTravelDisutilityFactory;
	private final EmissionModule emissionModule;
	private final EmissionResponsibilityCostModule emissionCostModule;
	private final NoiseContext noiseContext;
	private final DecongestionInfo decongestionInfo;
	private final PlanCalcScoreConfigGroup cnScoringGroup;
	private Set<Id<Link>> hotspotLinks = null;

	public CbNETimeDistanceTravelDisutilityFactory(RandomizingTimeDistanceTravelDisutilityFactory randomizedTimeDistanceTravelDisutilityFactory, 
			EmissionModule emissionModule, EmissionResponsibilityCostModule emissionCostModule, NoiseContext noiseContext, DecongestionInfo decongestionInfo, PlanCalcScoreConfigGroup cnScoringGroup) {
		this.randomizedTimeDistanceTravelDisutilityFactory = randomizedTimeDistanceTravelDisutilityFactory;
		this.emissionModule = emissionModule;
		this.emissionCostModule = emissionCostModule;
		this.noiseContext = noiseContext;
		this.decongestionInfo = decongestionInfo;
		this.cnScoringGroup = cnScoringGroup;
	}

	@Override
	public final TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		
		randomizedTimeDistanceTravelDisutilityFactory.setSigma(sigma);
		
		return new CbNETollTimeDistanceTravelDisutility(
				randomizedTimeDistanceTravelDisutilityFactory.createTravelDisutility(timeCalculator),
				timeCalculator,
				this.emissionModule,
				this.emissionCostModule,
				this.noiseContext,
				this.decongestionInfo,
				cnScoringGroup.getMarginalUtilityOfMoney(),
				this.sigma,
				this.hotspotLinks
			);
	}
	
	public void setSigma ( double val ) {
		this.sigma = val;
	}
	
	public void setHotspotLinks(Set<Id<Link>> hotspotLinks) {
		this.hotspotLinks = hotspotLinks;
	}
}
