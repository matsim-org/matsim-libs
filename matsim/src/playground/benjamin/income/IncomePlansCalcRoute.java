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
package playground.benjamin.income;

import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;


/**
 * @author dgrether
 *
 */
public class IncomePlansCalcRoute extends PlansCalcRoute{

	
	private BKickIncomeTravelTimeDistanceCostCalculator incomeCostCalculator;

	/**
	 * Uses the speed factors from the config group and the rerouting of the factory 
	 */
	public IncomePlansCalcRoute(final PlansCalcRouteConfigGroup group, final Network network, 
			final TravelCost costCalculator,
			final TravelTime timeCalculator, LeastCostPathCalculatorFactory factory){
		super(group, network, costCalculator, timeCalculator, factory);
		this.incomeCostCalculator = (BKickIncomeTravelTimeDistanceCostCalculator)costCalculator;
	}

	@Override
	public void run(final Person person) {
		this.incomeCostCalculator.setIncome(person.getHousehold().getIncome());
		super.run(person);
	}
	

	@Override
	public void run(Plan plan){
		this.incomeCostCalculator.setIncome(plan.getPerson().getHousehold().getIncome());
		super.run(plan);
	}
	
}
