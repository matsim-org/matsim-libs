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
	default void cleanup() {
	}

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
	IntSet waitForOtherParts(double time);

	/**
	 * Set the current simulation time.
	 */
	void setTime(double time);

	/**
	 * Return the runtime of the task, aggregated into bins of {@link #RUNTIME_BIN_SIZE} simulation seconds.
	 *
	 * @see #addRuntime(LongList, double, long)
	 */
	LongList getRuntime();

	/**
	 * Size of the time bins in which runtimes are aggregated in simulation seconds.
	 */
	int RUNTIME_BIN_SIZE = 10;

	/**
	 * Add a runtime to the bin of the given simulation time. Bin {@code i} holds the runtimes of all executions at
	 * simulation times in {@code (RUNTIME_BIN_SIZE * (i - 1), RUNTIME_BIN_SIZE * i]}.
	 */
	static void addRuntime(LongList runtimes, double time, long runtime) {
		int bin = (int) Math.ceil(time / RUNTIME_BIN_SIZE);
		while (runtimes.size() <= bin) {
			runtimes.add(0);
		}
		runtimes.set(bin, runtimes.getLong(bin) + runtime);
	}

	/**
	 * Avg runtime over last few executions.
	 */
	float getAvgRuntime();

	/**
	 * Reset the state of the task for a new iteration.
	 */
	void resetTask(int iteration);

	record Info(String name, int partition, LongList runtime) {
	}

}
