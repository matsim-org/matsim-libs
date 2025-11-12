package org.matsim.dsim;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongList;
import org.matsim.api.core.v01.LP;
import org.matsim.api.core.v01.Message;
import org.matsim.core.events.handler.EventHandler;

/**
 * Internal interface for processes / tasks that can be executed in parallel.
 * Such task can either execute an {@link LP} or an {@link EventHandler}.
 */
public sealed interface SimTask extends Runnable permits LPTask, EventHandlerTask {

	/**
	 * Get the name of the task.
	 */
	String getName();

	/**
	 * Get the partition number.
	 */
	int getPartition();

	/**
	 * Check if the task needs to be executed.
	 */
	default boolean needsExecution() {
		return true;
	}

	/**
	 * Called on all tasks before they are scheduled for execution, but only if {@link #needsExecution()} returns true.
	 */
	default void beforeExecution() {
	}

	/**
	 * Perform cleanup after the simulation has finished.
	 */
	default void cleanup() {}

	/**
	 * Add a message to the task.
	 */
	void add(Message msg);

	/**
	 * Get the supported message types.
	 */
	IntSet getSupportedMessages();

	/**
	 * Wait for messages from other ranks.
	 */
	IntSet waitForOtherRanks(double time);

	/**
	 * Set the current simulation time.
	 */
	void setTime(double time);

	/**
	 * Return the runtime of the task.
	 */
	LongList getRuntime();

	/**
	 * Avg runtime over last few executions.
	 */
	float getAvgRuntime();

    record Info(String name, int partition, LongList runtime) {
    }

}
