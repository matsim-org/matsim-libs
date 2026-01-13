package org.matsim.dsim;

import it.unimi.dsi.fastutil.ints.IntSet;
import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;
import org.matsim.api.core.v01.LP;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.MessageComparator;
import org.matsim.api.core.v01.events.handler.BlockingMode;
import org.matsim.api.core.v01.events.handler.DistributedEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.serialization.SerializationProvider;
import org.matsim.dsim.events.EventMessagingPattern;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This task is able to run any event handler, regardless of its specification.
 */
@SuppressWarnings("rawtypes")
public final class DefaultEventHandlerTask extends EventHandlerTask {

	/**
	 * Pattern to use for messaging.
	 */
	@Nullable
	private final EventMessagingPattern pattern;

	private final DistributedEventsManager em;

	/**
	 * Total number partitions this event handler is running on. (on this node)
	 */
	private final int totalPartitions;

	/**
	 * Counter that is shared between all event handlers of the same type.
	 */
	private final AtomicInteger counter;

	/**
	 * Buffer holding incoming messages. These are switched between iterations.
	 */
	private final ManyToOneConcurrentLinkedQueue<Message> queueOdd = new ManyToOneConcurrentLinkedQueue<>();
	private final ManyToOneConcurrentLinkedQueue<Message> queueEven = new ManyToOneConcurrentLinkedQueue<>();

	/**
	 * Intermediate list of message.
	 */
	private final List<Message> messages = new ArrayList<>();


	/**
	 * Switching phase for the queues.
	 */
	private final AtomicBoolean phase = new AtomicBoolean();

	private int cleanupSync = -1;


	public DefaultEventHandlerTask(EventHandler handler, int partition, int totalPartitions,
								   DistributedEventsManager em, SerializationProvider serializer,
								   @Nullable AtomicInteger counter) {
		super(handler, partition, supportsAsync(handler));
		this.totalPartitions = totalPartitions;
		this.counter = counter;
		this.pattern = buildConsumers(serializer, em.getComputeNode().isDistributed());
		this.em = em;

		if (pattern != null && async)
			throw new IllegalArgumentException("Message pattern and async execution together are not supported.");
	}

	/**
	 * Wait for async task to finish (if set).
	 */
	public void waitAsync(boolean lastStep) throws ExecutionException, InterruptedException {
		if (future != null)
			future.get();

		future = null;
	}

	@Override
	public void beforeExecution() {
		phase.set(!phase.get());
	}

	public void add(Message msg) {
		ManyToOneConcurrentLinkedQueue<Message> queue = phase.get() ? queueOdd : queueEven;
		queue.add(msg);
	}

	public IntSet waitForOtherRanks(double time) {

		if (pattern != null && (isSyncTime() || cleanupSync > 0)) {
			return pattern.waitForOtherRanks(time);
		}

		return LP.NO_NEIGHBORS;
	}

	@Override
	public void cleanup() {
		super.cleanup();
		this.cleanupSync = 2;
	}

	@Override
	public void resetTask(int iteration) {
		super.resetTask(iteration);
		this.cleanupSync = -1;
	}

	@Override
	public void run() {
		long t = System.nanoTime();

		em.setContext(partition);
		ManyToOneConcurrentLinkedQueue<Message> queue = phase.get() ? queueEven : queueOdd;
		Message msg;

		// Sort events if needed
		if (needsSorting) {
			while ((msg = queue.poll()) != null) {
				messages.add(msg);
			}

			messages.sort(MessageComparator.INSTANCE);

			for (Message m : messages) {
				try {
					process(m);
				} catch (Exception e) {
					dumpEvents(Path.of("debug_event_dump.xml"), messages);
					throw new RuntimeException("Error in %s processing message: %s".formatted(getName(), m), e);
				}
			}

//			dumpEvents(Path.of("event_dump_%s_%.0f.xml".formatted(getName(), time)), messages);

			messages.clear();
			needsSorting = false;
		} else
			while ((msg = queue.poll()) != null) {
				process(msg);
			}

		if (pattern != null) {
			// always process in the inbox
			pattern.process(handler);
			if (shouldThisTaskSend() && (isSyncTime() || cleanupSync > 1)) {
				// sometimes send messages to other handlers
				pattern.communicate(broker, handler);
				// only send messages once on cleanup
				cleanupSync -= 1;
			}
		}

		// In the case of a NODE_CONCURRENT handler, we have n tasks, but only one handler, which collects all the data on one node.
		// The task which reaches this point last should perform the communication with other nodes.
//			if (counter != null && counter.incrementAndGet() == totalPartitions) {
//				pattern.communicate(broker, handler);
//				pattern.process(handler);
//				counter.set(0);
//			} else {
//				pattern.communicate(broker, handler);
//				pattern.process(handler);
//			}

		// only run cleanup once.
//		if (isCleanUp) {
//			isCleanUp = false;
//		}
		storeRuntime(t);
	}

	/**
	 * Whether the handler supports async execution.
	 */
	public static boolean supportsAsync(EventHandler handler) {
		DistributedEventHandler annotation = handler.getClass().getAnnotation(DistributedEventHandler.class);
		return annotation != null && !annotation.blocking().equals(BlockingMode.SIM_STEP);
	}

	private boolean shouldThisTaskSend() {
		if (counter == null) return true;
		// In the case of a NODE_CONCURRENT handler, we have n tasks, but only one handler, which collects all the data on one node.
		// The task which reaches this point last should perform the communication with other nodes.
		if (counter.incrementAndGet() == totalPartitions) {
			counter.set(0);
			return true;
		} else {
			return false;
		}
	}
}
