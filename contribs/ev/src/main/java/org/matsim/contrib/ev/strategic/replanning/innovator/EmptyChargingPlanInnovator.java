package org.matsim.contrib.ev.strategic.replanning.innovator;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;

/**
 * This implementation creates empty charging plans (= no charging at all).
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class EmptyChargingPlanInnovator implements ChargingPlanInnovator {
	@Override
	public ChargingPlan createChargingPlan(Person person, Plan plan, ChargingPlans chargingPlans) {
		return new ChargingPlan();
	}
}
