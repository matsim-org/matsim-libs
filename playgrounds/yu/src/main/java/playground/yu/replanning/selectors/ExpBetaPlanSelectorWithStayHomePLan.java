/* *********************************************************************** *
 * project: org.matsim.*
 * ExpBetaPlanSelectorWithStayHomePLan.java
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
package playground.yu.replanning.selectors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.yu.demandModifications.StayHomePlan;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.BseStrategyManager;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.parametersCorrection.BseParamCalibrationControlerListener;
import playground.yu.utils.container.CollectionMax;

/**
 * @author yu
 *
 */
public class ExpBetaPlanSelectorWithStayHomePLan implements PlanSelector {
	private static final Logger log = Logger
			.getLogger(ExpBetaPlanSelectorWithStayHomePLan.class);
	protected static final double MIN_WEIGHT = Double.MIN_VALUE;
	/**
	 * (f - the probability that any NOT "stay home" {@code Plan} can be
	 * chosen).
	 */
	protected final double f, betaBrain;
	private double stayHomeScore = Double.NEGATIVE_INFINITY;

	public ExpBetaPlanSelectorWithStayHomePLan(Scenario scenario) {
		Config config = scenario.getConfig();
		betaBrain = config.planCalcScore().getBrainExpBeta();
		String fStr = config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				BseStrategyManager.NOT_STAY_HOME_PROB);
		if (fStr == null) {
			f = 0.5;
		} else {
			f = Double.parseDouble(fStr);
		}
	}

	private Double getDummyStayHomePlanScore4ChoiceProb(Person person) {
		/*
		 * calculate stay Home Plan score, in order to realize the choice
		 * probability "1-f", U_stayHome = 1/betaBrain * ln[(1-f)/f *
		 * sigma[exp(betaBrain * (U_other-U_max)]] + U_max
		 */
		Plan stayHomePlan = null;
		double expBetaScoreDiffSum = 0d;
		List<Double> unStayHomeScores = new ArrayList<Double>();

		for (Plan plan : person.getPlans()) {

			if (!StayHomePlan.isAStayHomePlan(plan)) {
				// expBetaScoreSum += Math.exp(betaBrain * plan.getScore());
				unStayHomeScores.add(plan.getScore());
			} else {// stay home
				stayHomePlan = plan;
			}
		}

		if (stayHomePlan == null) {
			log.error("There are NOT \"stay home\" Plan of Person\t"
					+ person.getId() + "\t!!!");
			return null;
		}

		double Vmax = CollectionMax.getDoubleMax(unStayHomeScores);
		for (Double Vj : unStayHomeScores) {
			expBetaScoreDiffSum += Math.exp(betaBrain * (Vj - Vmax));
		}

		return Math.log((1d - f) / f * expBetaScoreDiffSum) / betaBrain + Vmax;
	}

	/**
	 * @return Returns a random plan from the person, random but according to
	 *         its weight.
	 */
	@Override
	public Plan selectPlan(final Person person) {
		// preferentially choose NULL or NaN -score Plan
		for (Plan plan : person.getPlans()) {
			Double score = plan.getScore();
			if (score == null) {
				return plan;
			}
			if (Double.isNaN(score)) {
				return plan;
			}
		}

		// get the weights of all plans
		Map<Plan, Double> weights = calcWeights(person);

		double sumWeights = 0.0;
		for (Double weight : weights.values()) {
			sumWeights += weight.doubleValue();
		}

		// choose a random number over interval [0, sumWeights[
		double selnum = sumWeights * MatsimRandom.getRandom().nextDouble();
		for (Plan plan : person.getPlans()) {
			selnum -= weights.get(plan);
			if (selnum <= 0.0) {
				return plan;
			}
		}

		// hmm, no plan returned... either the person has no plans, or the
		// plan(s) have no score.
		if (person.getPlans().size() > 0) {
			return person.getPlans().get(0);
		}

		// this case should never happen, except a person has no plans at all.
		return null;
	}

	/**
	 * Calculates the weight of a single plan.
	 *
	 * @param plan
	 * @param maxScore
	 * @return the weight of the plan
	 */
	protected double calcPlanWeight(final Plan plan, final double maxScore) {

		if (plan.getScore() == null) {
			return Double.NaN;
		}
		Double score = plan.getScore();
		if (StayHomePlan.isAStayHomePlan(plan)) {
			score = stayHomeScore;
		}
		double weight = Math.exp(betaBrain * (score - maxScore));
		if (weight < MIN_WEIGHT) {
			weight = MIN_WEIGHT;
		}
		return weight;
	}

	/**
	 * Builds the weights of all plans.
	 *
	 * @param person
	 * @return a map containing the weights of all plans
	 */
	Map<Plan, Double> calcWeights(final Person person) {
		stayHomeScore = getDummyStayHomePlanScore4ChoiceProb(person);
		// - first find the max. score of all plans of this person
		double maxScore = Double.NEGATIVE_INFINITY;
		for (Plan plan : person.getPlans()) {
			Double score = plan.getScore();
			if (StayHomePlan.isAStayHomePlan(plan)) {
				score = stayHomeScore;
			}
			if (score != null && score.doubleValue() > maxScore) {
				maxScore = score.doubleValue();
			}

		}

		Map<Plan, Double> weights = new LinkedHashMap<Plan, Double>(person
				.getPlans().size());

		for (Plan plan : person.getPlans()) {
			weights.put(plan, calcPlanWeight(plan, maxScore));
		}

		return weights;
	}

	/*
	 * public double getSelectionProbability(final Plan plan) {
	 *
	 * Map<Plan, Double> weights = calcWeights(plan.getPerson()); double
	 * thisWeight = weights.get(plan);
	 *
	 * double sumWeights = 0.0; for (Double weight : weights.values()) {
	 * sumWeights += weight.doubleValue(); }
	 *
	 * return thisWeight / sumWeights; }
	 */
}
