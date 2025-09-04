package org.matsim.dsim.executors;

import org.matsim.core.events.handler.EventHandler;
import org.matsim.api.core.v01.LP;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.dsim.DistributedEventsManager;
import org.matsim.dsim.EventHandlerTask;
import org.matsim.dsim.LPTask;
import org.matsim.dsim.SimTask;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public interface LPExecutor extends Steppable {

    /**
     * Add an {@link LP} to the executor.
     */
    LPTask register(LP lp, DistributedEventsManager manager, int part);

    /**
     * Add an {@link EventHandler} to the executor.
     */
    EventHandlerTask register(EventHandler handler, DistributedEventsManager manager,
                              int part, int totalParts,
                              @Nullable AtomicInteger counter);


	/**
	 * Runs all event handlers tasks.
	 */
	void runEventHandler();

	/**
	 * Executed after the simulation has run.
	 */
	void afterSim();

    /**
     * Remove a task from the execution loop.
     */
    void deregister(SimTask task);

    /**
     * Process the runtimes of all tasks.
     */
    void processRuntimes(Consumer<SimTask.Info> f);


    void shutdown();
}
