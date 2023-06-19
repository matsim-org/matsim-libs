package org.matsim.contrib.pseudosimulation.distributed;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.replanning.modules.ReplacePlanFromSlave;
import org.matsim.core.replanning.PlanStrategy;

import jakarta.inject.Provider;
import java.util.HashMap;

class ReplacePlanFromSlaveFactory implements Provider<PlanStrategy> {

	private final HashMap<String, Plan> plans;

	public ReplacePlanFromSlaveFactory(HashMap<String, Plan> newPlans) {
		plans = newPlans;
	}

	@Override
	public PlanStrategy get() {
		return new ReplacePlanFromSlave(plans);
	}

}
