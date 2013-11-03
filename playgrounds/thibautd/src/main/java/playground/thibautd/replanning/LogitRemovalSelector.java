/* *********************************************************************** *
 * project: org.matsim.*
 * LogitRemovalSelector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.replanning;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * A selector wich selects a plan according to the probability for its score to
 * be the lowest score in the agent's DB, according to a multinimial logit model.
 * </br>
 * It suffers from the same (important) problems as the logit plan changer:
 * as error terms are considered independent and the DB contains very similar plans,
 * we are in an obvious case of the "red bus/blue bus" paradox, where adding
 * identical plans to the choice set increases the probability that one of those
 * plans is selected.
 * </br>
 * It may however be better than removing the worst plan, as it ensures some diversity
 * preservation. Which plan selector has to be used at replanning is not clear
 * (uniform random or logit?).
 *
 * @author thibautd
 */
public class LogitRemovalSelector implements PlanSelector {

	protected final double beta;

	public LogitRemovalSelector(final Controler c) {
		this.beta = c.getConfig().planCalcScore().getBrainExpBeta();
	}

	/**
	 * @return Returns a random plan from the person, random but according to its weight.
	 */
	@Override
	public Plan selectPlan(final HasPlansAndId<Plan> person) {

		// get the weights of all plans
		Map<Plan, Double> weights = this.calcWeights(person);

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

		// hmm, no plan returned... either the person has no plans, or the plan(s) have no score.
		if (person.getPlans().size() > 0) {
			return person.getPlans().get(0);
		}

		// this case should never happen, except a person has no plans at all.
		throw new RuntimeException( "no plan selected for person "+person );
	}

	/**
	 * Builds the weights of all plans.
	 *
	 * @param person
	 * @return a map containing the weights of all plans
	 */
	private Map<Plan, Double> calcWeights(final HasPlansAndId<Plan>  person) {

		// - first find the max. score of all plans of this person
		double maxScore = Double.NEGATIVE_INFINITY;
		for (Plan plan1 : person.getPlans()) {
			if ((plan1.getScore() != null) && (plan1.getScore().doubleValue() > maxScore)) {
				maxScore = plan1.getScore().doubleValue();
			}
		}

		Map<Plan, Double> weights = new LinkedHashMap<Plan, Double>(person.getPlans().size());

		for (Plan plan : person.getPlans()) {
			weights.put(plan, Math.exp( -plan.getScore().doubleValue() ) );
		}

		return weights;
	}
}
