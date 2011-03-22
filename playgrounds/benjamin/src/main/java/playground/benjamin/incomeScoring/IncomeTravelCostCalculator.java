/* *********************************************************************** *
 * project: org.matsim.*
 * BKickIncomeTravelTimeDistanceCostCalculator
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
package playground.benjamin.incomeScoring;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.households.Household;
import org.matsim.households.Income;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.households.Income.IncomePeriod;


/**
 * @author bkick after dgrether
 *
 */
public class IncomeTravelCostCalculator implements PersonalizableTravelCost {
		
//	private Double betaCost = null;
	private double incomePerDay;
	
	/*	this parameter has to be equal to the one appearing several times in the scoring function, e.g.
	 * 	ScoringFromDailyIncome, ScoringFromLeg and ScoringFromToll and eventually other money related parts of the scoring function.
	 * 	Also see IncomeTravelCostCalculator!
		"Car" in the parameter name is not relevant.*/
	private static double betaIncomeCar = 4.58;
	
	private double distanceCostFactor;
	private double betaTravelTime;
	
	protected TravelTime timeCalculator;
	
	private PersonHouseholdMapping personHouseholdMapping;

	
	public IncomeTravelCostCalculator(final TravelTime timeCalculator, PlanCalcScoreConfigGroup charyparNagelScoring, PersonHouseholdMapping personHouseholdMapping) {
		this.timeCalculator = timeCalculator;
		this.personHouseholdMapping = personHouseholdMapping;
		
		/* Usually, utility from traveling should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 * Distance dependent routing is only impelemted for car since pt is only pseudo transit*/

//		this.distanceCostFactor = - charyparNagelScoring.getMarginalUtlOfDistanceCar();
		this.distanceCostFactor = - charyparNagelScoring.getMonetaryDistanceCostRateCar() * charyparNagelScoring.getMarginalUtilityOfMoney() ;

		//also opportunity costs of time have to be considered at this point (second summand)!
		this.betaTravelTime = (- charyparNagelScoring.getTraveling_utils_hr() / 3600.0) + (charyparNagelScoring.getPerforming_utils_hr() / 3600.0);
	}

	//calculate generalized travel costs
	public double getLinkGeneralizedTravelCost(Link link, double time) {
		double betaCost = betaIncomeCar / this.incomePerDay;
		double distance   = link.getLength();
		double distanceCost = this.distanceCostFactor * distance;
		
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time);
		
		double generalizedDistanceCost   = betaCost * distanceCost;
		double generalizedTravelTimeCost = this.betaTravelTime * travelTime;
		
			if (this.distanceCostFactor == 0.0) {
				return generalizedTravelTimeCost;
			}
		
		double generalizedTravelCost = generalizedDistanceCost + generalizedTravelTimeCost;
		return generalizedTravelCost;
	}
	
	@Override
	public void setPerson(Person person) {
		Household household = this.personHouseholdMapping.getHousehold(person.getId());
		Income income = household.getIncome();
		this.setIncome(income);
//		this.betaCost = betaIncomeCar / this.incomePerDay;	
	}

	private void setIncome(Income income) {
		if (income.getIncomePeriod().equals(IncomePeriod.year)) {
			//assumption: 240 working days per year
			this.incomePerDay = income.getIncome() / 240;
		}
		else {
			throw new UnsupportedOperationException("Can't calculate income per day");
		}
	}
	
}
