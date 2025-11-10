package org.matsim.dsim;

import com.google.inject.Inject;
import com.google.inject.Provider;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.EventSource;
import org.matsim.api.core.v01.events.handler.DistributedEventHandler;
import org.matsim.api.core.v01.events.handler.DistributedMode;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.api.core.v01.messages.EventRegistry;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.communication.Communicator;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.serialization.SerializationProvider;
import org.matsim.dsim.executors.LPExecutor;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class DistributedEventsManager implements EventsManager {

	private static final Logger log = LogManager.getLogger(DistributedEventsManager.class);

	private final MessageBroker broker;

	private final ComputeNode computeNode;
	private final LPExecutor executor;
	private final SerializationProvider serializer;

	/**
	 * All event handlers and their tasks.
	 */
	private final List<EventHandlerTask> tasks = new ArrayList<>();

	/**
	 * Task mapped by partition and supported event type.
	 */
	private final Long2ObjectMap<List<SimTask>> byPartitionAndType = new Long2ObjectOpenHashMap<>(1024);

	/**
	 * Direct event listener that don't queue events but process immediately.
	 */
	private final Long2ObjectMap<List<Consumer<Message>>> directListener = new Long2ObjectOpenHashMap<>();

	/**
	 * Global event listeners with their index.
	 */
	private final Int2ObjectMap<List<EventHandlerTask>> globalListener = new Int2ObjectOpenHashMap<>();

	/**
	 * Listener for event types on other nodes/ranks. Always exactly one other node, or broadcast to all.
	 */
	private final Int2IntMap remoteListener = new Int2IntOpenHashMap();
	/**
	 * One queue for target ranks with outgoing events.
	 */
	private final Int2ObjectMap<ManyToOneConcurrentLinkedQueue<Event>> remoteEvents = new Int2ObjectOpenHashMap<>();

	/**
	 * Stores current context for a thread.
	 */
	private final ThreadLocal<AtomicInteger> ctxPartition = ThreadLocal.withInitial(() -> new AtomicInteger(-1));

	/**
	 * Set of other nodes that are waited for during event exchange.
	 */
	private final IntSet waitFor = new IntOpenHashSet();

	private final boolean eventsDisabled;
	/**
	 * The remote sync step for events, which is the minimum of all handlers.
	 */
	private double remoteSyncStep = Double.POSITIVE_INFINITY;
	/**
	 * The last time {@link #remoteEvents} have been synced.
	 */
	private double lastSync = -1;

	@Inject
	DistributedEventsManager(MessageBroker broker, ComputeNode computeNode, LPExecutor executor, SerializationProvider serializer) {
		this.broker = broker;
		this.computeNode = computeNode;
		this.executor = executor;
		this.serializer = serializer;
		this.eventsDisabled = Objects.equals(System.getenv("DISABLE_EVENTS"), "1");
	}

	/**
	 * Whether the handler supports async execution.
	 */
	public static boolean supportsAsync(EventHandler handler) {
		DistributedEventHandler ann = handler.getClass().getAnnotation(DistributedEventHandler.class);
		return ann != null && ann.async();
	}

	/**
	 * Whether this is a global event handler.
	 */
	public static boolean isGlobal(EventHandler handler) {
		DistributedEventHandler ann = handler.getClass().getAnnotation(DistributedEventHandler.class);
		return ann == null || ann.value() == DistributedMode.GLOBAL;
	}

	@Override
	public <T extends EventHandler> List<T> addHandler(Provider<T> provider) {
		return addInternal(provider.get(), provider);
	}

	@Override
	public void addHandler(EventHandler handler) {

		DistributedEventHandler partition = handler.getClass().getAnnotation(DistributedEventHandler.class);
		if (partition != null && partition.value() == DistributedMode.PARTITION) {
			throw new IllegalArgumentException("Adding instance of PartitionEventHandler without provider. " +
				"The eventManager is not able to create the instance. Use .addHandler(Provider<EventHandler> provider)");
		}

		addInternal(handler, null);
	}

	private <T extends EventHandler> List<T> addInternal(T handler, Provider<T> provider) {

		String name = handler.getName();

		List<T> handlers = new ArrayList<>();

		DistributedEventHandler partition = handler.getClass().getAnnotation(DistributedEventHandler.class);

		if (partition != null && partition.value() == DistributedMode.NODE_SINGLETON) {
			log.info("Registering event handler {} for node {}", name, computeNode.getRank());
			Integer part = computeNode.getParts().getFirst();
			EventHandlerTask task = executor.register(handler, this, part, 1, null);
			addTask(task, part);
			handlers.add(handler);

		} else if (partition != null && (partition.value() == DistributedMode.PARTITION || partition.value() == DistributedMode.PARTITION_SINGLETON)) {
			log.info("Registering event handler {} for each partition", name);

			AtomicInteger partitionCounter = new AtomicInteger();
			for (int part : computeNode.getParts()) {
				EventHandlerTask task = executor.register(handler, this, part, computeNode.getParts().size(), partitionCounter);

				addTask(task, part);
				handlers.add(handler);

				if (partition.value() == DistributedMode.PARTITION) {
					T next = provider.get();
					if (handler == next)
						throw new IllegalStateException("The provider must return a new instance of the handler or PARTITION_SINGLETON must be set.");

					handler = next;
				}
			}
		} else if (computeNode.getRank() == 0) {
			log.warn("Registering global event handler {}", name);
			Integer part = computeNode.getParts().getFirst();
			EventHandlerTask task = executor.register(handler, this, part, 1, null);
			addTask(task, part);
			handlers.add(handler);
		}

		return handlers;
	}

	private void addTask(EventHandlerTask task, int part) {

		broker.register(task, part);

		for (int type : task.getSupportedMessages()) {

			if (!serializer.hasType(type)) {
				log.warn("No serializer for type {} from task {}", type, task.getName());
				continue;
			}

			EventSource source = task.getEventSource(type);
			if (!task.isDirect() && source == EventSource.GLOBAL) {
				globalListener.computeIfAbsent(type, _ -> new ArrayList<>()).add(task);
				remoteSyncStep = Double.min(remoteSyncStep, task.getHandler().getProcessInterval());
			}
			// otherwise the task is local and is not added to any queue.

			// Store handler as listener for all partitions
			if (source == EventSource.GLOBAL || source == EventSource.NODE) {
				for (Integer p : computeNode.getParts()) {
					long address = MessageBroker.address(p, type);
					if (task.isDirect())
						directListener.computeIfAbsent(address, _ -> new ArrayList<>()).add(task.getConsumer(type));
					else
						byPartitionAndType.computeIfAbsent(address, _ -> new ArrayList<>()).add(task);
				}
			} else {

				long address = MessageBroker.address(part, type);
				if (task.isDirect())
					directListener.computeIfAbsent(address, _ -> new ArrayList<>()).add(task.getConsumer(type));
				else
					byPartitionAndType.computeIfAbsent(address, _ -> new ArrayList<>()).add(task);

			}
		}

		task.setBroker(broker);
		tasks.add(task);
	}

	/**
	 * Remove task from internal data structures.
	 */
	private void removeTask(EventHandlerTask task) {

		byPartitionAndType.values().forEach(list -> list.remove(task));
		globalListener.values().forEach(list -> list.remove(task));

		if (task.isDirect()) {
			for (int type : task.getSupportedMessages()) {
				Consumer<Message> c = task.getConsumer(type);
				directListener.values().forEach(list -> list.remove(c));
			}
		}

		tasks.remove(task);
		broker.deregister(task);
		executor.deregister(task);
	}

	/**
	 * Communicates with all messages broker to synchronize.
	 * This also resets the internal state to prepare for a new iteration.
	 */
	void syncEventRegistry(Communicator comm) {

		EventRegistry.EventRegistryBuilder registry = EventRegistry.builder();

		registry.rank(comm.getRank());
		registry.eventTypes(new IntOpenHashSet(globalListener.keySet()));
		registry.syncStep(remoteSyncStep);

		// Clear all data structures
		waitFor.clear();
		remoteListener.clear();
		lastSync = -1;

		EventRegistry self = registry.build();
		List<EventRegistry> all = comm.allGather(self, 1, serializer);

		if (!globalListener.isEmpty()) {

			Map<String, List<String>> info = new HashMap<>();

			globalListener.forEach((type, list) -> {
				String name = serializer.getType(type).getSimpleName();
				for (EventHandlerTask task : list) {
					info.computeIfAbsent(task.getName(), _ -> new ArrayList<>()).add(name);
				}
			});

			log.warn("Globals listener on this node: \n\t{}", String.join("\n\t", info.entrySet().stream()
				.map(e -> "%s: %s".formatted(e.getKey(), e.getValue()))
				.toList()));

			log.warn("### Global event listeners reduce parallelism and impact performance negatively. ###");
		}

		// During initialization, this map contains the local sync steps, but these are not relevant for
		// the same node.
		remoteSyncStep = Double.POSITIVE_INFINITY;

		for (EventRegistry reg : all) {

			remoteSyncStep = Double.min(remoteSyncStep, reg.getSyncStep());

			if (reg.getRank() == comm.getRank())
				continue;


			for (int type : reg.getEventTypes()) {
				// If remote listener already contains the event type, this event must be broadcast to all
				int receiver = remoteListener.containsKey(type) ? Communicator.BROADCAST_TO_ALL : reg.getRank();

				remoteListener.put(type, receiver);
				remoteEvents.computeIfAbsent(receiver, _ -> new ManyToOneConcurrentLinkedQueue<>());
			}

			// TODO: Does not consider if events are broadcasted from other nodes
			if (!self.getEventTypes().isEmpty()) {
				waitFor.add(reg.getRank());
			}
		}

		IntOpenHashSet remotes = new IntOpenHashSet(remoteListener.values());
		// Remove event queues that have not outgoing events
		remoteEvents.keySet().removeIf(k -> !remotes.contains(k));

		Map<String, String> info = new HashMap<>();
		for (Int2IntMap.Entry kv : remoteListener.int2IntEntrySet()) {
			String name = serializer.getType(kv.getIntKey()).getSimpleName();
			info.put(name, "Node: %d".formatted(kv.getIntValue()));

		}

		log.info("N{} Event registry: {}, with time step: {}", comm.getRank(), info, remoteSyncStep);
		log.info("N{} Events expected from nodes: {}", comm.getRank(), waitFor);
	}

	public void setContext(int partition) {
		ctxPartition.get().set(partition);
	}

	@Override
	public void removeHandler(EventHandler handler) {

		tasks.stream()
			.filter(t -> t.getHandler() == handler)
			.findAny()
			.ifPresent(this::removeTask);
	}

	@Override
	public void processEvent(Event e) {

		if (eventsDisabled) {
			return;
		}

		// Need to process two times, for handlers that registered for ANY_TYPE
		boolean sent = processInternal(e, e.getType(), false);
		processInternal(e, Event.ANY_TYPE, sent);
	}

	/**
	 * Return whether event was queued for remote sending.
	 */
	private boolean processInternal(Event e, int type, boolean alreadySent) {
		// Send to all local tasks
		long address = MessageBroker.address(ctxPartition.get().get(), type);

		List<Consumer<Message>> direct = directListener.get(address);
		if (direct != null)
			direct.forEach(c -> c.accept(e));

		List<SimTask> listener = byPartitionAndType.get(address);
		if (listener != null)
			listener.forEach(t -> t.add(e));

		// Send the event remotely
		if (!alreadySent && remoteListener.containsKey(type)) {
			remoteEvents.get(remoteListener.get(type)).add(e);
			return true;
		}

		return false;
	}

	@Override
	public void resetHandlers(int iteration) {
		for (EventHandlerTask task : tasks) {
			task.getHandler().reset(iteration);
		}
	}

	@Override
	public void initProcessing() {

	}

	@Override
	public void beforeSimStep(double time) {
		List<Event> events = broker.getEvents();

		if (events.isEmpty())
			return;

//        log.debug("Received {} events from other nodes", events.size());

		// Only global handlers may have received events from other nodes
		for (Event event : events) {
			globalListener.getOrDefault(Event.ANY_TYPE, List.of())
				.forEach(task -> {
					task.add(event);
					task.setSorting();
				});

			globalListener.getOrDefault(event.getType(), List.of())
				.forEach(task -> {
					task.add(event);
					task.setSorting();
				});
		}

		events.clear();
	}

	@Override
	public void afterSimStep(double time) {

		if (lastSync + remoteSyncStep <= time) {

			for (Int2ObjectMap.Entry<ManyToOneConcurrentLinkedQueue<Event>> kv : remoteEvents.int2ObjectEntrySet()) {

				int receiver = kv.getIntKey();
				ManyToOneConcurrentLinkedQueue<Event> events = remoteEvents.get(receiver);

//                log.debug("Syncing {} events to {}", events.size(), receiver);

				Event e;
				while ((e = events.poll()) != null) {
					broker.send(e, receiver);
				}

				broker.addNullMessage(receiver);
			}

			if (!waitFor.isEmpty()) {
				waitFor.forEach(broker::addWaitForRank);
			}

			lastSync = time;
		}
	}

	@Override
	public void finishProcessing() {

		// Force last sync
		double time = lastSync + remoteSyncStep;

		afterSimStep(time);
		broker.beforeSimStep(time);
		broker.syncTimestep(time, true);

		beforeSimStep(time + 1);

		executor.runEventHandler();

	}

	public ComputeNode getComputeNode() {
		return computeNode;
	}
}
