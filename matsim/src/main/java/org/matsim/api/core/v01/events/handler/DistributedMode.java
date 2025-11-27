package org.matsim.api.core.v01.events.handler;

/**
 * Enum for distributed event handling modes.
 */
public enum DistributedMode {

    /**
     * One handler exists for the whole simulation.
     */
    GLOBAL,

    /**
     * One handler exists per partition.
     */
    PARTITION,

    /**
     * One handler processes events on each partition, but concurrently.
	 * Its state is shared between partitions.
     * Such handler is not thread-safe and must support concurrent processing of events.
     */
    PARTITION_SINGLETON,

    /**
     * One handler exists per node (jvm). Events are queued and processed sequentially.
     */
    NODE_SINGLETON,

}
