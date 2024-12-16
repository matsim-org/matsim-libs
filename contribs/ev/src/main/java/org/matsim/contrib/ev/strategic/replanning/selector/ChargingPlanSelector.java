package org.matsim.contrib.ev.strategic.replanning.selector;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;

/**
 * This interface looks at the current memory of charging plans of an agent and
 * selects one among the existing ones.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public interface ChargingPlanSelector {
	ChargingPlan select(Person person, Plan plan, ChargingPlans chargingPlans);
}
