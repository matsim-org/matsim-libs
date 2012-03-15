/* *********************************************************************** *
 * project: org.matsim.*
 * BKickLegScoring
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
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.households.Income;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.TollTravelCostCalculator;

/**
 * @author bkick
 * @author michaz
 * 
 */

// a dummy class in order to meet the framework
class GautengTollTravelCostCalculator implements PersonalizableTravelCost {

	class NullTravelCostCalculator implements PersonalizableTravelCost {

		@Override
		public void setPerson(Person person) {
			
		}

		@Override
		public double getLinkGeneralizedTravelCost(Link link, double time) {
			return 0;
		}

	}

	private TollTravelCostCalculator tollTravelCostCalculator;
	
	public GautengTollTravelCostCalculator(RoadPricingScheme scheme) {
		this.tollTravelCostCalculator = new TollTravelCostCalculator(new NullTravelCostCalculator(), scheme);
	}

	@Override
	public void setPerson(Person person) {
	}

	//calculating additional generalized toll costs
	@Override
	public double getLinkGeneralizedTravelCost(Link link, double time) {
		double amount = tollTravelCostCalculator.getLinkGeneralizedTravelCost(link, time);
		int betaIncomeCar = 0;
		int incomePerDay = 0;
		double additionalGeneralizedTollCost = (betaIncomeCar / incomePerDay) * amount;
		return additionalGeneralizedTollCost;
	}

}
