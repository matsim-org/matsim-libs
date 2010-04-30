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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;

/**
 * @author dgrether
 * @author bkick
 * @author michaz
 *
 */

public class ScoringFromLeg extends LegScoringFunction {

	private static final Logger log = Logger.getLogger(ScoringFromLeg.class);

/*	setting these parameters different from each other, will cause problems when converting utils into money terms.
	Also see ScoringFromDailyIncome, ScoringFromToll or other money related parts of the scoring function*/
	private static double betaIncomeCar = 4.58;
	private static double betaIncomePt = 4.58;

	private double incomePerDay;

	private final Network network;

	public ScoringFromLeg(final Plan plan, final CharyparNagelScoringParameters params, Network network, double householdIncomePerDay) {
		super(plan, params);
		this.incomePerDay = householdIncomePerDay;
		this.network = network;
		log.trace("Using BKickLegScoring...");
	}

	@Override
	public void finish() {
		
	}

	@Override
	protected double calcLegScore(double departureTime, double arrivalTime, Leg leg) {
		return calculateLegScore(departureTime, arrivalTime, leg);
	}

	private double calculateLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
		double distance = calculateLegDistance(leg);
		double travelTime = arrivalTime - departureTime; // traveltime in seconds
		if (TransportMode.car.equals(leg.getMode())) {
			double betaIncome = betaIncomeCar;
			double distanceCostCar = this.params.marginalUtilityOfDistanceCar * distance;
			double distanceCost = distanceCostCar;
			double betaTravelTime = this.params.marginalUtilityOfTraveling;
			double legScore = calculateScore(betaIncome, distanceCost, betaTravelTime, travelTime);
			return legScore;
		} else if (TransportMode.pt.equals(leg.getMode())) {
			double betaIncome = betaIncomePt;
			double distanceCostPt = this.params.marginalUtilityOfDistancePt * distance;
			double distanceCost = distanceCostPt;
			double betaTravelTime = this.params.marginalUtilityOfTravelingPT;
			double legScore = calculateScore(betaIncome, distanceCost, betaTravelTime, travelTime);
			return legScore;
		} else {
			throw new IllegalStateException("Scoring funtion not defined for other modes than pt and car!");
		}
	}

	private double calculateLegDistance(final Leg leg) {
		Route route = leg.getRoute();
		double dist = route.getDistance();
		if (TransportMode.car.equals(leg.getMode())) {
			dist += this.network.getLinks().get(route.getEndLinkId()).getLength();
		}
		
		if (Double.isNaN(dist)){
			throw new IllegalStateException("Route distance is NaN for person: " + this.plan.getPerson().getId());
		}
		return dist;
	}

	private double calculateScore(double betaIncome, double distanceCost, double betaTravelTime, double travelTime) {
		double betaCost = betaIncome / this.incomePerDay;
		double distanceCostScore = betaCost       * distanceCost;
		double travelTimeScore   = betaTravelTime * travelTime ;
		
		//this is the actual (negative) utility from distance costs and travel time
		double score = distanceCostScore + travelTimeScore;
		
		if (Double.isNaN(score)){
			throw new IllegalStateException("Leg score is NaN for person: " + this.plan.getPerson().getId());
		}
		return score;
	}

}
