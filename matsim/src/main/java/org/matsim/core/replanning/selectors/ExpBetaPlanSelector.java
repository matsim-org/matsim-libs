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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.MatsimRandom;

/**
 * Selects one of the existing plans of the person based on the
 * weight = exp(beta*score).
 *
 * @author mrieser
 */
public class ExpBetaPlanSelector<T extends BasicPlan, I> implements PlanSelector<T, I> {

	protected static final double MIN_WEIGHT = Double.MIN_VALUE;
	protected final double beta;

	public ExpBetaPlanSelector( final double logitScaleFactor ) {
		this.beta = logitScaleFactor ;
	}

	public ExpBetaPlanSelector(ScoringConfigGroup charyparNagelScoringConfigGroup) {
		this( charyparNagelScoringConfigGroup.getBrainExpBeta() ) ;
	}

	/**
	 * @return a random plan from the person, random but according to its weight.
	 */
	@Override
	public T selectPlan(final HasPlansAndId<T, I> person) {

		// get the weights of all plans
		Map<T, Double> weights = this.calcWeights(person);

		double sumWeights = 0.0;
		for (Double weight : weights.values()) {
			sumWeights += weight;
		}

		// choose a random number over interval [0, sumWeights[
		double selnum = sumWeights * MatsimRandom.getRandom().nextDouble();
		for (T plan : person.getPlans()) {
			selnum -= weights.get(plan);
			if (selnum <= 0.0) {
				return plan;
			}
		}

		// hmm, no plan returned... either the person has no plans, or the plan(s) have no score.
		if (person.getPlans().size() > 0) {
			return person.getPlans().get(0);
		}

		// this case should never happen, except a person has no plans at all.
		return null;
	}

	/**
	 * Calculates the weight of a single plan.
	 *
	 * @return the weight of the plan
	 */
	protected double calcPlanWeight(final T plan, final double maxScore) {
		// NOTE: The deduction of "maxScore" from all scores is a numerical trick.  It ensures that the values of exp(...)
		// are in some normal range, instead of close to numerical infinity.  The latter leads to numerically instable
		// results (this is not fiction; we had that some time ago). kai, aug'12

		if (plan.getScore() == null) {
			return Double.NaN;
		}
		double weight = Math.exp(this.beta * (plan.getScore() - maxScore));
		if (weight < MIN_WEIGHT) weight = MIN_WEIGHT;
		return weight;
	}

	/**
	 * Builds the weights of all plans.
	 *
	 * @return a map containing the weights of all plans
	 */
	Map<T, Double> calcWeights(final HasPlansAndId<T, ?> person) {

		// - first find the max. score of all plans of this person
		double maxScore = Double.NEGATIVE_INFINITY;
		for (T plan1 : person.getPlans()) {
			if ( (plan1.getScore() != null) && plan1.getScore().isNaN() ) {
				LogManager.getLogger(this.getClass()).error("encountering getScore().isNaN().  This class is not well behaved in this situation.  Continuing anyway ...") ;
			}
			if ((plan1.getScore() != null) && (plan1.getScore() > maxScore)) {
				maxScore = plan1.getScore();
			}
		}

		Map<T, Double> weights = new LinkedHashMap<T, Double>(person.getPlans().size());

		for (T plan : person.getPlans()) {
			weights.put(plan, this.calcPlanWeight(plan, maxScore));
			// see note in calcPlanWeight!
		}

		return weights;
	}

    /**
     * @return the probability that this expBetaPlanSelector will select this plan for this person.
     */
	public static <T extends BasicPlan, I> double getSelectionProbability(ExpBetaPlanSelector<T, I> expBetaPlanSelector, HasPlansAndId<T, ?> person, final T plan) {
		Map<T, Double> weights = expBetaPlanSelector.calcWeights(person);
		double thisWeight = weights.get(plan);

		double sumWeights = 0.0;
		for (Double weight : weights.values()) {
			sumWeights += weight;
		}

		return (thisWeight / sumWeights);
	}

}
