package playground.pieter.distributed;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;

import java.util.HashMap;

class ReplacePlanFromSlaveFactory implements PlanStrategyFactory {

	private final HashMap<String, Plan> plans;

	public ReplacePlanFromSlaveFactory(HashMap<String, Plan> newPlans) {
		plans = newPlans;
	}

	@Override
	public PlanStrategy get() {
		return new ReplacePlanFromSlave(plans);
	}

}
