package org.matsim.dsim;

import com.google.inject.Provider;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.handler.DistributedEventHandler;
import org.matsim.api.core.v01.events.handler.DistributedMode;
import org.matsim.api.core.v01.events.handler.ProcessingMode;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.serialization.SerializationProvider;
import org.matsim.dsim.executors.LPExecutor;
import org.matsim.dsim.executors.SingleExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class DistributedEventsManagerTest {

	@Test
	public void globalHandler() {
		MessageBroker broker = mock(MessageBroker.class);
		var provider = new SerializationProvider();
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
		var provider = new SerializationProvider();
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
		var provider = new SerializationProvider();
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
		var provider = new SerializationProvider();
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
		var provider = new SerializationProvider();
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
		var serializationProvider = new SerializationProvider();
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
		var serializationProvider = new SerializationProvider();
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
}
