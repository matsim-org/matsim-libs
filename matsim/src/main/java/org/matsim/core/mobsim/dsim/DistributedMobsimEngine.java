package org.matsim.core.mobsim.dsim;

import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

/**
 * A distributed engine accepts agents and processes simulation steps.
 */
public interface DistributedMobsimEngine extends MobsimEngine {

	/**
	 * Process receives {@link SimStepMessage} and updates the engine's internal state.
	 */
	default void process(SimStepMessage stepMessage, double now) {
	}

}
