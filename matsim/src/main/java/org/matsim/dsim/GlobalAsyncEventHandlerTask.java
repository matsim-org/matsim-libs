package org.matsim.dsim;

import com.google.common.annotations.Beta;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;
import org.matsim.api.core.v01.LP;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.MessageComparator;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.serialization.SerializationProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This event task runs async over the whole simulation and is only waited for at the very end.
 */
@Beta
public final class GlobalAsyncEventHandlerTask extends EventHandlerTask {

	/**
	 * First counter holds current sim step.
	 */
	private static final long COUNTER1_MASK = 0xFFFFFFFL; // 28 bits

	/**
	 * Second counter holds current process step.
	 */
	private static final long COUNTER2_MASK = 0xFFFFFFFL; // 28 bits
	private static final long STATE_MASK = 0xFFL; // 8 bits

	private static final int COUNTER1_SHIFT = 36; // 28 + 8
	private static final int COUNTER2_SHIFT = 8;  // 8

	private static final byte STATE_IDLE = 0;
	private static final byte STATE_PROCESSING = 1;

	/**
	 * Single queue for all messages.
	 */
	private final ManyToOneConcurrentLinkedQueue<Message> backlog = new ManyToOneConcurrentLinkedQueue<>();

	/**
	 * Intermediate list of message.
	 */
	private final List<Message> messages = new ArrayList<>();

	/**
	 * Stores the state as (sim step, process step, running/idle).
	 */
	private final AtomicLong state = new AtomicLong();

	public GlobalAsyncEventHandlerTask(EventHandler handler, DistributedEventsManager manager, int partition,
									   SerializationProvider serializer) {
		super(handler, manager, partition, true);
		buildConsumers(serializer);
	}

	private boolean updateState(long expected, int simCounter, int processCounter, byte byteValue) {
		long newState = ((long) simCounter << COUNTER1_SHIFT) |
			((long) processCounter << COUNTER2_SHIFT) |
			(byteValue & STATE_MASK);

		return this.state.compareAndSet(expected, newState);
	}

	@Override
	public void beforeExecution() {

		while (true) {
			long state = this.state.get();

			int simCounter = (int) ((state >> COUNTER1_SHIFT) & COUNTER1_MASK);
			int processCounter = (int) ((state >> COUNTER2_SHIFT) & COUNTER2_MASK);
			byte running = (byte) (state & STATE_MASK);

			if (updateState(state, simCounter + 1, processCounter, running)) {
				break;
			}
		}
	}

	@Override
	public void waitAsync(boolean lastStep) throws ExecutionException, InterruptedException {
		// Only wait at the last step
		if (lastStep && future != null)
			future.get();

		future = null;
	}

	@Override
	public void add(Message msg) {
		backlog.add(msg);
	}

	@Override
	public IntSet waitForOtherRanks(double time) {
		return LP.NO_NEIGHBORS;
	}

	@Override
	public void run() {

		long t = System.nanoTime();
		manager.setContext(partition);

		while (true) {

			long state = this.state.get();
			int simCounter = (int) ((state >> COUNTER1_SHIFT) & COUNTER1_MASK);
			int processCounter = (int) ((state >> COUNTER2_SHIFT) & COUNTER2_MASK);
			byte running = (byte) (state & STATE_MASK);

			if (running == STATE_IDLE && processCounter < simCounter) {
				if (updateState(state, simCounter, processCounter + 1, STATE_PROCESSING)) {
					state = runStep();

					simCounter = (int) ((state >> COUNTER1_SHIFT) & COUNTER1_MASK);
					processCounter = (int) ((state >> COUNTER2_SHIFT) & COUNTER2_MASK);

					// If sim and process are equal exit the task
					if (processCounter == simCounter) {
						needsSorting = false;
						storeRuntime(t);
						return;
					}
				}
			} else {
				break;
			}
		}
	}


	private long runStep() {

		Message msg;
		while ((msg = backlog.poll()) != null) {
			messages.add(msg);
		}

		// Sort events if needed
		if (needsSorting)
			messages.sort(MessageComparator.INSTANCE);

//		System.out.println("Processing: " +  messages.size());

		for (Message message : messages) {
			process(message);
		}

		messages.clear();

		while (true) {

			long state = this.state.get();

			int simCounter = (int) ((state >> COUNTER1_SHIFT) & COUNTER1_MASK);
			int processCounter = (int) ((state >> COUNTER2_SHIFT) & COUNTER2_MASK);
			if (updateState(state, simCounter, processCounter, STATE_IDLE)) {
				return state;
			}
		}
	}

}
