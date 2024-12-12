package org.matsim.dsim;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;
import org.matsim.api.LP;
import org.matsim.api.core.v01.MessageProcessor;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.EventSource;
import org.matsim.api.core.v01.events.MessageComparator;
import org.matsim.api.core.v01.events.handler.AggregatingEventHandler;
import org.matsim.api.core.v01.events.handler.DistributedEventHandler;
import org.matsim.api.core.v01.events.handler.DistributedMode;
import org.matsim.api.core.v01.events.handler.EventsFrom;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.dsim.Message;
import org.matsim.core.serialization.SerializationProvider;
import org.matsim.dsim.events.AggregateFromAll;
import org.matsim.dsim.events.EventMessagingPattern;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Log4j2
@SuppressWarnings("rawtypes")
public final class EventHandlerTask implements SimTask {

	/**
	 * Handler to execute.
	 */
	@Getter
	private final EventHandler handler;
	/**
	 * Pattern to use for messaging.
	 */
	@Nullable
	private final EventMessagingPattern pattern;

	private final DistributedEventsManager manager;

	/**
	 * Partition number.
	 */
	private final int partition;

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
	 * This maps the messages types to corresponding function on the LP used for processing.
	 */
	private final Int2ObjectMap<Consumer<Message>> consumers = new Int2ObjectOpenHashMap<>();

	/**
	 * Stores which events are accepted for each type.
	 */
	private final Int2ObjectMap<EventSource> eventSources = new Int2ObjectOpenHashMap<>();

	/**
	 * Runtimes of each iteration.
	 */
	private final LongList runtimes = new LongArrayList();
	/**
	 * Switching phase for the queues.
	 */
	private final AtomicBoolean phase = new AtomicBoolean();
	/**
	 * Avg. runtime of last iterations.
	 */
	private float avgRuntime = 0.0f;
	/**
	 * Current simulation time. Needs to be volatile to ensure visibility across threads.
	 */
	@Setter
	private volatile double time;
	/**
	 * Indicates if the messages need to be sorted.
	 */
	private volatile boolean needsSorting = false;
	@Setter
	private MessageBroker broker;

	public EventHandlerTask(EventHandler handler, int partition, int totalPartitions,
							DistributedEventsManager manager, SerializationProvider serializer,
							AtomicInteger counter) {
		this.handler = handler;
		this.partition = partition;
		this.totalPartitions = totalPartitions;
		this.manager = manager;
		this.counter = counter;
		this.pattern = buildConsumers(serializer);
	}

	@Override
	public String getName() {
		return handler.getName();
	}

	@Override
	public int getPartition() {
		return partition;
	}

	/**
	 * Need to be called when events have been added out-of-order
	 */
	public void setSorting() {
		this.needsSorting = true;
	}

	/**
	 * Supports direct execution.
	 */
	boolean isDirect() {
		DistributedEventHandler handler = this.handler.getClass().getAnnotation(DistributedEventHandler.class);
		String d = System.getenv("DISABLE_DIRECT_EVENTS");
		return handler != null && handler.directProcessing() && !Objects.equals(d, "1");
	}

	Consumer<Message> getConsumer(int type) {
		return consumers.get(type);
	}

	@SneakyThrows
	@SuppressWarnings("unchecked")
	private EventMessagingPattern<?> buildConsumers(SerializationProvider serializer) {

		for (Class<?> ifType : handler.getClass().getInterfaces()) {

			DistributedEventHandler distributed = handler.getClass().getAnnotation(DistributedEventHandler.class);
			boolean node = distributed != null && distributed.value() == DistributedMode.NODE_SINGLETON;
			boolean partition = distributed != null && (distributed.value() == DistributedMode.PARTITION_SINGLETON || distributed.value() == DistributedMode.PARTITION);

			if (MessageProcessor.class.isAssignableFrom(ifType)) {

				Method[] methods = ifType.getDeclaredMethods();

				// Could be an aggregated interface without methods, this can be ignored
				// Event handler not implementing a EventHandler interface will not be recognized
				if (methods.length == 0)
					continue;

				if ((!methods[0].getName().equals("process") && !methods[0].getName().equals("handleEvent")) || methods[0].getParameterCount() != 1)
					continue;

				Class<?> msgType = methods[0].getParameterTypes()[0];
				boolean isEvent = Event.class.isAssignableFrom(msgType);
				int type = serializer.getType(msgType);

				String target = isEvent ? "handleEvent" : "process";
				Method consumerMethod = handler.getClass().getMethod(target, msgType);

				consumers.put(type, (Consumer<Message>) LambdaUtils.createConsumer(handler, msgType, target));
				if (isEvent) {
					EventSource source = node ? EventSource.NODE : partition ? EventSource.PARTITION : EventSource.GLOBAL;
					if (consumerMethod.isAnnotationPresent(EventsFrom.class)) {
						source = consumerMethod.getAnnotation(EventsFrom.class).value();
					}

					eventSources.put(type, source);
				}
			}
		}

		if (consumers.isEmpty()) {
			throw new IllegalStateException(("No event handling methods found for: %s. " +
				"This is most likely due to unsupported class hierarchy. " +
				"Please explicitly implement needed EventHandler").formatted(handler.getClass().getName()));
		}

		// Register consumers for message patterns
		if (handler instanceof AggregatingEventHandler<?>) {
			AggregateFromAll<Message> h = new AggregateFromAll<>();

			Method m = handler.getClass().getDeclaredMethod("send");
			int type = serializer.getType(m.getReturnType());
			consumers.put(type, h);
			return h;
		}

		return null;
	}

	@Override
	public boolean needsExecution() {
		return time > 0 && time % handler.getProcessInterval() == 0;
	}

	@Override
	public void beforeExecution() {
		phase.set(!phase.get());
	}

	public void add(Message msg) {
		ManyToOneConcurrentLinkedQueue<Message> queue = phase.get() ? queueOdd : queueEven;
		queue.add(msg);
	}


	public IntSet getSupportedMessages() {
		return consumers.keySet();
	}

	/**
	 * Return the event source for the given type.
	 */
	EventSource getEventSource(int type) {
		return eventSources.get(type);
	}

	public IntSet waitForOtherRanks(double time) {

		if (pattern != null && needsExecution()) {
			return pattern.waitForOtherRanks(time);
		}

		return LP.NO_NEIGHBORS;
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
		ManyToOneConcurrentLinkedQueue<Message> queue = phase.get() ? queueEven : queueOdd;
		Message msg;

		// Sort events if needed
		if (needsSorting) {
			while ((msg = queue.poll()) != null) {
				messages.add(msg);
			}

			messages.sort(MessageComparator.INSTANCE);

			for (Message m : messages) {
				process(m);
			}

			messages.clear();
			needsSorting = false;
		} else
			while ((msg = queue.poll()) != null) {
				process(msg);
			}

		// TODO singleton handler on a single node does not need to have communication pattern
		if (pattern != null) {
			// TODO: these need to happen between different time steps?

			// If there is a counter, only the last event handler will perform communication
			if (counter != null) {
				if (counter.incrementAndGet() == totalPartitions) {
					pattern.process(handler);
					pattern.communicate(broker, handler);
					counter.set(0);
				}
			} else {
				pattern.process(handler);
				pattern.communicate(broker, handler);
			}
		}

		int s = (int) (time / 10);
		// Fill with zeros
		if (runtimes.size() < s)
			runtimes.addElements(runtimes.size(), new long[s - runtimes.size()]);

		long rt = System.nanoTime() - t;
		avgRuntime = 0.8f * avgRuntime + 0.2f * rt;

		if ((time % 10) == 0)
	        runtimes.add(rt);
    }


	private void process(Message msg) {

		Consumer<Message> consumer = consumers.get(msg.getType());
		if (consumer == null) {
			Consumer<Message> any = consumers.get(Event.ANY_TYPE);
			if (any != null) {
				any.accept(msg);
				return;
			}

			throw new IllegalArgumentException("No processor found for message: " + msg);
		}

		consumer.accept(msg);
	}

	@Override
	public String toString() {
		return "EventHandlerTask{" +
			"handler=" + handler.getClass().getName() +
			", partition=" + partition +
			'}';
	}
}
