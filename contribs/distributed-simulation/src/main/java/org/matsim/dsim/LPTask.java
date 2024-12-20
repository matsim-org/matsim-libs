package org.matsim.dsim;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;
import org.matsim.api.LP;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.MessageProcessor;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.serialization.SerializationProvider;

import java.lang.reflect.Method;
import java.util.function.Consumer;

@Log4j2
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
	 * Buffer holding incoming messages. These are switched between iterations.
	 */
	private final ManyToOneConcurrentLinkedQueue<Message> queueOdd = new ManyToOneConcurrentLinkedQueue<>();
	private final ManyToOneConcurrentLinkedQueue<Message> queueEven = new ManyToOneConcurrentLinkedQueue<>();

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
	 * Run time of the last few iterations.
	 */
	private long sumRuntime = 0;

	/**
	 * Indicates whether the LP has been initialized.
	 */
	private boolean initialized = false;

	/**
	 * Current simulation time. Needs to be volatile to ensure visibility across threads.
	 */
	@Setter
	private volatile double time;

	/**
	 * Switching phase for the queues.
	 */
	private volatile boolean phase;

	public LPTask(LP lp, int partition, DistributedEventsManager manager, SerializationProvider serializer) {
		this.lp = lp;
		this.steppable = lp instanceof Steppable s ? s : null;
		this.partition = partition;
		this.manager = manager;

		buildConsumers(serializer);
	}

	@Override
	public String getName() {
		return lp.getClass().getSimpleName();
	}

	@Override
	public int getPartition() {
		return partition;
	}

	@SneakyThrows
	@SuppressWarnings("unchecked")
	private void buildConsumers(SerializationProvider serializer) {

		for (Class<?> ifType : lp.getClass().getInterfaces()) {
			if (MessageProcessor.class.isAssignableFrom(ifType)) {
				Method[] methods = ifType.getDeclaredMethods();

				Class<?> msgType = methods[0].getParameterTypes()[0];
				int type = serializer.getType(msgType);

				consumers.put(type, (Consumer<Message>) LambdaUtils.createConsumer(lp, msgType, "process"));
			}
		}
	}

	@Override
	public void beforeExecution() {
		phase = !phase;
	}

	/**
	 * Adds a message to the buffer.
	 */
	public void add(Message msg) {
		ManyToOneConcurrentLinkedQueue<Message> queue = phase ? queueOdd : queueEven;
		queue.add(msg);
	}

	/**
	 * Return the set of supported messages.
	 */
	public IntSet getSupportedMessages() {
		return consumers.keySet();
	}

	public IntSet waitForOtherRanks(double time) {
		return lp.waitForOtherRanks(time);
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
	public void run() {

		long t = System.nanoTime();

		manager.setContext(partition);
		ManyToOneConcurrentLinkedQueue<Message> queue = phase ? queueEven : queueOdd;

		Message msg;
		while ((msg = queue.poll()) != null) {
			process(msg);
		}

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
		sumRuntime += rt;

		// Only add the runtime to the list if the time is a multiple of 10
		if ((time % 10) == 0) {
			runtimes.add(sumRuntime);
			sumRuntime = 0;
		}
    }

	@Override
	public void cleanup() {
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
