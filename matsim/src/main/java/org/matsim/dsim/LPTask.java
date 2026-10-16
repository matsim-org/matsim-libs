package org.matsim.dsim;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;
import org.matsim.api.core.v01.LP;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.MessageProcessor;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.serialization.MessageTypeRegistry;
import org.matsim.dsim.simulation.SimStepMessage;

import java.lang.invoke.LambdaConversionException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class LPTask implements SimTask {

	/**
	 * LP to execute.
	 */
	private final LP lp;
	private final Steppable steppable;
	/**
	 * Partition number.
	 */
	private final int partition;

	/**
	 * Events manager for this task.
	 */
	private final DistributedEventsManager manager;

	/**
	 * Order in which the messages of one time step are processed. The buffer below is filled by the other partitions' threads, so the order
	 * in which they arrive is thread timing and differs between runs - and that order decides outcomes, because processing a
	 * {@link SimStepMessage} is what hands vehicles over to this partition's links: two vehicles handed to the same link in the same time
	 * step take their positions in its queue in the order their batches are processed.
	 * <p>
	 * Ordering by sending partition and then by message type is canonical: each sender contributes at most one {@link SimStepMessage} per
	 * type and time step, and the messages inside a batch are already in the order the sender collected them. The sort is stable, so any
	 * message that does not carry a sender keeps its arrival position relative to its equals.
	 */
	private static final Comparator<Message> DELIVERY_ORDER = Comparator
		.comparingInt((Message msg) -> msg instanceof SimStepMessage batch ? batch.sender() : Integer.MIN_VALUE)
		.thenComparingInt(Message::getType);

	/**
	 * Buffer holding incoming messages. These are switched between iterations.
	 */
	private final ManyToOneConcurrentLinkedQueue<Message> queueOdd = new ManyToOneConcurrentLinkedQueue<>();
	private final ManyToOneConcurrentLinkedQueue<Message> queueEven = new ManyToOneConcurrentLinkedQueue<>();

	/**
	 * The messages of the current time step, drained from the buffer so that they can be ordered before processing. Only ever touched by
	 * the thread executing this task, and reused to keep the step loop free of allocations.
	 */
	private final List<Message> incoming = new ArrayList<>();

	/**
	 * This maps the messages types to corresponding function on the LP used for processing.
	 */
	private final Int2ObjectMap<Consumer<Message>> consumers = new Int2ObjectOpenHashMap<>();

	/**
	 * Runtimes of each iteration.
	 */
	private final LongList runtimes = new LongArrayList();

	/**
	 * Avg. runtime of last iterations.
	 */
	private float avgRuntime = 0.0f;

	/**
	 * Indicates whether the LP has been initialized.
	 */
	private boolean initialized = false;

	/**
	 * Current simulation time. Needs to be volatile to ensure visibility across threads.
	 */
	private volatile double time;

	/**
	 * Switching phase for the queues.
	 */
	private final AtomicBoolean phase = new AtomicBoolean(true);

	public LPTask(LP lp, int partition, DistributedEventsManager manager, MessageTypeRegistry registry) {
		this.lp = lp;
		this.steppable = lp instanceof Steppable s ? s : null;
		this.partition = partition;
		this.manager = manager;

		buildConsumers(registry);
	}

	@Override
	public String getName() {
		return lp.getClass().getSimpleName();
	}

	@Override
	public int getPartition() {
		return partition;
	}

	@Override
	public boolean needsExecution() {
		return SimTask.super.needsExecution();
	}

	@SuppressWarnings("unchecked")
	private void buildConsumers(MessageTypeRegistry registry) {

		for (Class<?> ifType : lp.getClass().getInterfaces()) {
			if (MessageProcessor.class.isAssignableFrom(ifType)) {
				Method[] methods = ifType.getDeclaredMethods();

				Class<?> msgType = methods[0].getParameterTypes()[0];
				int type = registry.getType(msgType);

				try {
					consumers.put(type, (Consumer<Message>) LambdaUtils.createConsumer(lp, msgType, "process"));
				} catch (LambdaConversionException | ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public void beforeExecution() {
		phase.set(!phase.get());
	}

	/**
	 * Adds a message to the buffer.
	 */
	public void add(Message msg) {
		ManyToOneConcurrentLinkedQueue<Message> queue = phase.get() ? queueOdd : queueEven;
		queue.add(msg);
	}

	/**
	 * Return the set of supported messages.
	 */
	public IntSet getSupportedMessages() {
		return consumers.keySet();
	}

	public IntSet waitForOtherParts(double time) {
		return lp.waitForOtherParts(time);
	}

	@Override
	public void setTime(double time) {
		this.time = time;
	}

	@Override
	public LongList getRuntime() {
		return runtimes;
	}

	@Override
	public float getAvgRuntime() {
		return avgRuntime;
	}

	@Override
	public void resetTask(int iteration) {
		// nothing to do here.
	}

	@Override
	public void run() {

		long t = System.nanoTime();

		manager.setContext(partition);
		ManyToOneConcurrentLinkedQueue<Message> queue = phase.get() ? queueEven : queueOdd;

		Message msg;
		while ((msg = queue.poll()) != null) {
			incoming.add(msg);
		}

		if (incoming.size() > 1) {
			incoming.sort(DELIVERY_ORDER);
		}

		for (Message incomingMsg : incoming) {
			process(incomingMsg);
		}
		incoming.clear();

		if (!initialized) {
			lp.onPrepareSim();
			initialized = true;
		}

		// perform a sim step, if this LP is steppable
		if (steppable != null) {
			steppable.doSimStep(time);
		}

		long rt = System.nanoTime() - t;
		avgRuntime = 0.8f * avgRuntime + 0.2f * rt;
		SimTask.addRuntime(runtimes, time, rt);
	}

	@Override
	public void cleanup() {
		manager.setContext(partition);
		lp.onCleanupSim();
	}

	private void process(Message msg) {
		Consumer<Message> consumer = consumers.get(msg.getType());
		if (consumer == null) {
			throw new IllegalArgumentException("No processor found for message: " + msg);
		}
		consumer.accept(msg);
	}

	@Override
	public String toString() {
		return "LPTask{" +
			"lp=" + lp.getClass().getName() +
			", partition=" + partition +
			'}';
	}
}
