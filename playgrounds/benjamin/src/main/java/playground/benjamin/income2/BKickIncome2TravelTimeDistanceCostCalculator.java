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
package playground.benjamin.income2;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.households.Household;
import org.matsim.households.Income;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.households.Income.IncomePeriod;


/**
 * @author dgrether
 *
 */
public class BKickIncome2TravelTimeDistanceCostCalculator implements PersonalizableTravelCost {

	private static final Logger log = Logger.getLogger(BKickIncome2TravelTimeDistanceCostCalculator.class);
	
	private static double betaIncomeCar = 4.58;
	
	protected TravelTime timeCalculator;
	private double traveltimeCostFactor;
	private double marginalUtlOfDistance;
	
	private PersonHouseholdMapping personHouseholdMapping;
	
	private double income;

	public BKickIncome2TravelTimeDistanceCostCalculator(final TravelTime timeCalculator, CharyparNagelScoringConfigGroup charyparNagelScoring, PersonHouseholdMapping personHouseholdMapping) {
		this.timeCalculator = timeCalculator;
		/* Usually, the travel-utility should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 */
		this.traveltimeCostFactor = (- charyparNagelScoring.getTraveling() / 3600.0) + (charyparNagelScoring.getPerforming() / 3600.0);
		this.marginalUtlOfDistance = - charyparNagelScoring.getMarginalUtlOfDistanceCar() * betaIncomeCar;
		this.personHouseholdMapping = personHouseholdMapping;
//		log.info("Using BKickIncome2TravelTimseDistanceCostCalculator...");
	}

	/**
	 * @see org.matsim.core.router.util.TravelCost#getLinkTravelCost(org.matsim.core.network.LinkImpl, double)
	 */
	public double getLinkTravelCost(Link link, double time) {
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time);
		if (this.marginalUtlOfDistance == 0.0) {
			return travelTime * this.traveltimeCostFactor;
		}
		double cost = travelTime * this.traveltimeCostFactor + (this.marginalUtlOfDistance * link.getLength() / this.income);
//		log.error("Link id " + link.getId() + " cost "  + cost );
		return cost;
	}
	
	
	@Override
	public void setPerson(Person person) {
		Household household = this.personHouseholdMapping.getHousehold(person.getId());
		Income income = household.getIncome();
		this.setIncome(income);
	}

	private void setIncome(Income income) {
		if (income.getIncomePeriod().equals(IncomePeriod.year)) {
			this.income = income.getIncome() / 240;
		}
		else {
			throw new UnsupportedOperationException("Can't calculate income per day");
		}
	}
	
}
