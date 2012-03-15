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
package playground.kai.gauteng.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.roadpricing.RoadPricingScheme;

/**
 * @author bkick after dgrether
 *
 */
public class GautengTollTravelCostCalculatorFactory implements TravelCostCalculatorFactory {

	private RoadPricingScheme scheme;

	public GautengTollTravelCostCalculatorFactory(RoadPricingScheme roadPricingScheme) {
		this.scheme = roadPricingScheme;
	}
	
	public PersonalizableTravelCost createTravelCostCalculator(PersonalizableTravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
		final GautengTravelCostCalculator incomeTravelCostCalculator = new GautengTravelCostCalculator(timeCalculator, cnScoringGroup);
		final GautengTollTravelCostCalculator incomeTollTravelCostCalculator = new GautengTollTravelCostCalculator(scheme);
		
		return new PersonalizableTravelCost() {

			@Override
			public void setPerson(Person person) {
				incomeTravelCostCalculator.setPerson(person);
				incomeTollTravelCostCalculator.setPerson(person);
			}

			//somehow summing up the income related generalized travel costs and the income related toll costs for the router...
			//remark: this method should be named "getLinkGeneralizedTravelCosts" or "getLinkDisutilityFromTraveling"
			@Override
			public double getLinkGeneralizedTravelCost(Link link, double time) {
				double generalizedTravelCost = incomeTravelCostCalculator.getLinkGeneralizedTravelCost(link, time);
				double additionalGeneralizedTollCost = incomeTollTravelCostCalculator.getLinkGeneralizedTravelCost(link, time);
				return generalizedTravelCost + additionalGeneralizedTollCost;
			}
			
		};
	}

}
