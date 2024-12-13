package org.matsim.contrib.drt.optimizer.distributed;


import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

import java.util.List;

/**
 * Provides information to the optimizer.
 */
public class DrtOptimizerCommunicator implements MobsimBeforeSimStepListener {

	private final DrtNodeCommunicator comm;
	private final String mode;
	private final DrtOptimizer optimizer;

	public DrtOptimizerCommunicator(DrtNodeCommunicator comm, String mode, DrtOptimizer optimizer) {
		this.comm = comm;
		this.mode = mode;
		this.optimizer = optimizer;
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {

		List<DrtRequest> requests = comm.getRequests(mode);
		requests.forEach(optimizer::requestSubmitted);

	}

}
