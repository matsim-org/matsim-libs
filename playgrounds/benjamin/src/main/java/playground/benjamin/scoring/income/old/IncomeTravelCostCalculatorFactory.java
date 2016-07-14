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
package playground.benjamin.scoring.income.old;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.vehicles.Vehicle;


/**
 * @author bkick after dgrether
 *
 */
public class IncomeTravelCostCalculatorFactory implements TravelDisutilityFactory {

	private PersonHouseholdMapping personHouseholdMapping;
	private final PlanCalcScoreConfigGroup cnScoringGroup;

	public IncomeTravelCostCalculatorFactory(PersonHouseholdMapping personHouseholdMapping, PlanCalcScoreConfigGroup cnScoringGroup) {
		this.personHouseholdMapping = personHouseholdMapping;
		this.cnScoringGroup = cnScoringGroup;
	}
	
	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		final IncomeTravelCostCalculator incomeTravelCostCalculator = new IncomeTravelCostCalculator(timeCalculator, cnScoringGroup, personHouseholdMapping);
		
		return new TravelDisutility() {

			@Override
			public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				double generalizedTravelCost = incomeTravelCostCalculator.getLinkTravelDisutility(link, time, person, vehicle);
				return generalizedTravelCost;
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return incomeTravelCostCalculator.getLinkMinimumTravelDisutility(link);
			}
			
		};
	}

}
