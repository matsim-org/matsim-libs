package org.matsim.contrib.ev.strategic.replanning.selector;

import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.common.util.WeightedRandomSelection;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;
import org.matsim.core.gbl.MatsimRandom;

/**
 * This selector implementation selects an existing charging plan from an
 * agent's charging plan memory based on the plans' scores and the logit
 * formula. Plans with a higher score, will, hence, be selected with higher
 * probability.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ExponentialChargingPlanSelector implements ChargingPlanSelector {
	private final double beta;
	private final Random random;

	public ExponentialChargingPlanSelector(double beta) {
		this.beta = beta;
		this.random = MatsimRandom.getLocalInstance();
	}

	@Override
	public ChargingPlan select(Person person, Plan plan, ChargingPlans chargingPlans) {
		double maximum = Double.NEGATIVE_INFINITY;

		for (ChargingPlan chargingPlan : chargingPlans.getChargingPlans()) {
			if (Double.isFinite(chargingPlan.getScore())) {
				maximum = Math.max(maximum, chargingPlan.getScore());
			}
		}

		if (Double.isFinite(maximum)) {
			WeightedRandomSelection<ChargingPlan> selector = new WeightedRandomSelection<>(random);

			for (ChargingPlan chargingPlan : chargingPlans.getChargingPlans()) {
				selector.add(chargingPlan, Math.exp(beta * (chargingPlan.getScore() - maximum)));
			}

			return selector.select();
		} else {
			return new RandomChargingPlanSelector().select(person, plan, chargingPlans);
		}
	}
}
