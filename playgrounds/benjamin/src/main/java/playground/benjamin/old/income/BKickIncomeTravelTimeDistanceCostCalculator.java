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
package playground.benjamin.old.income;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.households.Income;
import org.matsim.households.Income.IncomePeriod;


/**
 * @author dgrether
 *
 */
public class BKickIncomeTravelTimeDistanceCostCalculator implements PersonalizableTravelCost {

	private static final Logger log = Logger.getLogger(BKickIncomeTravelTimeDistanceCostCalculator.class);
	
	private static double betaIncomeCar = 1.31;
	
	protected TravelTime timeCalculator;
	private double travelCostFactor;
	private double marginalUtlOfDistance;
	
	private double income;

	public BKickIncomeTravelTimeDistanceCostCalculator(final TravelTime timeCalculator, CharyparNagelScoringConfigGroup charyparNagelScoring) {
		this.timeCalculator = timeCalculator;
		/* Usually, the travel-utility should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 */
		this.travelCostFactor = (- charyparNagelScoring.getTraveling() / 3600.0) + (charyparNagelScoring.getPerforming() / 3600.0);
		this.marginalUtlOfDistance = charyparNagelScoring.getMarginalUtlOfDistanceCar() * 1.31;
		
		log.info("Using BKickIncomeTravelTimeDistanceCostCalculator...");
	}

	/**
	 * @see org.matsim.core.router.util.TravelCost#getLinkTravelCost(org.matsim.core.network.LinkImpl, double)
	 */
	public double getLinkTravelCost(Link link, double time) {
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time);
		if (this.marginalUtlOfDistance == 0.0) {
			return travelTime * this.travelCostFactor;
		}
		return travelTime * this.travelCostFactor - (this.marginalUtlOfDistance * link.getLength() / this.income);
	}
	
	
	public void setIncome(Income income) {
		if (income.getIncomePeriod().equals(IncomePeriod.year)) {
			this.income = income.getIncome() / (240 * 3.5);
		}
		else {
			throw new UnsupportedOperationException("Can't calculate income per trip");
		}
	}

	@Override
	public void setPerson(Person person) {
		// TODO: put setIncome(person.getIncome()) or some such here, and remoset setIncome.
	}
	
	
	

}
