package org.matsim.api.core.v01.events.handler;

import org.matsim.dsim.DistributedEventsManager;

/**
 * Enum for distributed event handling modes. The {@link DistributedEventsManager} decides how many event handlers are created depending on
 * this value.
 */
public enum DistributedMode {

	/**
	 * One handler exists for the whole simulation. In the case of a distributed setup, the handler is located on node 0. This means that all
	 * events will be sent to this node, potentially causing a lot of network traffic.
	 */
	GLOBAL,

	/**
	 * One handler exists per node (jvm). All events generated on a compute node are sent to this handler. In a single node setup, this results
	 * in the same behavior as GLOBAL. Events are processed sequentially in the order with which they are processed by the events manager. This
	 * means the handler implementation can pretend that everything is single threaded.
	 */
	NODE,

	/**
	 * One handler exists per node (jvm) as with NODE. Other than NODE, events are passed to this event handler concurrently. This means the
	 * implementation has to take care of thread safety. Potentially, this mode is faster than NODE, as simulation processes place events in
	 * separate event queues. Also, simulation processes can call NODE_CONCURRENT event handlers directly.
	 */
	NODE_CONCURRENT,

	/**
	 * One exists per partition. All events generated on a simulation partition are sent to this handler.
	 */
	PARTITION,
}
