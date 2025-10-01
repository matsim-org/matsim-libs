package org.matsim.api.core.v01.events;

/**
 * THis enum defines the source of an event in a distributed simulation.
 */
public enum EventSource {

	/**
	 * Event generated anywhere in the simulation.
	 */
    GLOBAL,
	/**
	 * Event generated on the same node.
	 */
    NODE,
	/**
	 * Event generated on the same partition.
	 */
    PARTITION,

}
