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
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.benjamin.internalization.EmissionCostModule;
import playground.ikaddoura.router.VTTSTimeDistanceTravelDisutilityFactory;
import playground.vsp.congestion.handlers.TollHandler;


/**
 * 
 *  Accounts for Congestion, Noise and Exhaust Emissions
 *  and agent- and trip-specific VTTS.
 * 
 * @author ikaddoura
 *
 */
public final class VttsCNETimeDistanceTravelDisutilityFactory implements TravelDisutilityFactory {

	private double sigma = 0. ;
	private Set<Id<Link>> hotspotLinks = null;
	private VTTSTimeDistanceTravelDisutilityFactory vttsTimeDistanceTravelDisutilityFactory;
	private final EmissionModule emissionModule;
	private final EmissionCostModule emissionCostModule;
	private final NoiseContext noiseContext;
	private final TollHandler tollHandler;
	private final PlanCalcScoreConfigGroup cnScoringGroup;

	public VttsCNETimeDistanceTravelDisutilityFactory(VTTSTimeDistanceTravelDisutilityFactory vttsTimeDistanceTravelDisutilityFactory, 
			EmissionModule emissionModule, EmissionCostModule emissionCostModule, NoiseContext noiseContext, TollHandler tollHandler, PlanCalcScoreConfigGroup cnScoringGroup) {
		this.vttsTimeDistanceTravelDisutilityFactory = vttsTimeDistanceTravelDisutilityFactory;
		this.emissionModule = emissionModule;
		this.emissionCostModule = emissionCostModule;
		this.noiseContext = noiseContext;
		this.tollHandler = tollHandler;
		this.cnScoringGroup = cnScoringGroup;
	}

	@Override
	public final TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		
		vttsTimeDistanceTravelDisutilityFactory.setSigma(sigma);
		
		return new CNETollTimeDistanceTravelDisutility(
				vttsTimeDistanceTravelDisutilityFactory.createTravelDisutility(timeCalculator),
				timeCalculator,
				this.emissionModule,
				this.emissionCostModule,
				this.noiseContext,
				this.tollHandler,
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
