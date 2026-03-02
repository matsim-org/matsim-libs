package org.matsim.api.core.v01.events.handler;

/**
 * Defines how a {@link DistributedEventHandler} should be synced, when executed as a separate Task
 */
public enum BlockingMode {

	/*
	 * Never wait for this event handler to finish before continuing the simulation. No event handler supports this yet.
	 * Therefore, disable it
	 */
	//NEVER,

	/**
	 * Wait until the event handler has finished processing all events before advancing the simulation to the next time step
	 */
	SIM_STEP,

	/**
	 * Wait until the event handler has finished processing all events before invoking this event handler again. For example,
	 * if the event handler requests execution every 900 seconds, the entire simulation will wait until this handler finishes
	 * the entire simulation must wait at time step 1800 until the handler has finished processing all events from the last
	 * time it was invoked at time step 900.
	 */
	ASYNC
}
