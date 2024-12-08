package org.matsim.contrib.ev.strategic.replanning.selector;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;

/**
 * This selector implementation selects the charging plan that has achieved the
 * highest charging score from the charging plan memory of the agent.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class BestChargingPlanSelector implements ChargingPlanSelector {
	@Override
	public ChargingPlan select(Person person, Plan plan, ChargingPlans chargingPlans) {
		return chargingPlans.getChargingPlans().stream().sorted((a, b) -> -Double.compare(a.getScore(), b.getScore()))
				.findFirst().get();
	}
}
