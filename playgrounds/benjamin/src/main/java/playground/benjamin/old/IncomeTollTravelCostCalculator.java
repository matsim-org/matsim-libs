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
package playground.benjamin.old;

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
public class IncomeTollTravelCostCalculator implements PersonalizableTravelCost {

	public class NullTravelCostCalculator implements PersonalizableTravelCost {

		@Override
		public void setPerson(Person person) {
			
		}

		@Override
		public double getLinkTravelCost(Link link, double time) {
			return 0;
		}

	}

	/*	this parameter has to be equal to the one appearing several times in the scoring function, e.g.
	 * 	ScoringFromDailyIncome, ScoringFromLeg and ScoringFromToll and eventually other money related parts of the scoring function.
	 * 	Also see IncomeTravelCostCalculator!
	 *  "Car" in the parameter name is not relevant.*/
	private static double betaIncomeCar = 4.58;
	
	private double incomePerDay;
	
	private PersonHouseholdMapping hhdb;
	
	private TollTravelCostCalculator tollTravelCostCalculator;
	
	
	public IncomeTollTravelCostCalculator(PersonHouseholdMapping hhdb, RoadPricingScheme scheme) {
		this.hhdb = hhdb;
		PersonalizableTravelCost nullTravelCostCalculator = new NullTravelCostCalculator();
		this.tollTravelCostCalculator = new TollTravelCostCalculator(nullTravelCostCalculator, scheme);
	}

	@Override
	public void setPerson(Person person) {
		this.incomePerDay = getHouseholdIncomePerDay(person, hhdb);
	}

	//calculating additional generalized toll costs
	@Override
	public double getLinkTravelCost(Link link, double time) {
		double amount = tollTravelCostCalculator.getLinkTravelCost(link, time);
		double additionalGeneralizedTollCost = (betaIncomeCar / incomePerDay) * amount;
		return additionalGeneralizedTollCost;
	}

	private double getHouseholdIncomePerDay(Person person, PersonHouseholdMapping hhdb) {
		Income income = hhdb.getHousehold(person.getId()).getIncome();
		double incomePerDay = this.calculateIncomePerDay(income);
		if (Double.isNaN(incomePerDay)){
			throw new IllegalStateException("cannot calculate income for person: " + person.getId());
		}
		return incomePerDay;
	}

	private double calculateIncomePerDay(Income income) {
		if (income.getIncomePeriod().equals(IncomePeriod.year)) {
			//assumption: 240 working days per year
			double incomePerDay = income.getIncome() / 240;
			return incomePerDay;
		} else {
			throw new UnsupportedOperationException("Can't calculate income per day");
		}
	}
	
}
