package org.matsim.core.mobsim.dsim;

import org.matsim.api.core.v01.Message;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.List;
import java.util.Map;

/**
 * A distributed engine accepts agents and processes simulation steps.
 */
public interface DistributedMobsimEngine extends MobsimEngine {

	/**
	 * Returns a map of message type IDs to handlers that process batches of incoming messages of that type.
	 * Called once during engine registration in {@link org.matsim.dsim.simulation.SimProcess}.
	 * Engines that receive no inter-partition messages can leave this as the default empty map.
	 */
	default Map<Integer, MessageHandler> getMessageHandlers() {
		return Map.of();
	}

	/**
	 * Determine the order in which engines are executed in a distributed simulation.
	 */
	default double getEnginePriority() {
		return 0.0;
	}

	@FunctionalInterface
	interface MessageHandler {
		void handle(List<Message> messages, double now);
	}

}
