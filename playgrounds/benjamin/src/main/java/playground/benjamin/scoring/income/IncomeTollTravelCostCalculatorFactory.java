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
package playground.benjamin.scoring.income;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.benjamin.scoring.income.IncomeTollTravelCostCalculator;
import playground.benjamin.scoring.income.IncomeTravelCostCalculator;


/**
 * @author bkick after dgrether
 *
 */
public class IncomeTollTravelCostCalculatorFactory implements TravelDisutilityFactory {

	private PersonHouseholdMapping personHouseholdMapping;
	
	private RoadPricingScheme scheme;

	public IncomeTollTravelCostCalculatorFactory(PersonHouseholdMapping personHouseholdMapping, RoadPricingScheme roadPricingScheme) {
		this.personHouseholdMapping = personHouseholdMapping;
		this.scheme = roadPricingScheme;
		Logger.getLogger(this.getClass()).warn("Unfortunately, I think that with this setup, toll is added twice to the router:\n" +
				"                     * Once in this class (income-dependent).\n" +
				"                     * Once by the standard roadpricing setup (using a utility of money of one).\n" +
				"                     I may be wrong; please let me know if you ever find out (one way or the other. kai, mar'12") ;
	}
	
	public PersonalizableTravelDisutility createTravelDisutility(PersonalizableTravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
		final IncomeTravelCostCalculator incomeTravelCostCalculator = new IncomeTravelCostCalculator(timeCalculator, cnScoringGroup, personHouseholdMapping);
		final IncomeTollTravelCostCalculator incomeTollTravelCostCalculator = new IncomeTollTravelCostCalculator(personHouseholdMapping, scheme);
		
		return new PersonalizableTravelDisutility() {

			@Override
			public void setPerson(Person person) {
				incomeTravelCostCalculator.setPerson(person);
				incomeTollTravelCostCalculator.setPerson(person);
			}

			//somehow summing up the income related generalized travel costs and the income related toll costs for the router...
			//remark: this method should be named "getLinkGeneralizedTravelCosts" or "getLinkDisutilityFromTraveling"
			@Override
			public double getLinkTravelDisutility(Link link, double time) {
				double generalizedTravelCost = incomeTravelCostCalculator.getLinkTravelDisutility(link, time);
				double additionalGeneralizedTollCost = incomeTollTravelCostCalculator.getLinkTravelDisutility(link, time);
				return generalizedTravelCost + additionalGeneralizedTollCost;
			}
			
		};
	}

}
