package org.matsim.dsim.executors;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.LP;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.serialization.SerializationProvider;
import org.matsim.dsim.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class PoolExecutor implements LPExecutor {

	private static final Logger log = LogManager.getLogger(PoolExecutor.class);

	private final ExecutorService executor;
	private final SerializationProvider serializer;

	/**
	 * Executions from the current sim step.
	 */
	private final List<Future<?>> executions = new LinkedList<>();

	private final List<SimTask> tasks = new ArrayList<>();
	private int step;

	@Inject
	public PoolExecutor(SerializationProvider serializer, DSimConfigGroup config) {
		this.serializer = serializer;
		this.executor = Executors.newWorkStealingPool(config.getThreads() == 0 ? Runtime.getRuntime().availableProcessors() : config.getThreads());
	}

	/**
	 * Simple modulo operation that works for powers of 2.
	 */
	private static int mod128(int x) {
		return (x & (128 - 1));
	}

	@Override
	public LPTask register(LP lp, DistributedEventsManager manager, int part) {
		LPTask task = new LPTask(lp, part, manager, serializer);
		tasks.add(task);
		return task;
	}

	@Override
	public EventHandlerTask register(EventHandler handler, DistributedEventsManager manager, int part, int totalParts, AtomicInteger counter) {
		EventHandlerTask task = new DefaultEventHandlerTask(handler, part, totalParts, manager, serializer, counter);

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
		if (mod128(step++) == 0)
			tasks.sort((o1, o2) -> -Float.compare(o1.getAvgRuntime(), o2.getAvgRuntime()));

		// Prepare all tasks before executing them
		for (SimTask task : tasks) {
			task.setTime(time);
			if (task.needsExecution()) {
				waitForTask(task, false);
				task.beforeExecution();
			}
		}

		for (SimTask task : tasks) {
			if (task.needsExecution()) {
				Future<?> ft = executor.submit(task);
				if (task instanceof EventHandlerTask et && et.isAsync()) {
					et.setFuture(ft);
				} else
					executions.add(ft);
			}
		}

		for (Future<?> execution : executions) {
			try {
				execution.get();
			} catch (InterruptedException | ExecutionException e) {
				log.error("Error while executing task", e);
				executor.shutdown();
				throw new RuntimeException(e.getCause());
			}
		}

		executions.clear();
	}

	@Override
	public void afterSim() {
		// Execute sequentially
		tasks.forEach(SimTask::cleanup);
	}

	@Override
	public void runEventHandler() {
		for (SimTask task : tasks) {

			if (task instanceof EventHandlerTask) {
				waitForTask(task, true);
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
				log.error("Error while executing event handler", e);
				executor.shutdown();
				throw new RuntimeException(e.getCause());
			}
		}

		executions.clear();
	}

	private void waitForTask(SimTask task, boolean lastStep) {
		// Wait for async event handlers
		if (task instanceof EventHandlerTask et) {
			try {
				et.waitAsync(lastStep);
			} catch (InterruptedException | ExecutionException e) {
				log.error("Error while executing async event task", e);
				executor.shutdown();
				throw new RuntimeException(e.getCause());
			}
		}
	}
}
