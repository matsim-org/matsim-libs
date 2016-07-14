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
package playground.benjamin.scoring.income.old;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.households.Income;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.vehicles.Vehicle;

/**
 * @author bkick
 * @author michaz
 * 
 */

// a dummy class in order to meet the framework
public class IncomeTollTravelCostCalculator implements TravelDisutility {

	public class NullTravelCostCalculator implements TravelDisutility {

		@Override
		public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
			return 0;
		}
		
		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			return 0;
		}

	}

	/*	this parameter has to be equal to the one appearing several times in the scoring function, e.g.
	 * 	ScoringFromDailyIncome, ScoringFromLeg and ScoringFromToll and eventually other money related parts of the scoring function.
	 * 	Also see IncomeTravelCostCalculator!
	 *  "Car" in the parameter name is not relevant.*/
	private static double betaIncomeCar = 4.58;
	
	private PersonHouseholdMapping hhdb;
	
	private TravelDisutility tollTravelCostCalculator;

	
	public IncomeTollTravelCostCalculator(PersonHouseholdMapping hhdb, RoadPricingScheme scheme, Config config) {
		this.hhdb = hhdb;
		TravelDisutility nullTravelCostCalculator = new NullTravelCostCalculator();
//		this.tollTravelCostCalculator = new TravelDisutilityIncludingToll(nullTravelCostCalculator, scheme, config);
		throw new RuntimeException("above constructor is gone.  please talk to me if you still need this (not a big probley, just a "
				+ "different syntax. kai, sep'14") ;
	}

	//calculating additional generalized toll costs
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double incomePerDay = getHouseholdIncomePerDay(person, hhdb);
		double amount = tollTravelCostCalculator.getLinkTravelDisutility(link, time, person, vehicle);
		double additionalGeneralizedTollCost = (betaIncomeCar / incomePerDay) * amount;
		return additionalGeneralizedTollCost;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
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
