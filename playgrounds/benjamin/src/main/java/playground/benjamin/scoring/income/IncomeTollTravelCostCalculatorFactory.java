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
import org.matsim.core.config.Config;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.vehicles.Vehicle;


/**
 * @author bkick after dgrether
 *
 */
public class IncomeTollTravelCostCalculatorFactory implements TravelDisutilityFactory {

	private PersonHouseholdMapping personHouseholdMapping;
	
	private RoadPricingScheme scheme;

	private final Config config;

	public IncomeTollTravelCostCalculatorFactory(PersonHouseholdMapping personHouseholdMapping, RoadPricingScheme roadPricingScheme, Config config) {
		this.personHouseholdMapping = personHouseholdMapping;
		this.scheme = roadPricingScheme;
		Logger.getLogger(this.getClass()).warn("Unfortunately, I think that with this setup, toll is added twice to the router:\n" +
				"                     * Once in this class (income-dependent).\n" +
				"                     * Once by the standard roadpricing setup (using a utility of money of one).\n" +
				"                     I may be wrong; please let me know if you ever find out (one way or the other. kai, mar'12") ;
		this.config = config ;
	}
	
	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		final IncomeTravelCostCalculator incomeTravelCostCalculator = new IncomeTravelCostCalculator(timeCalculator, config.planCalcScore(), personHouseholdMapping);
		final IncomeTollTravelCostCalculator incomeTollTravelCostCalculator = new IncomeTollTravelCostCalculator(personHouseholdMapping, scheme, config);
		
		return new TravelDisutility() {

			//somehow summing up the income related generalized travel costs and the income related toll costs for the router...
			//remark: this method should be named "getLinkGeneralizedTravelCosts" or "getLinkDisutilityFromTraveling"
			@Override
			public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				double generalizedTravelCost = incomeTravelCostCalculator.getLinkTravelDisutility(link, time, person, vehicle);
				double additionalGeneralizedTollCost = incomeTollTravelCostCalculator.getLinkTravelDisutility(link, time, person, vehicle);
				return generalizedTravelCost + additionalGeneralizedTollCost;
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return incomeTollTravelCostCalculator.getLinkMinimumTravelDisutility(link);
			}
			
		};
	}

}
