package org.matsim.contrib.ev.strategic.replanning.innovator;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;

/**
 * This interface represents an innovator for charging plans. It obtains the
 * current charging plan (or null if none) and returns a new charging plan that
 * is updated or created from scratch.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public interface ChargingPlanInnovator {
	ChargingPlan createChargingPlan(Person person, Plan plan, ChargingPlans chargingPlans);
}
