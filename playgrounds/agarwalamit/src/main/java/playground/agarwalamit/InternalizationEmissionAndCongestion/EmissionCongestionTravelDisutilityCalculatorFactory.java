/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.InternalizationEmissionAndCongestion;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import playground.benjamin.internalization.EmissionCostModule;
import playground.vsp.congestion.handlers.TollHandler;

/**
 * @author amit after Benjamin and Ihab
 */
public class EmissionCongestionTravelDisutilityCalculatorFactory implements TravelDisutilityFactory {

	private final EmissionModule emissionModule;
	private final EmissionCostModule emissionCostModule;
	private Set<Id<Link>> hotspotLinks;
	private TollHandler tollHandler;
	private final PlanCalcScoreConfigGroup cnScoringGroup;
	
	public EmissionCongestionTravelDisutilityCalculatorFactory(EmissionModule emissionModule, EmissionCostModule emissionCostModule, 
			TollHandler tollHandler, PlanCalcScoreConfigGroup cnScoringGroup) {
		this.emissionModule = emissionModule;
		this.emissionCostModule = emissionCostModule;
		this.tollHandler = tollHandler;
		this.cnScoringGroup = cnScoringGroup;
	}

	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator){
		final EmissionCongestionTravelDisutilityCalculator ectdc = new EmissionCongestionTravelDisutilityCalculator(timeCalculator, cnScoringGroup, this.emissionModule, this.emissionCostModule, this.hotspotLinks, this.tollHandler);

		return new TravelDisutility(){

			@Override
			public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				double linkTravelDisutility = ectdc.getLinkTravelDisutility(link, time, person, vehicle);
				return linkTravelDisutility;
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return ectdc.getLinkMinimumTravelDisutility(link);
			}
		};
	}

	public void setHotspotLinks(Set<Id<Link>> hotspotLinks) {
		this.hotspotLinks = hotspotLinks;
	}

}
