/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.julia.distribution;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import playground.vsp.emissions.EmissionModule;



public class ResponsibilityTravelDisutilityCalculatorFactory implements TravelDisutilityFactory{
	
	private final EmissionModule emissionModule;
	private final ResponsibilityCostModule rcm;
	
	public ResponsibilityTravelDisutilityCalculatorFactory(EmissionModule emissionModule, ResponsibilityCostModule rcm){
		this.emissionModule = emissionModule;
		this.rcm = rcm;
	}
	/*
	 * 

public class EmissionTravelDisutilityCalculatorFactory implements TravelDisutilityFactory {

	private final EmissionModule emissionModule;
	private final EmissionCostModule emissionCostModule;
	private Set<Id> hotspotLinks;

	public EmissionTravelDisutilityCalculatorFactory(EmissionModule emissionModule, EmissionCostModule emissionCostModule) {
		this.emissionModule = emissionModule;
		this.emissionCostModule = emissionCostModule;
	}

	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup){
		final EmissionTravelDisutilityCalculator etdc = new EmissionTravelDisutilityCalculator(timeCalculator, cnScoringGroup, emissionModule, emissionCostModule, hotspotLinks);

		return new TravelDisutility(){

			@Override
			public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				double linkTravelDisutility = etdc.getLinkTravelDisutility(link, time, person, vehicle);
				return linkTravelDisutility;
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return etdc.getLinkMinimumTravelDisutility(link);
			}
		};
	}

	public void setHotspotLinks(Set<Id> hotspotLinks) {
		this.hotspotLinks = hotspotLinks;
	}

}
	 */

	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
		final ResponsibilityTravelDisutilityCalculator rtdc = new ResponsibilityTravelDisutilityCalculator(timeCalculator, cnScoringGroup, emissionModule, rcm);
		
		return new TravelDisutility() {
			
			@Override
			public double getLinkTravelDisutility(Link link, double time,
					Person person, Vehicle vehicle) {
				return rtdc.getLinkTravelDisutility(link, time, person, vehicle);
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return rtdc.getMinimumTravelDisutility(link);
			}
		};
	}

}
