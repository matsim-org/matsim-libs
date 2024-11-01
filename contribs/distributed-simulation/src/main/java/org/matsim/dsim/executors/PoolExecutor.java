package org.matsim.dsim.executors;

import com.google.inject.Inject;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.LP;
import org.matsim.api.core.v01.messages.SimulationNode;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.serialization.SerializationProvider;
import org.matsim.dsim.DistributedEventsManager;
import org.matsim.dsim.EventHandlerTask;
import org.matsim.dsim.LPTask;
import org.matsim.dsim.SimTask;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Log4j2
public final class PoolExecutor implements LPExecutor {

    private final ExecutorService executor;
    private final SerializationProvider serializer;
    private final List<Future<?>> executions = new LinkedList<>();
    private final List<SimTask> tasks = new ArrayList<>();
    private int step;

    @Inject
    public PoolExecutor(SerializationProvider serializer, SimulationNode node) {
        this.serializer = serializer;
        this.executor = Executors.newWorkStealingPool(node.getCores());
    }

    /**
     * Simple modulo operation that works for powers of 2.
     */
    private static int mod(int x, int y) {
        return (x & (y - 1));
    }

    @Override
    public LPTask register(LP lp, DistributedEventsManager manager, int part) {
        LPTask task = new LPTask(lp, part, manager, serializer);
        tasks.add(task);
        return task;
    }

    @Override
    public EventHandlerTask register(EventHandler handler, DistributedEventsManager manager, int part, int totalParts, AtomicInteger counter) {
        EventHandlerTask task = new EventHandlerTask(handler, part, totalParts, manager, serializer, counter);
        tasks.add(task);
        return task;
    }

    @Override
    public void deregister(SimTask task) {
        tasks.remove(task);
    }

    @Override
    public void processRuntimes(Consumer<SimTask.Info> f) {
        for (SimTask task : tasks) {
            f.accept(new SimTask.Info(task.getName(), task.getPartition(), task.getRuntime()));
        }
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public void doSimStep(double time) {

        // Sort by descending runtime for better load distribution
        if (mod(step++, 128) == 0)
            tasks.sort((o1, o2) -> -Float.compare(o1.getAvgRuntime(), o2.getAvgRuntime()));

        // Prepare all tasks before executing them
        for (SimTask task : tasks) {
            task.setTime(time);
            if (task.needsExecution())
                task.beforeExecution();
        }

        for (SimTask task : tasks) {
            if (task.needsExecution())
                executions.add(executor.submit(task));
        }

        for (Future<?> execution : executions) {
            try {
                execution.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error while executing task", e);
                executor.shutdown();
            }
        }

        executions.clear();
    }

	@Override
	public void runEventHandler() {
		for (SimTask task : tasks) {
			if (task instanceof EventHandlerTask) {
				task.beforeExecution();
			}
		}

		// These need to be two loops because of concurrency

		for (SimTask task : tasks) {
			if (task instanceof EventHandlerTask) {
				executions.add(executor.submit(task));
			}
		}

		for (Future<?> execution : executions) {
			try {
				execution.get();
			} catch (InterruptedException | ExecutionException e) {
				log.error("Error while executing task", e);
				executor.shutdown();
			}
		}

		executions.clear();
	}
}
