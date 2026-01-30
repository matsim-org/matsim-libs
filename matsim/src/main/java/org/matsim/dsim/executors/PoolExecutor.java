package org.matsim.dsim.executors;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.LP;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.serialization.SerializationProvider;
import org.matsim.dsim.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class PoolExecutor implements LPExecutor {

	private static final Logger log = LogManager.getLogger(PoolExecutor.class);

	//private final ExecutorService executor;
	private final BusyThreadpool executor;
	private final SerializationProvider serializer;

	/**
	 * Executions from the current sim step.
	 */
	private final List<Future<?>> executions = new LinkedList<>();

	private final List<EventHandlerTask> eventHandlerTasks = new ArrayList<>();
	private final List<LPTask> lpTasks = new ArrayList<>();
	private final List<SimTask> allTasks = new ArrayList<>();
	private int step;

	@Inject
	public PoolExecutor(SerializationProvider serializer, DSimConfigGroup config) {
		this.serializer = serializer;
		var size = config.getThreads() == 0 ? Runtime.getRuntime().availableProcessors() : config.getThreads();
		//this.executor = Executors.newFixedThreadPool(config.getThreads() == 0 ? Runtime.getRuntime().availableProcessors() : config.getThreads());
		log.info("Creating PoolExecutor with {} threads. Using BusyPool.", size);
		//this.executor = Executors.newFixedThreadPool(size);
		this.executor = new BusyThreadpool(size);
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
		lpTasks.add(task);
		allTasks.add(task);
		return task;
	}

	@Override
	public EventHandlerTask register(EventHandler handler, DistributedEventsManager em, int part, int totalParts, AtomicInteger counter) {
		EventHandlerTask task = new DefaultEventHandlerTask(handler, part, totalParts, em, serializer, counter);
		eventHandlerTasks.add(task);
		allTasks.add(task);
		return task;
	}

	@Override
	public void deregister(SimTask task) {
		if (task instanceof LPTask lpt) {
			lpTasks.remove(lpt);
		} else if (task instanceof EventHandlerTask et) {
			eventHandlerTasks.remove(et);
		}
		allTasks.remove(task);
	}

	@Override
	public void processRuntimes(Consumer<SimTask.Info> f) {
		for (var task : allTasks) {
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
			lpTasks.sort((o1, o2) -> -Float.compare(o1.getAvgRuntime(), o2.getAvgRuntime()));

		// Prepare all tasks before executing them
		for (SimTask task : allTasks) {
			task.setTime(time);
			if (task.needsExecution()) {
				waitForTask(task, false);
				task.beforeExecution();
			}
		}

		// submit lp tasks in bulk
		var lpFuture = executor.submitAll(lpTasks);
		executions.add(lpFuture);

		for (EventHandlerTask task : eventHandlerTasks) {
			if (task.needsExecution()) {
				submitEventHandlerTask(task, time);
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

	public void submitEventHandlerTask(EventHandlerTask task, double time) {
		Future<?> ft = executor.submit(task);
		if (task.isAsync()) {
			task.setFuture(ft);
		} else
			executions.add(ft);
	}

	@Override
	public void afterSim() {
		// Execute sequentially
		allTasks.forEach(SimTask::cleanup);
	}

	@Override
	public void runEventHandler() {
		for (var task : eventHandlerTasks) {
			waitForTask(task, true);
			task.beforeExecution();
		}

		// These need to be two loops because of concurrency

		for (var task : eventHandlerTasks) {
			executions.add(executor.submit(task));
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
		try {
			if (task instanceof EventHandlerTask et)
				et.waitAsync(lastStep);
		} catch (InterruptedException | ExecutionException e) {
			log.error("Error while executing async event task", e);
			executor.shutdown();
			throw new RuntimeException(e.getCause());
		}
	}
}
