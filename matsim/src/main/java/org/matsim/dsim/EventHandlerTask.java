package org.matsim.dsim;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.MessageProcessor;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.EventSource;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.serialization.SerializationProvider;
import org.matsim.dsim.events.AggregateFromAll;
import org.matsim.dsim.events.EventMessagingPattern;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public sealed abstract class EventHandlerTask implements SimTask permits DefaultEventHandlerTask, GlobalAsyncEventHandlerTask, SingleNodeAsyncEventHandlerTask {

	/**
	 * Handler to execute.
	 */
	protected final EventHandler handler;

	public EventHandler getHandler() {
		return handler;
	}

	/**
	 * Partition number.
	 */
	protected final int partition;

	/**
	 * This maps the messages types to corresponding function on the LP used for processing.
	 */
	protected final Int2ObjectMap<Consumer<Message>> consumers = new Int2ObjectOpenHashMap<>();

	/**
	 * Stores which events are accepted for each type.
	 */
	protected final Int2ObjectMap<EventSource> eventSources = new Int2ObjectOpenHashMap<>();

	protected final SerializationProvider serializer;

	/**
	 * Runtimes of each iteration.
	 */
	protected final LongList runtimes = new LongArrayList();

	protected MessageBroker broker;

	public void setBroker(MessageBroker broker) {
		this.broker = broker;
	}

	/**
	 * Avg. runtime of last iterations.
	 */
	protected float avgRuntime = 0.0f;
	/**
	 * Run time of the last few iterations.
	 */
	protected long sumRuntime = 0;
	/**
	 * Current simulation time. Needs to be volatile to ensure visibility across threads.
	 */
	protected volatile double time;

	public void setTime(double time) {
		this.time = time;
	}

	/**
	 * Indicates if the messages need to be sorted.
	 */
	protected volatile boolean needsSorting = false;

	/**
	 * Whether this task can be executed asynchronously between sim steps.
	 */
	protected final boolean async;

	public boolean isAsync() {
		return async;
	}

	/**
	 * If the task is executed asynchronously, the future is stored here.
	 */
	protected Future<?> future;

	public void setFuture(Future<?> future) {
		this.future = future;
	}

	public EventHandlerTask(EventHandler handler, int partition, boolean async, SerializationProvider serializer) {
		this.handler = handler;
		this.partition = partition;
		this.async = async;
		this.serializer = serializer;
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
		return handler != null && handler.processing().equals(ProcessingMode.DIRECT) && !Objects.equals(d, "1");
	}

	Consumer<Message> getConsumer(int type) {
		return consumers.get(type);
	}

	@SuppressWarnings("unchecked")
	protected EventMessagingPattern<?> buildConsumers(SerializationProvider serializer, boolean isDistributed) {

		DistributedEventHandler distributed = handler.getClass().getAnnotation(DistributedEventHandler.class);
		boolean node = distributed != null && distributed.value() == DistributedMode.NODE;
		boolean partition = distributed != null && (distributed.value() == DistributedMode.NODE_CONCURRENT || distributed.value() == DistributedMode.PARTITION);

		for (Class<?> ifType : handler.getClass().getInterfaces()) {
			if (MessageProcessor.class.isAssignableFrom(ifType)) {

				Method[] methods = ifType.getDeclaredMethods();

				// Could be an aggregated interface without methods, this can be ignored
				// Event handler not implementing a EventHandler interface will not be recognized
				if (methods.length == 0)
					continue;

				if ((!methods[0].getName().equals("process") && !methods[0].getName().equals("handleEvent")) || methods[0].getParameterCount() != 1)
					continue;

				Class<?> msgClass = methods[0].getParameterTypes()[0];
				boolean isEvent = Event.class.isAssignableFrom(msgClass);
				int type = serializer.getType(msgClass);

				String target = isEvent ? "handleEvent" : "process";

				Method consumerMethod = getConsumerMethod(target, msgClass);

				consumers.put(type, (Consumer<Message>) createConsumer(handler, msgClass, target));
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

		boolean singleton = distributed != null && (distributed.value() == DistributedMode.NODE || distributed.value() == DistributedMode.NODE_CONCURRENT);

		// Singleton handlers on one jvm don't need to communicate
		if (singleton && !isDistributed) {
			return null;
		}

		// Register consumers for message patterns
		if (handler instanceof AggregatingEventHandler<?>) {
			AggregateFromAll<Message> h = new AggregateFromAll<>();
			Method m = getHandlerSendMethod(handler);
			int type = serializer.getType(m.getReturnType());
			consumers.put(type, h);
			return h;
		}

		return null;
	}

	private Method getConsumerMethod(String target, Class<?> msgType) {
		try {
			return handler.getClass().getMethod(target, msgType);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private Method getHandlerSendMethod(EventHandler target) {
		try {
			return target.getClass().getDeclaredMethod("send");
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private Consumer<? extends Message> createConsumer(Object handler, Class<?> msgType, String target) {
		try {
			return LambdaUtils.createConsumer(handler, msgType, target);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * Wait for async task to finish (if set).
	 */
	public abstract void waitAsync(boolean last) throws ExecutionException, InterruptedException;

	@Override
	public final boolean needsExecution() {
		return time > 0 && time % handler.getProcessInterval() == 0;
	}

	protected boolean isSyncTime() {
		return (time > 0 && time % handler.getSyncInterval() == 0);
	}

	@Override
	public void resetTask(int iteration) {
		this.handler.reset(iteration);
	}

	public final IntSet getSupportedMessages() {
		return consumers.keySet();
	}

	/**
	 * Return the event source for the given type.
	 */
	EventSource getEventSource(int type) {
		return eventSources.get(type);
	}

	@Override
	public LongList getRuntime() {
		return runtimes;
	}

	@Override
	public float getAvgRuntime() {
		return avgRuntime;
	}

	/**
	 * Debug function to dump events to a file.
	 */
	static void dumpEvents(Path out, Collection<Message> messages) {
		StringBuilder b = new StringBuilder();
		for (Message m2 : messages) {
			if (m2 instanceof Event ev) {
				ev.writeAsXML(b);
			}
		}

		try {
			Files.write(out, b.toString().getBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Process one message / event.
	 */
	protected final void process(Message msg) {

		var consumer = findConsumer(msg);
		if (consumer == null) {
			throw new IllegalArgumentException("No processor found for message: " + msg);
		}
		consumer.accept(msg);

		/*
		var consumerData = consumers.get(msg.getType());
		if (consumerData != null) {
			var clazz = msg.getClass().getSuperclass();
			var superType = this.serializer.getType(clazz);
		}


		var consumer = consumers.get(msg.getType());
		var clazz = msg.getClass();





		if (consumer == null) {
			consumer = consumers.get(Event.ANY_TYPE);
		}
		if (consumer == null) {
			throw new IllegalArgumentException("No processor found for message: " + msg);
		}

		consumer.accept(msg);

		 */
	}

	private Consumer<Message> findConsumer(Message msg) {

		Class<?> clazz = msg.getClass();
		var consumer = consumers.get(msg.getType());

		// ideally, we are passed an event for which we have stored a consumer.
		// However, it is allowed to have events handlers that listen for events which are the base class
		// for some event. In that case, we need to go up the inheritance hierarchy and check whether we
		// have a suitable consumer. This relies on the events manager to only pass us events that are we
		// might be interested in.
		while (consumer == null && Event.class.isAssignableFrom(clazz)) {

			clazz = clazz.getSuperclass();
			var type = serializer.getType(clazz);
			consumer = consumers.get(type);
		}
		return consumer;
	}

	/**
	 * Store run time information.
	 *
	 * @param t nanoseconds before current step started.
	 */
	protected final void storeRuntime(long t) {
		int s = (int) (time / 10);
		// Fill with zeros
		if (runtimes.size() < s)
			runtimes.addElements(runtimes.size(), new long[s - runtimes.size()]);

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
	public final String toString() {
		return "EventHandlerTask{" +
			"handler=" + handler.getClass().getName() +
			", partition=" + partition +
			'}';
	}
}
