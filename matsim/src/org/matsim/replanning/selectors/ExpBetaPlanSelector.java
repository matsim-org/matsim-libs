/* *********************************************************************** *
 * project: org.matsim.*
 * ExpBetaPlanSelector.java
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

package org.matsim.replanning.selectors;

import org.matsim.basic.v01.BasicPlan;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

/**
 * Selects one of the existing plans of the person based on the
 * weight = exp(beta*score). If there are unscored plans, a random one
 * of the unscored plans will be chosen (optimistic strategy).
 *
 * @author mrieser
 */
public class ExpBetaPlanSelector implements PlanSelectorI {

	private static double MIN_WEIGHT = Double.MIN_VALUE;
	private final double beta;

	public ExpBetaPlanSelector() {
		this.beta = Double.parseDouble(Gbl.getConfig().getParam("planCalcScore", "BrainExpBeta"));
	}

	/**
	 * Selects a random plan from the person, random but according to its weight.
	 * If there are plans with undefined score, one of those plans will
	 * be selected.
	 */
	public Plan selectPlan(final Person person) {
		// First check if there are any unscored plans
		Plan selectedPlan = selectUnscoredPlan(person);
		if (selectedPlan != null) return selectedPlan;

		// Okay, no unscored plans...

		// Build the weights of all plans
		// - first find the max. score of all plans of this person
		double maxScore = Double.NEGATIVE_INFINITY;
		for (BasicPlan plan : person.getPlans()) {
			if (plan.getScore() > maxScore) maxScore = plan.getScore();
		}

		// - now calculate the weights
		double[] weights = new double[person.getPlans().size()];
		double sumWeights = 0.0;

		int idx = 0;
		for (Plan plan : person.getPlans()) {
			weights[idx] = calcPlanWeight(plan, maxScore);
			sumWeights += weights[idx];
			idx++;
		}

		// choose a random number over interval [0,sumWeights[
		double selnum = sumWeights*Gbl.random.nextDouble();
		idx = 0;
		for (Plan plan : person.getPlans()) {
			selnum -= weights[idx];
			if (selnum <= 0.0) {
				person.setSelectedPlan(plan);
				return plan;
			}
			idx++;
		}

		// this case should never happen, except a person has no plans at all.
		return null;
	}

	/**
	 * Returns a plan with undefined score, chosen randomly among all plans
	 * of the person with undefined score.
	 *
	 * @param person
	 * @return a random plan with undefined score, or null if none such plan exists.
	 */
	private Plan selectUnscoredPlan(final Person person) {
		int cntUnscored = 0;
		for (Plan plan : person.getPlans()) {
			if (Plan.isUndefinedScore(plan.getScore())) {
				cntUnscored++;
			}
		}
		if (cntUnscored > 0) {
			// select one of the unscored plans
			int idxUnscored = Gbl.random.nextInt(cntUnscored);
			cntUnscored = 0;
			for (Plan plan : person.getPlans()) {
				if (Plan.isUndefinedScore(plan.getScore())) {
					if (cntUnscored == idxUnscored) {
						person.setSelectedPlan(plan);
						return plan;
					}
					cntUnscored++;
				}
			}
		}
		return null;
	}

	/**
	 * Calculates the weight of a single plan
	 *
	 * @param plan
	 * @param maxScore
	 * @return the weight of the plan
	 */
	private double calcPlanWeight(final Plan plan, final double maxScore) {
		double weight = Math.exp(this.beta * (plan.getScore() - maxScore));
		if (weight <= 0.0) weight = MIN_WEIGHT;
		return weight;
	}
}
