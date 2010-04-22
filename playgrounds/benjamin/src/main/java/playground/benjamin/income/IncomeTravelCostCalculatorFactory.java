/* *********************************************************************** *
 * project: org.matsim.*
 * Income1TravelCostCalculatorFactory
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
package playground.benjamin.income;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.roadpricing.RoadPricingScheme;


/**
 * @author dgrether
 *
 */
public class IncomeTravelCostCalculatorFactory implements TravelCostCalculatorFactory {

	private PersonHouseholdMapping personHouseholdMapping;
	
	private RoadPricingScheme scheme;

	public IncomeTravelCostCalculatorFactory(PersonHouseholdMapping personHouseholdMapping, RoadPricingScheme roadPricingScheme) {
		this.personHouseholdMapping = personHouseholdMapping;
		this.scheme = roadPricingScheme;
	}

	public PersonalizableTravelCost createTravelCostCalculator(TravelTime timeCalculator, CharyparNagelScoringConfigGroup cnScoringGroup) {
		final IncomeTravelCostCalculator incomeTravelCostCalculator = new IncomeTravelCostCalculator(timeCalculator, cnScoringGroup, personHouseholdMapping);
		final IncomeTollTravelCostCalculator incomeTollTravelCostCalculator = new IncomeTollTravelCostCalculator(personHouseholdMapping, scheme);
		
		return new PersonalizableTravelCost() {

			@Override
			public void setPerson(Person person) {
				incomeTravelCostCalculator.setPerson(person);
				incomeTollTravelCostCalculator.setPerson(person);
			}

			@Override
			public double getLinkTravelCost(Link link, double time) {
				double tollCost = incomeTollTravelCostCalculator.getLinkTravelCost(link, time);
				double otherCost = incomeTravelCostCalculator.getLinkTravelCost(link, time);
				return tollCost + otherCost;
			}
			
		};
	}

}
