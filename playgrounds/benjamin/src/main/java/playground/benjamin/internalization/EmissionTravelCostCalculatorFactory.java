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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;

import playground.benjamin.emissions.EmissionModule;

/**
 * @author benjamin
 *
 */
public class EmissionTravelCostCalculatorFactory implements	TravelDisutilityFactory {

	private final EmissionModule emissionModule;

	public EmissionTravelCostCalculatorFactory(EmissionModule emissionModule) {
		this.emissionModule = emissionModule;
	}

	@Override
	public PersonalizableTravelDisutility createTravelDisutility(PersonalizableTravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup){
		final EmissionTravelCostCalculator etcc = new EmissionTravelCostCalculator(timeCalculator, cnScoringGroup, emissionModule);

		return new PersonalizableTravelDisutility(){

			@Override
			public void setPerson(Person person) {
				etcc.setPerson(person);
			}

			@Override
			public double getLinkTravelDisutility(Link link, double time) {
				double generalizedTravelCost = etcc.getLinkTravelDisutility(link, time);
				return generalizedTravelCost;
			}
		};
	}

}
