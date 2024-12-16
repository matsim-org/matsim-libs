package org.matsim.contrib.ev.strategic.replanning.selector;

import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;
import org.matsim.core.gbl.MatsimRandom;

/**
 * This selector implementation selects a random charging plan from the charging
 * plan memory of the agent.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class RandomChargingPlanSelector implements ChargingPlanSelector {
	private final Random random;

	public RandomChargingPlanSelector() {
		this.random = MatsimRandom.getLocalInstance();
	}

	@Override
	public ChargingPlan select(Person person, Plan plan, ChargingPlans chargingPlans) {
		int planIndex = random.nextInt(chargingPlans.getChargingPlans().size());
		return chargingPlans.getChargingPlans().get(planIndex);
	}
}
