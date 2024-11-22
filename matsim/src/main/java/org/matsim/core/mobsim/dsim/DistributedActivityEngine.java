package org.matsim.core.mobsim.dsim;

import org.matsim.core.mobsim.qsim.ActivityEngine;

/**
 * Interface for handling activities of agents.
 * This interface indicates that the activity handler is safe for use in distributed simulations.
 *
 * @see ActivityEngine
 */
public interface DistributedActivityEngine extends ActivityEngine, DistributedMobsimEngine {

	/**
	 * Activity engines are sorted by their priority in descending order.
	 */
	default double priority() {
		return 0.0;
	}

}
