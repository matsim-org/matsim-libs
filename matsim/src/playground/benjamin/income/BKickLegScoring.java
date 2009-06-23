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
package playground.benjamin.income;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Route;
import org.matsim.core.basic.v01.households.BasicIncome;
import org.matsim.core.basic.v01.households.BasicIncome.IncomePeriod;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;

/**
 * @author dgrether
 * 
 */
public class BKickLegScoring extends LegScoringFunction {

	private static final Logger log = Logger.getLogger(BKickLegScoring.class);

	private static double betaIncomeCar = 1.31;

	private static double betaIncomePt = 1.31;

	private double incomePerTrip;

	public BKickLegScoring(final Plan plan, final CharyparNagelScoringParameters params) {
		super(plan, params);
		BasicIncome income = plan.getPerson().getHousehold().getIncome();
		this.incomePerTrip = this.calculateIncomePerTrip(income);
		
//		log.info("Using BKickLegScoring...");
	}

	@Override
	protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in
		// seconds
		double dist = 0.0; // distance in meters

		if (TransportMode.car.equals(leg.getMode())) {
			Route route = leg.getRoute();
			dist = route.getDistance();
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling + this.params.marginalUtilityOfDistanceCar * dist
					* betaIncomeCar / this.incomePerTrip + betaIncomeCar * Math.log(this.incomePerTrip);
		}
		else
			if (TransportMode.pt.equals(leg.getMode())) {
				dist = leg.getRoute().getDistance();
//				log.error("dist: " + dist);
				tmpScore += travelTime * this.params.marginalUtilityOfTravelingPT + this.params.marginalUtilityOfDistancePt
						* dist * betaIncomePt / this.incomePerTrip + betaIncomePt * Math.log(this.incomePerTrip);
			}
			else {
				throw new IllegalStateException("Scoring funtion not defined for other modes than pt and car!");
			}

		return tmpScore;
	}

	private double calculateIncomePerTrip(BasicIncome income) {
		double ipt = Double.NaN;
		if (income.getIncomePeriod().equals(IncomePeriod.year)) {
			ipt = income.getIncome() / (240 * 3.5);
//			log.debug("income: " + ipt);
		}
		else {
			throw new UnsupportedOperationException("Can't calculate income per trip");
		}
		return ipt;
	}

}
