/* *********************************************************************** *
 * project: org.matsim.*
 * StayHomePlanASC.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 *
 */
package playground.yu.demandModifications;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;


/**
 * <P>
 * calculates the special ASC of every stay home plan from a MATSim simulation
 * with a {@code Population}, in which each person has one "stay home
 * {@code Plan}".
 * </P>
 * <P>
 * ASC = 1/betaBrain * ln( (1-f)/f * sigma exp(betaBrain * (Vi-Vmax)) ) + Vmax -
 * VstayHome
 * </P>
 * <P>
 * i - index of not stay home Plan
 * </P>
 * <P>
 * f - the probability that any "not stay home {@code Plan}" can be chosen
 * </P>
 * <P>
 * betaBrain - brainExpBeta in {@code PlanCalcScoreConfigGroup}
 * </P>
 * <P>
 * Vmax - max value amony scores of "not stay home" {@code Plan}s
 * </P>
 * <P>
 * VstayHome - score of "stay home" {@code Plan} that was created by "scoring"
 * in MATSim
 * </P>
 *
 * @author yu
 *
 */
public class StayHomePlanASC extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
	public static String STAY_HOME_ASC = "stayHomeASC";

	private List<Double> notStayHomeScores = new ArrayList<Double>();
	private double Vmax, stayHomeScore;
	private Plan stayHome;

	private final double f, betaBrain;

	/**
	 * @param f
	 *            the probability that any "not stay home {@code Plan}" can be
	 *            chosen
	 * @param betaBrain
	 *            brainExpBeta in {@code PlanCalcScoreConfigGroup}
	 */
	public StayHomePlanASC(double f, double betaBrain) {
		this.f = f;
		this.betaBrain = betaBrain;
	}

	@Override
	public void run(Plan plan) {
		Double score = plan.getScore();
		if (score == null) {
			throw new RuntimeException(
					"This Plan has not score, scoring should have been done preprocessiong.");
		}

		if (!StayHomePlan.isAStayHomePlan(plan)) {
			notStayHomeScores.add(score);
			if (score > Vmax) {
				Vmax = score;
			}
		} else/* is a "stay home" Plan */{
			stayHome = plan;
			stayHomeScore = score;
		}
	}

	private void reset() {
		notStayHomeScores.clear();
		Vmax = Double.NEGATIVE_INFINITY;
		stayHomeScore = Double.NEGATIVE_INFINITY;
		stayHome = null;
	}

	@Override
	public void run(Person person) {
		reset();
		for (Plan plan : person.getPlans()) {
			run(plan);
		}
		finish();
	}

	/**
	 * saves ASC in custom attribute of this unique "stay home {@code Plan}" of
	 * this Person
	 */
	private void finish() {
		double expBetaScoreDiffSum = 0d;
		for (Double Vj : notStayHomeScores) {
			expBetaScoreDiffSum += Math.exp(betaBrain * (Vj - Vmax));
		}
		double ASC = Math.log((1 - f) / f * expBetaScoreDiffSum) / betaBrain
				+ Vmax - stayHomeScore;
		stayHome.getCustomAttributes().put(STAY_HOME_ASC, ASC);
	}
}
