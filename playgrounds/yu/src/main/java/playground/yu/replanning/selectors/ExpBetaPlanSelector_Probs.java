/* *********************************************************************** *
 * project: org.matsim.*
 * ExpBetaPlanSelector_Probs.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;

import playground.yu.utils.DebugTools;

/**
 * @author yu
 * 
 */
public class ExpBetaPlanSelector_Probs extends ExpBetaPlanSelector {

	/**
	 * @param charyparNagelScoringConfigGroup
	 */
	public ExpBetaPlanSelector_Probs(
			CharyparNagelScoringConfigGroup charyparNagelScoringConfigGroup) {
		super(charyparNagelScoringConfigGroup);
	}

	/**
	 * directly copied from
	 * {@codeorg.matsim.core.replanning.selectors.ExpBetaPlanSelector
	 * .selectPlan(Person person)}, with output, that reforms choice probability
	 * (weights)
	 */
	public Plan selectPlan(final Person person) {

		// Build the weights of all plans
		// - first find the max. score of all plans of this person
		double maxScore = Double.NEGATIVE_INFINITY;
		for (Plan plan : person.getPlans()) {
			if ((plan.getScore() != null)
					&& (plan.getScore().doubleValue() > maxScore)) {
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

		System.out.print(ExpBetaPlanSelector_Probs.class.getName() + "\t"
				+ DebugTools.getLineNumber(new Exception()) + "\tperson\t"
				+ person.getId() + "probs:\t");
		for (int i = 0; i < weights.length; i++)
			System.out.print(weights[i] / sumWeights + "\t");
		System.out.println();

		// choose a random number over interval [0,sumWeights[
		double selnum = sumWeights * MatsimRandom.getRandom().nextDouble();
		idx = 0;
		for (Plan plan : person.getPlans()) {
			selnum -= weights[idx];
			if (selnum <= 0.0) {
				return plan;
			}
			idx++;
		}

		// hmm, no plan returned... either the person has no plans, or the
		// plan(s) have no score.
		if (person.getPlans().size() > 0) {
			return person.getPlans().get(0);
		}

		// this case should never happen, except a person has no plans at all.
		return null;
	}
}
