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
package playground.benjamin.scoring.income.old;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.households.Income;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.vehicles.Vehicle;


/**
 * @author dgrether
 *
 */
public class BKickIncomeTravelTimeDistanceCostCalculator implements TravelDisutility {

	private static final Logger log = Logger.getLogger(BKickIncomeTravelTimeDistanceCostCalculator.class);
	
	private static double betaIncomeCar = 1.31;
	
	protected TravelTime timeCalculator;
	private double travelCostFactor;
	private double marginalUtlOfDistance;
	
	private double income;

	public BKickIncomeTravelTimeDistanceCostCalculator(final TravelTime timeCalculator, PlanCalcScoreConfigGroup charyparNagelScoring) {
		this.timeCalculator = timeCalculator;
		/* Usually, the travel-utility should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 */
		this.travelCostFactor = (-charyparNagelScoring.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.0) + (charyparNagelScoring.getPerforming_utils_hr() / 3600.0);

//		this.marginalUtlOfDistance = charyparNagelScoring.getMarginalUtlOfDistanceCar() * 1.31;
		this.marginalUtlOfDistance = charyparNagelScoring.getModes().get(TransportMode.car).getMonetaryDistanceRate() * 1.31 * charyparNagelScoring.getMarginalUtilityOfMoney() ;
		
		log.info("Using BKickIncomeTravelTimeDistanceCostCalculator...");
	}

	/**
	 * @see org.matsim.core.router.util.TravelDisutility#getLinkTravelDisutility(org.matsim.core.network.LinkImpl, double)
	 */
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);
		if (this.marginalUtlOfDistance == 0.0) {
			return travelTime * this.travelCostFactor;
		}
		return travelTime * this.travelCostFactor - (this.marginalUtlOfDistance * link.getLength() / this.income);
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}
	
	
	public void setIncome(Income income) {
		if (income.getIncomePeriod().equals(IncomePeriod.year)) {
			this.income = income.getIncome() / (240 * 3.5);
		}
		else {
			throw new UnsupportedOperationException("Can't calculate income per trip");
		}
	}

}
