/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionTravelCostCalculatorFactory.java
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
package playground.benjamin.internalization;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;


/**
 * @author benjamin
 *
 */
public class EmissionTravelDisutilityCalculatorFactory implements TravelDisutilityFactory {

	private final EmissionModule emissionModule;
	private final EmissionCostModule emissionCostModule;
	private Set<Id<Link>> hotspotLinks;
	private final PlanCalcScoreConfigGroup cnScoringGroup;

	public EmissionTravelDisutilityCalculatorFactory(EmissionModule emissionModule, EmissionCostModule emissionCostModule,
			PlanCalcScoreConfigGroup cnScoringGroup) {
		this.emissionModule = emissionModule;
		this.emissionCostModule = emissionCostModule;
		this.cnScoringGroup = cnScoringGroup;
	}

	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator){
		return new EmissionTravelDisutilityCalculator(timeCalculator, cnScoringGroup, emissionModule, emissionCostModule, hotspotLinks);
	}

	public void setHotspotLinks(Set<Id<Link>> hotspotLinks) {
		this.hotspotLinks = hotspotLinks;
	}

}
