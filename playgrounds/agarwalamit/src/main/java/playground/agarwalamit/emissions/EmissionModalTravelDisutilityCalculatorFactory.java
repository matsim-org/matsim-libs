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
package playground.agarwalamit.emissions;

import java.util.Set;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicles;
import playground.vsp.airPollution.flatEmissions.EmissionCostModule;


/**
 * @author benjamin
 *
 */
public class EmissionModalTravelDisutilityCalculatorFactory implements TravelDisutilityFactory {

	@Inject  private EmissionModule emissionModule;
	@Inject  private EmissionCostModule emissionCostModule;
	private Set<Id<Link>> hotspotLinks;
	@Inject private PlanCalcScoreConfigGroup cnScoringGroup;
	@Inject
	private Vehicles vehicles;

	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator){
		return new EmissionModalTravelDisutilityCalculator(timeCalculator, cnScoringGroup, emissionModule, emissionCostModule, hotspotLinks, vehicles);
	}

	public void setHotspotLinks(Set<Id<Link>> hotspotLinks) {
		this.hotspotLinks = hotspotLinks;
	}

}
