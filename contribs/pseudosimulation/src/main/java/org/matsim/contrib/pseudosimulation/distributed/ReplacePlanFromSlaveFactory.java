package org.matsim.contrib.pseudosimulation.distributed;

import jakarta.inject.Provider;
import java.util.HashMap;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.replanning.modules.ReplacePlanFromSlave;
import org.matsim.core.replanning.PlanStrategy;

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
