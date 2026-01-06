/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.scoring.functions;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.pt.routes.TransitPassengerRoute;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is a re-implementation of the original CharyparNagel function, based on a
 * modular approach.
 *
 * @author rashid_waraich
 */
public class CharyparNagelLegScoring implements SumScoringFunction.TripScoring {

	private final ScoringParameters params;
	// have a separate parameter for money, so that we can set individual utilities per person (usually, this is the same as in the scoring params above)
	private final double marginalUtilityOfMoney;
	private final Set<String> ptModes;
	private final Set<String> modesAlreadyConsideredForDailyConstants;
	private final DoubleList legScores;


	protected double score;

	public CharyparNagelLegScoring(final ScoringParameters params, Set<String> ptModes) {
		this.params = params;
		this.marginalUtilityOfMoney = params.marginalUtilityOfMoney;
		this.ptModes = ptModes;
		this.modesAlreadyConsideredForDailyConstants = new HashSet<>();
		this.legScores = new DoubleArrayList();
	}

	/**
	 * Scoring with person-specific marginal utility of money
	 */
	public CharyparNagelLegScoring(final ScoringParameters params, double marginalUtilityOfMoney, Set<String> ptModes) {
		this.params = params;
		this.ptModes = ptModes;
		this.modesAlreadyConsideredForDailyConstants = new HashSet<>();
		this.legScores = new DoubleArrayList();
		this.marginalUtilityOfMoney = marginalUtilityOfMoney;
	}

	/**
	 * Scoring with pt modes set to 'pt'
	 */
	public CharyparNagelLegScoring(final ScoringParameters params) {
		this(params, new HashSet<>(Collections.singletonList("pt")));
	}

	@Override
	public void finish() {

	}

	@Override
	public double getScore() {
		return this.score;
	}

	@Override
	public void explainScore(StringBuilder out) {
		out.append("legs_util=").append(score);

		// Store for each leg
		if (!legScores.isEmpty()) {
			for (int i = 0; i < legScores.size(); i++) {
				out.append(ScoringFunction.SCORE_DELIMITER).append(" legs_").append(i).append("_util=").append(legScores.getDouble(i));
			}
		}
	}

	@Override
	public void handleTrip(TripStructureUtils.Trip trip) {

		var seenModesInTrip = new HashSet<String>();
		var tripScore = 0.;
		var ptLegsInTrip = new AtomicInteger();

		for (var leg : trip.getLegsOnly()) {
			var baseScore = calcBasicLegScore(leg);
			var waitScore = calcWaitScore(leg);
			// this adds the mode constant only once per trip. This is what the old code did for pt. For other modes this is a different behavior
			// compared to the previous code. Yet we usually only have several walk legs per trip and the constant is usually 0. for this mode.
			var tripConstant = calcTripConstant(leg, seenModesInTrip);
			var dailyConstant = calcDailyConstant(leg, modesAlreadyConsideredForDailyConstants);
			var lineSwitch = calcLineSwitch(leg, ptLegsInTrip);
			var legScore = baseScore + waitScore + tripConstant + dailyConstant + lineSwitch;
			legScores.add(legScore);
			tripScore += legScore;
		}
		this.score += tripScore;
	}

	private double calcBasicLegScore(Leg leg) {
		Gbl.assertIf(leg.getTravelTime().isDefined());
		if (Double.isNaN(leg.getRoute().getDistance())) {
			throw new RuntimeException("Distance is NaN which cannot be interpreted. A previous version of this code allowed NaN distances, but " +
				" threw an exception at a later point. Therefore we abort here immediately");
		}

		var modeParams = params.modeParams.get(leg.getMode());
		var utilTravelTime = leg.getTravelTime().seconds() * modeParams.marginalUtilityOfTraveling_s;
		var utilDist = leg.getRoute().getDistance() * modeParams.marginalUtilityOfDistance_m;
		// use the extra marginal utility of money parameter, to allow for person individual utilities of money.
		var utilDistCosts = leg.getRoute().getDistance() * modeParams.monetaryDistanceCostRate * this.marginalUtilityOfMoney;

		return utilTravelTime + utilDist + utilDistCosts;
	}

	private double calcWaitScore(Leg leg) {
		if (leg.getRoute() instanceof TransitPassengerRoute tpr) {
			var waitTime = tpr.getBoardingTime().seconds() - leg.getDepartureTime().seconds();
			var waitUtil = params.marginalUtilityOfWaitingPt_s - params.modeParams.get(leg.getMode()).marginalUtilityOfTraveling_s;
			return waitTime * waitUtil;
		}
		return 0.;
	}

	/**
	 * Returns the constant for leg's mode if it is not contained in seen modes.
	 */
	private double calcTripConstant(Leg leg, Set<String> seenModes) {
		if (seenModes.contains(leg.getMode())) {
			return 0.;
		}
		seenModes.add(leg.getMode());
		return params.modeParams.get(leg.getMode()).constant;
	}

	private double calcDailyConstant(Leg leg, Set<String> seenModes) {
		if (seenModes.contains(leg.getMode())) {
			return 0.;
		}
		seenModes.add(leg.getMode());
		return params.modeParams.get(leg.getMode()).dailyUtilityConstant + params.modeParams.get(leg.getMode()).dailyMoneyConstant * params.marginalUtilityOfMoney;
	}

	/**
	 *
	 * @param ptLegsInTrip use AtomicInteger so that we can change the counter at the call site.
	 */
	private double calcLineSwitch(Leg leg, AtomicInteger ptLegsInTrip) {
		if (ptModes.contains(leg.getMode())) {
			if (ptLegsInTrip.incrementAndGet() > 1) {
				return params.utilityOfLineSwitch;
			}
		}
		return 0.;
	}
}
