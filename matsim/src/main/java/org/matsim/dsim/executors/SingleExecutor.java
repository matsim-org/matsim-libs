package org.matsim.dsim.executors;

import com.google.inject.Inject;
import org.matsim.api.core.v01.LP;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.serialization.SerializationProvider;
import org.matsim.dsim.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * This executor runs everything on the main thread.
 */
public final class SingleExecutor implements LPExecutor {

	private final SerializationProvider serializer;

	private final List<SimTask> tasks = new ArrayList<>();

	@Inject
	public SingleExecutor(SerializationProvider serializer) {
		this.serializer = serializer;
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
	}

	@Override
	public void doSimStep(double time) {
		for (SimTask task : tasks) {
			task.setTime(time);
			if (task.needsExecution()) {
				task.beforeExecution();
				task.run();
			}
		}
	}

	@Override
	public void afterSim() {
		tasks.forEach(SimTask::cleanup);
	}

	@Override
	public void runEventHandler() {
		for (SimTask task : tasks) {
			if (task instanceof EventHandlerTask) {
				task.beforeExecution();
				task.run();
			}
		}
	}
}
