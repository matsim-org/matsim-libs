package playground.pieter.distributed;

import java.util.HashMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;

class ReplacePlanFromSlaveFactory implements PlanStrategyFactory {

	private final HashMap<String, Plan> plans;

	public ReplacePlanFromSlaveFactory(HashMap<String, Plan> newPlans) {
		plans = newPlans;
	}

	@Override
	public PlanStrategy createPlanStrategy(Scenario scenario,
			EventsManager eventsManager) {
		return new ReplacePlanFromSlave(plans);
	}

}
