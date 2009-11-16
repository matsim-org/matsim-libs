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

package org.matsim.core.replanning.selectors;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;

/**
 * Selects one of the existing plans of the person based on the
 * weight = exp(beta*score).
 *
 * @author mrieser
 */
public class ExpBetaPlanSelector implements PlanSelector {

	protected static final double MIN_WEIGHT = Double.MIN_VALUE;
	protected final double beta;

	public ExpBetaPlanSelector() {
		this.beta = Double.parseDouble(Gbl.getConfig().getParam("planCalcScore", "BrainExpBeta"));
	}

	/**
	 * @return Returns a random plan from the person, random but according to its weight.
	 */
	public Plan selectPlan(final Person person) {

		// Build the weights of all plans
		// - first find the max. score of all plans of this person
		double maxScore = Double.NEGATIVE_INFINITY;
		for (Plan plan : person.getPlans()) {
			if ((plan.getScore() != null) && (plan.getScore().doubleValue() > maxScore)) {
				maxScore = plan.getScore().doubleValue();
			}
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
		double selnum = sumWeights*MatsimRandom.getRandom().nextDouble();
		idx = 0;
		for (Plan plan : person.getPlans()) {
			selnum -= weights[idx];
			if (selnum <= 0.0) {
				return plan;
			}
			idx++;
		}

		// hmm, no plan returned... either the person has no plans, or the plan(s) have no score.
		if (person.getPlans().size() > 0) {
			return person.getPlans().get(0);
		}

		// this case should never happen, except a person has no plans at all.
		return null;
	}

	/**
	 * Calculates the weight of a single plan
	 *
	 * @param plan
	 * @param maxScore
	 * @return the weight of the plan
	 */
	protected double calcPlanWeight(final Plan plan, final double maxScore) {
		if (plan.getScore() == null) {
			return Double.NaN;
		}
		double weight = Math.exp(this.beta * (plan.getScore() - maxScore));
		if (weight < MIN_WEIGHT) weight = MIN_WEIGHT;
		return weight;
	}
}
