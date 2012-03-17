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
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.households.Income;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.TravelDisutilityIncludingToll;

/**
 * The regular TollTravelCostCalculator assumes that generalized travel cost is already converted into money terms,
 * and adds toll payments without additional conversion.  This is incomplete at two fronts:<ul>
 * <li> Matsim now "thinks" in terms of utils, not monetary units.
 * <li> The relation between money and time may vary from agent to agent or between agent classes (often "value of time"
 * or "value of travel time savings", in matsim now in fact "utility of money" = person-dependent conversion factor of money
 * into utility)
 * </ul>
 * For those reasons the addition of toll to travel disutility needs to be redone her and in the factory. 
 * 
 * @author kn after
 * @author bkick after
 * @author michaz
 */
class GautengLinkTollCalculator {

	// a dummy class in order to meet the framework
	class NullTravelCostCalculator implements PersonalizableTravelDisutility {

		@Override
		public void setPerson(Person person) {
			
		}

		@Override
		public double getLinkTravelDisutility(Link link, double time) {
			return 0;
		}

	}

	private TravelDisutilityIncludingToll tollTravelCostCalculator;
	
	GautengLinkTollCalculator(RoadPricingScheme scheme) {
		this.tollTravelCostCalculator = new TravelDisutilityIncludingToll(new NullTravelCostCalculator(), scheme);
		// (because of the NullTravelCostCalculator, this will return the monetary amount of the toll for the link)
	}

	void setPerson(Person person) {
	}

	double getLinkTollForCars(Link link, double time) {

		return tollTravelCostCalculator.getLinkTravelDisutility(link, time);
		// yyyyyy despite what this seems, it is most probably not possible to construct a toll scheme where
		// toll rates vary by vehicle type.  Reason is that the scoring will use a different toll calculator class, unaffected
		// by the changes here.  kai, mar'12
		
	}

}
