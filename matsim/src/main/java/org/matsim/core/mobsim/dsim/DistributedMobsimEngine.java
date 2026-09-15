package org.matsim.core.mobsim.dsim;

import org.matsim.api.core.v01.Message;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.Map;

/**
 * A distributed engine accepts agents and processes simulation steps.
 */
public interface DistributedMobsimEngine extends MobsimEngine, DSimComponentsMessageProcessor {

	default Map<Class<? extends Message>, MessageHandler> getMessageHandlers() {
		return Map.of();
	}

	/**
	 * Determine the order in which engines are executed in a distributed simulation.
	 */
	default double getEnginePriority() {
		return 0.0;
	}

}
