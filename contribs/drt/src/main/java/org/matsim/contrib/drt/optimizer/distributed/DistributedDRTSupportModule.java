package org.matsim.contrib.drt.optimizer.distributed;

import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;

/**
 * Contains functionality needed in a distributed setup.
 */
public class DistributedDRTSupportModule extends AbstractDvrpModeQSimModule {

	public DistributedDRTSupportModule(String mode) {
		super(mode);
	}

	@Override
	protected void configureQSim() {

		if (!getSimNode().isHeadNode()) {
			addModalComponent(DrtOptimizer.class, modalProvider(getter -> new HeadNodeOptimizer()));
		} else {


		}

	}
}
