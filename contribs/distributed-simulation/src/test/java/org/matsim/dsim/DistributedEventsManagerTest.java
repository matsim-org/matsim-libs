package org.matsim.dsim;

import com.google.inject.Provider;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.DistributedEventHandler;
import org.matsim.api.core.v01.events.handler.DistributedMode;
import org.matsim.api.core.v01.events.handler.ProcessingMode;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.core.communication.LocalCommunicator;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.serialization.SerializationProvider;
import org.matsim.dsim.executors.LPExecutor;
import org.matsim.dsim.executors.SingleExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class DistributedEventsManagerTest {

	@Test
	public void globalHandler() {
		MessageBroker broker = mock(MessageBroker.class);
		var provider = SerializationProvider.getInstance();
		LPExecutor executor = new SingleExecutor(provider);
		var computeNode = ComputeNode.builder()
			.rank(0)
			.parts(IntList.of(0, 1))
			.build();

		var globalHandler = new GlobalTestHandler();
		var manager = new DistributedEventsManager(broker, computeNode, executor, provider);
		manager.addHandler(globalHandler);

		manager.setContext(0);
		manager.processEvent(new TestEvent(1., "test"));
		assertEquals(0, globalHandler.counter.get());
		manager.setContext(1);
		manager.processEvent(new TestEvent(1., "test"));
		assertEquals(0, globalHandler.counter.get());

		// events are queued on the handler task and are only processed after the executor runs the event handler task
		executor.runEventHandler();
		assertEquals(2, globalHandler.counter.get());
	}

	@Test
	public void globalHandlerNotHeadNode() {
		MessageBroker broker = mock(MessageBroker.class);
		var provider = SerializationProvider.getInstance();
		LPExecutor executor = new SingleExecutor(provider);
		var computeNode = ComputeNode.builder()
			.rank(1)
			.parts(IntList.of(0, 1))
			.build();

		var globalHandler = new GlobalTestHandler();
		var manager = new DistributedEventsManager(broker, computeNode, executor, provider);
		manager.addHandler(globalHandler);

		manager.setContext(0);
		manager.processEvent(new TestEvent(1., "test"));
		manager.setContext(1);
		manager.processEvent(new TestEvent(1., "test"));

		// events are queued on the handler task and are only processed after the executor runs the event handler task
		executor.runEventHandler();
		assertEquals(0, globalHandler.counter.get());
	}

	@Test
	public void nodeHandler() {
		MessageBroker broker = mock(MessageBroker.class);
		var provider = SerializationProvider.getInstance();
		LPExecutor executor = new SingleExecutor(provider);
		var computeNode = ComputeNode.builder()
			.rank(0)
			.parts(IntList.of(0, 1))
			.build();

		var nodeHandler = new NodeTestHandler(new ArrayList<>(List.of("first", "second")));
		var manager = new DistributedEventsManager(broker, computeNode, executor, provider);
		manager.addHandler(nodeHandler);

		manager.setContext(1);
		manager.processEvent(new TestEvent(1., "first"));
		assertEquals(2, nodeHandler.expectedTypes.size());
		manager.setContext(0);
		assertEquals(2, nodeHandler.expectedTypes.size());
		manager.processEvent(new TestEvent(1., "second"));

		executor.runEventHandler();

		assertEquals(0, nodeHandler.expectedTypes.size());
	}

	@Test
	public void nodeConcurrentHandler() {
		MessageBroker broker = mock(MessageBroker.class);
		var provider = SerializationProvider.getInstance();
		LPExecutor executor = new SingleExecutor(provider);
		var computeNode = ComputeNode.builder()
			.rank(0)
			.parts(IntList.of(0, 1))
			.build();

		var nodeHandler = new NodeConcurrentTestHandler(new ArrayList<>(List.of("first", "second")));
		var manager = new DistributedEventsManager(broker, computeNode, executor, provider);
		manager.addHandler(nodeHandler);

		// submit event from process 1 first and from 0 second. This relies on the internal ordering in
		// SingleExecutor :-/, as it will call tasks with increasing partition count. In our case it will
		// call partition 0 and then 1. Thus, the ordering is changed, other than with the NodeTestHandler above.
		manager.setContext(1);
		manager.processEvent(new TestEvent(1., "second"));
		assertEquals(2, nodeHandler.expectedData.size());
		manager.setContext(0);
		manager.processEvent(new TestEvent(1., "first"));
		assertEquals(2, nodeHandler.expectedData.size());

		executor.runEventHandler();

		assertEquals(0, nodeHandler.expectedData.size());
	}

	@Test
	public void nodeConcurrentHandlerDirect() {
		MessageBroker broker = mock(MessageBroker.class);
		var provider = SerializationProvider.getInstance();
		LPExecutor executor = new SingleExecutor(provider);
		var computeNode = ComputeNode.builder()
			.rank(0)
			.parts(IntList.of(0, 1))
			.build();

		var nodeHandler = new NodeConcurrentDirectTestHandler(new ArrayList<>(List.of("first", "second")));
		var manager = new DistributedEventsManager(broker, computeNode, executor, provider);
		manager.addHandler(nodeHandler);

		// submit event from process 1 first and from 0 second. Other than the NodeConcurrent test above, the order
		// should not change, as the handler should be called directly.
		manager.setContext(1);
		manager.processEvent(new TestEvent(1., "first"));
		assertEquals(1, nodeHandler.expectedData.size());
		manager.setContext(0);
		manager.processEvent(new TestEvent(1., "second"));
		assertEquals(0, nodeHandler.expectedData.size());

		executor.runEventHandler();
		assertEquals(0, nodeHandler.expectedData.size());
	}

	@Test
	public void partitionHandler() {
		MessageBroker broker = mock(MessageBroker.class);
		var serializationProvider = SerializationProvider.getInstance();
		LPExecutor executor = new SingleExecutor(serializationProvider);
		var computeNode = ComputeNode.builder()
			.rank(0)
			.parts(IntList.of(0, 1))
			.build();

		var handlerProvider = new PartitionHandlerProvider();
		var manager = new DistributedEventsManager(broker, computeNode, executor, serializationProvider);
		manager.addHandler(handlerProvider);

		var emittingPartition = 1;
		manager.setContext(emittingPartition);
		manager.processEvent(new TestEvent(1., "first"));

		// the manager calls the handler direct, so no need for executing handler tasks.
		// also, we expect that only the handler which was generated for the emitting
		// partition has received the event. All other handlers should have NO events.
		for (var i = 0; i < computeNode.getParts().size(); i++) {
			var handlerI = handlerProvider.handlers.get(i);
			if (i == emittingPartition) {
				assertEquals(1, handlerI.counter.get());
			} else {
				assertEquals(0, handlerI.counter.get());
			}
		}
	}

	@Test
	public void partitionHandlerParticularPart() {

		MessageBroker broker = mock(MessageBroker.class);
		var serializationProvider = SerializationProvider.getInstance();
		LPExecutor executor = new SingleExecutor(serializationProvider);
		var computeNode = ComputeNode.builder()
			.rank(0)
			.parts(IntList.of(0, 1))
			.build();

		var handler = new PartitionHandler();
		var manager = new DistributedEventsManager(broker, computeNode, executor, serializationProvider);
		manager.addHandler(handler, 0);

		var emittingPartition = 1;
		manager.setContext(emittingPartition);
		manager.processEvent(new TestEvent(1., "first"));
		assertEquals(0, handler.counter.get());

		emittingPartition = 0;
		manager.setContext(emittingPartition);
		manager.processEvent(new TestEvent(1., "second"));
		assertEquals(1, handler.counter.get());
	}

	@Test
	public void eventClassHierachy() {

		MessageBroker broker = mock(MessageBroker.class);
		var serializationProvider = SerializationProvider.getInstance();
		LPExecutor executor = new SingleExecutor(serializationProvider);
		var computeNode = ComputeNode.builder()
			.rank(0)
			.parts(IntList.of(0, 1))
			.build();

		var manager = new DistributedEventsManager(broker, computeNode, executor, serializationProvider);

		// this handler wants to be notified for subclassedEvents
		var subclassedTestEventHandler = new SubclassedEvent.Handler() {

			final AtomicInteger counter = new AtomicInteger(0);

			@Override
			public void handleEvent(SubclassedEvent e) {
				counter.incrementAndGet();
			}
		};
		// This handler wants to handle the intermediate Type
		var testEventHandler = new GlobalTestHandler();
		// This handler wants to handle the basic event type
		var basicEventHandler = new BasicEventHandler() {

			final AtomicInteger counter = new AtomicInteger(0);

			@Override
			public void handleEvent(Event event) {
				counter.incrementAndGet();
			}
		};
		manager.addHandler(subclassedTestEventHandler);
		manager.addHandler(testEventHandler);
		manager.addHandler(basicEventHandler);

		var emittingPartition = 0;
		manager.setContext(emittingPartition);

		manager.processEvent(new SubclassedEvent(1., "first"));
		manager.processEvent(new TestEvent(1., "second"));
		manager.processEvent(new LinkEnterEvent(1., Id.createVehicleId("unimportant-data"), Id.createLinkId("unimportant-data")));
		executor.runEventHandler();

		assertEquals(1, subclassedTestEventHandler.counter.get());
		assertEquals(2, testEventHandler.counter.get());
		assertEquals(3, basicEventHandler.counter.get());
	}
	
	@Test
	public void globalHandlerReceivesRemoteEvent() throws Exception {
		var communicators = LocalCommunicator.create(2);
		var provider = SerializationProvider.getInstance();

		var node0 = ComputeNode.builder().rank(0).parts(IntList.of(0)).build();
		var node1 = ComputeNode.builder().rank(1).parts(IntList.of(1)).build();
		var topology = Topology.builder()
			.totalPartitions(2)
			.computeNodes(List.of(node0, node1))
			.build();

		var executor0 = new SingleExecutor(provider);
		var broker0 = new MessageBroker(communicators.getFirst(), topology, provider);
		var manager0 = new DistributedEventsManager(broker0, node0, executor0, provider);

		var executor1 = new SingleExecutor(provider);
		var broker1 = new MessageBroker(communicators.getLast(), topology, provider);
		var manager1 = new DistributedEventsManager(broker1, node1, executor1, provider);

		// Register global handler on both nodes. It should only be registered with manager0
		var globalHandler = new GlobalTestHandler();
		manager0.addHandler(globalHandler);
		manager1.addHandler(globalHandler);

		// syncEventRegistry is a collective barrier: run both nodes concurrently
		runConcurrently(
			() -> manager0.syncEventRegistry(communicators.getFirst()),
			() -> manager1.syncEventRegistry(communicators.getLast())
		);

		// Fire an event on node 1 — the global handler on node 0 must receive it
		manager1.setContext(1);
		manager1.processEvent(new TestEvent(0.0, "from-node1"));

		// finishProcessing flushes queued remote events, syncs across nodes, and runs handlers
		runConcurrently(manager0::finishProcessing, manager1::finishProcessing);

		assertEquals(1, globalHandler.counter.get());
	}

	private static void runConcurrently(Runnable r0, Runnable r1) throws Exception {
		try (ExecutorService exec = Executors.newFixedThreadPool(2)) {
			var f0 = exec.submit(r0);
			var f1 = exec.submit(r1);
			f0.get();
			f1.get();
		}
	}

	@DistributedEventHandler(value = DistributedMode.GLOBAL)
	private static class GlobalTestHandler implements TestEvent.Handler {

		AtomicInteger counter = new AtomicInteger(0);

		@Override
		public void handleEvent(TestEvent event) {
			counter.incrementAndGet();
		}
	}

	@DistributedEventHandler(value = DistributedMode.NODE)
	private record NodeTestHandler(List<String> expectedTypes) implements TestEvent.Handler {

		@Override
		public void handleEvent(TestEvent event) {
			var expectedType = removeExpected();
			assertEquals(expectedType, event.data);
		}

		synchronized String removeExpected() {
			return expectedTypes.removeFirst();
		}
	}

	@DistributedEventHandler(value = DistributedMode.NODE_CONCURRENT)
	private record NodeConcurrentTestHandler(List<String> expectedData) implements TestEvent.Handler {

		@Override
		public void handleEvent(TestEvent event) {
			var expectedType = removeExpected();
			assertEquals(expectedType, event.data);
		}

		synchronized String removeExpected() {
			return expectedData.removeFirst();
		}
	}

	@DistributedEventHandler(value = DistributedMode.NODE_CONCURRENT, processing = ProcessingMode.DIRECT)
	private record NodeConcurrentDirectTestHandler(List<String> expectedData) implements TestEvent.Handler {

		@Override
		public void handleEvent(TestEvent event) {
			var expectedType = removeExpected();
			assertEquals(expectedType, event.data);
		}

		synchronized String removeExpected() {
			return expectedData.removeFirst();
		}
	}

	@DistributedEventHandler(value = DistributedMode.PARTITION, processing = ProcessingMode.DIRECT)
	private static class PartitionHandler implements TestEvent.Handler {

		final AtomicInteger counter = new AtomicInteger(0);

		@Override
		public void handleEvent(TestEvent event) {
			counter.incrementAndGet();
		}
	}

	private static class PartitionHandlerProvider implements Provider<PartitionHandler> {

		final List<PartitionHandler> handlers = new ArrayList<>();

		@Override
		public PartitionHandler get() {
			var handler = new PartitionHandler();
			handlers.add(handler);
			return handler;
		}
	}

	/**
	 * This class must be public so that it can be picked up by the serializtion provider.
	 */
	public static class TestEvent extends Event {

		final String data;

		public TestEvent(double time, String data) {
			super(time);
			this.data = data;
		}

		@Override
		public String getEventType() {
			return "DistributedEventsManagerTest::TestEvent";
		}

		interface Handler extends EventHandler {
			// it is in fact used, but through the events manager and not directly in the test code.
			@SuppressWarnings("unused")
			void handleEvent(TestEvent e);
		}
	}

	public static class SubclassedEvent extends TestEvent {
		public SubclassedEvent(double time, String data) {
			super(time, data);
		}

		@Override
		public String getEventType() {
			return "DistributedEventsManagerTest::SubClassedEvent";
		}

		interface Handler extends EventHandler {

			void handleEvent(SubclassedEvent e);
		}
	}
}
