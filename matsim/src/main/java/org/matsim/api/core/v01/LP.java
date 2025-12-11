package org.matsim.api.core.v01;

import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * A logical process, which encapsulates the state of a particular entity.
 * LPs are directly affected by events, and can cause change to other
 * parts of the system by “sending” events.
 */
public interface LP {

    /**
     * Special value to indicate that no neighbors should be waited for.
     */
    IntSet NO_NEIGHBORS = IntSet.of();

    /**
     * Special value to indicate that all neighbors should be waited for.
     */
    IntSet ALL_NODES_BROADCAST = IntSet.of(-1);

    /**
     * This method is called when the simulation is started.
     */
    default void onPrepareSim() {
    }

	/**
	 * Called after all simulation processes are finished.
	 */
	default void onCleanupSim() {
	}

    /**
     * This method is used to determine when the LP should wait for neighboring messages.
     */
    default IntSet waitForOtherRanks(double time) {
        return NO_NEIGHBORS;
    }
}
