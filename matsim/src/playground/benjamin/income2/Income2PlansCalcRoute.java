/* *********************************************************************** *
 * project: org.matsim.*
 * IncomePlansCalcRoute
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
package playground.benjamin.income2;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.households.PersonHouseholdMapping;


/**
 * @author dgrether
 *
 */
public class Income2PlansCalcRoute extends PlansCalcRoute{

	
	private BKickIncome2TravelTimeDistanceCostCalculator incomeCostCalculator;
	private PersonHouseholdMapping hhdb;

	/**
	 * Uses the speed factors from the config group and the rerouting of the factory 
	 * @param hhdb 
	 */
	public Income2PlansCalcRoute(final PlansCalcRouteConfigGroup group, final Network network, 
			final TravelCost costCalculator,
			final TravelTime timeCalculator, LeastCostPathCalculatorFactory factory, PersonHouseholdMapping hhdb){
		super(group, network, costCalculator, timeCalculator, factory);
		this.incomeCostCalculator = (BKickIncome2TravelTimeDistanceCostCalculator)costCalculator;
		this.hhdb = hhdb;
	}

	@Override
	public void run(final Person person) {
		this.incomeCostCalculator.setIncome(this.hhdb.getHousehold(person.getId()).getIncome());
		super.run(person);
	}
	

	@Override
	public void run(Plan plan){
		this.incomeCostCalculator.setIncome(this.hhdb.getHousehold(plan.getPerson().getId()).getIncome());
		super.run(plan);
	}
	
}
