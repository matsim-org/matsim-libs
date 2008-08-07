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
import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Person;
import org.matsim.population.Plan;

/**
 * Selects one of the existing plans of the person based on the
 * weight = exp(beta*score).
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
	 * @return Returns a random plan from the person, random but according to its weight.
	 */
	public Plan selectPlan(final Person person) {

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
		double selnum = sumWeights*MatsimRandom.random.nextDouble();
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
	private double calcPlanWeight(final Plan plan, final double maxScore) {
		double weight = Math.exp(this.beta * (plan.getScore() - maxScore));
		if (weight < MIN_WEIGHT) weight = MIN_WEIGHT;
		return weight;
	}
}
