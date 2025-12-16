package org.matsim.api.core.v01.events.handler;

/**
 * Indicates how a DistributedEventHandler should be called by the DistributedEventsManager.
 */
public enum ProcessingMode {

	/**
	 * The DistributedEventsManager calls the event handler directly on the same process the event was generated.
	 */
	DIRECT,
	/**
	 * The DistributedEventsManager calls the event handler as a separate task. Allowing event events to be processed
	 * concurrently with the DSim and other event handlers.
	 */
	TASK
}
