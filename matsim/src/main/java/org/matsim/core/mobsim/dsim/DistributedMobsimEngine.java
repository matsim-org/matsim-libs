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

	/**
	 * Determine the order in which engines are executed in a distributed simulation.
	 */
	default double getEnginePriority() {
		return 0.0;
	}

}
