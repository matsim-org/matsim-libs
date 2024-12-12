package org.matsim.contrib.drt.optimizer.distributed;

import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

/**
 * Optimizer that sends request to the head node and receives the optimized plan.
 */
public class HeadNodeOptimizer implements DrtOptimizer {

	@Override
	public void requestSubmitted(Request request) {

	}

	@Override
	public void nextTask(DvrpVehicle vehicle) {

	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {

	}
}
