package org.matsim.dsim;

import com.google.common.annotations.Beta;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;
import org.matsim.api.core.v01.LP;
import org.matsim.api.core.v01.Message;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.serialization.SerializationProvider;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This event task continuously processes event handlers on the same jvm.
 * It can only be used if no events arrive from other jvms.
 */
@Beta
public final class SingleNodeAsyncEventHandlerTask extends EventHandlerTask {


	/**
	 * Single queue for all messages.
	 */
	private final ManyToOneConcurrentLinkedQueue<Message> messages = new ManyToOneConcurrentLinkedQueue<>();

	/**
	 * Set to true when process should run.
	 */
	private final AtomicBoolean running = new AtomicBoolean(false);

	/**
	 * Future of the active task.
	 */
	private Future<?> active;

	public SingleNodeAsyncEventHandlerTask(EventHandler handler, DistributedEventsManager manager, int partition,
                                           SerializationProvider serializer) {
		super(handler, manager, partition, true);
		buildConsumers(serializer);
	}

	@Override
	public void waitAsync(boolean lastStep) throws ExecutionException, InterruptedException {
		// Only wait at the last step
		if (lastStep) {

			if (active != null)
				active.get();

			// Because the future is overwritten, one task may have not completed yet
			while (running.get()) {
				Thread.yield();
			}
		}
	}

	@Override
	public void add(Message msg) {
		messages.add(msg);
	}

	@Override
	public IntSet waitForOtherRanks(double time) {
		return LP.NO_NEIGHBORS;
	}

	@Override
	public void run() {

		long t = System.nanoTime();
		manager.setContext(partition);

		Future<?> self = future;

		if (!running.compareAndSet(false, true)) {
			return;
		}

		active = self;
		future = null;

		Message msg;
		while ((msg = messages.poll()) != null) {
			process(msg);
		}

		storeRuntime(t);
		running.set(false);
	}

}
