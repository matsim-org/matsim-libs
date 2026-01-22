package org.matsim.dsim;

import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.handler.AggregatingEventHandler;
import org.matsim.api.core.v01.events.handler.DistributedEventHandler;
import org.matsim.api.core.v01.events.handler.DistributedMode;
import org.matsim.api.core.v01.events.handler.ProcessingMode;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.core.communication.NullCommunicator;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.serialization.SerializationProvider;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultEventHandlerTaskTest {

	@Test
	public void syncTimeStep() {

		var handler1 = new TestEventHandler();
		var handler2 = new TestEventHandler();
		var serializationProvider = new SerializationProvider();
		var computeNode = ComputeNode.builder()
			.rank(0)
			.parts(IntList.of(0, 1))
			.distributed(true)
			.build();
		var topology = Topology.builder()
			.computeNodes(List.of(computeNode))
			.totalPartitions(2)
			.build();
		MessageBroker broker = new MessageBroker(new NullCommunicator(), topology);
		var em = mock(DistributedEventsManager.class);
		when(em.getComputeNode()).thenReturn(computeNode);

		var task1 = new DefaultEventHandlerTask(handler1, 0, 2, em, serializationProvider, null);
		task1.setBroker(broker);
		broker.register(task1, 0);
		var task2 = new DefaultEventHandlerTask(handler2, 1, 2, em, serializationProvider, null);
		task2.setBroker(broker);
		broker.register(task2, 1);

		var e1 = new GenericEvent("test event 1", 1.);
		task1.add(e1);

		// this should process e1 in the handleEvent method of the handler
		task1.setTime(5);
		task1.beforeExecution();
		task1.run();

		assertEquals(e1, handler1.handledEvent);
		assertEquals(0, handler1.handledMessages.size());

		// this should trigger a sync message to other partitions.
		task1.setTime(6);
		task1.beforeExecution();
		task1.run();

		// the message is not yet received.
		assertEquals(0, handler1.handledMessages.size());
		assertEquals(0, handler2.handledMessages.size());

		task2.setTime(7);
		task2.beforeExecution();
		task2.run();

		// handler 1 receives no message
		assertEquals(0, handler1.handledMessages.size());
		//handler 2 receives one message
		assertEquals(1, handler2.handledMessages.size());
		// handler 1 still has the one direct event
		assertEquals(e1, handler1.handledEvent);
		// handler2 has received no events
		assertNull(handler2.handledEvent);
	}

	@Test
	public void oneCleanupSync() {

		var handler1 = new TestEventHandler();
		var handler2 = new TestEventHandler();
		var serializationProvider = new SerializationProvider();
		var computeNode = ComputeNode.builder()
			.rank(0)
			.parts(IntList.of(0, 1))
			.distributed(true)
			.build();
		var topology = Topology.builder()
			.computeNodes(List.of(computeNode))
			.totalPartitions(2)
			.build();
		MessageBroker broker = new MessageBroker(new NullCommunicator(), topology);
		var em = mock(DistributedEventsManager.class);
		when(em.getComputeNode()).thenReturn(computeNode);

		var task1 = new DefaultEventHandlerTask(handler1, 0, 2, em, serializationProvider, null);
		task1.setBroker(broker);
		broker.register(task1, 0);
		var task2 = new DefaultEventHandlerTask(handler2, 1, 2, em, serializationProvider, null);
		task2.setBroker(broker);
		broker.register(task2, 1);

		// this should trigger a send once, even though it is not the sync interval
		task1.cleanup();
		task1.setTime(5);
		task1.beforeExecution();
		task1.run();
		task2.beforeExecution();
		task2.run();
		assertEquals(0, handler1.handledMessages.size());
		assertEquals(1, handler2.handledMessages.size());

		// try a second time
		task1.run();
		assertEquals(0, handler1.handledMessages.size());
		assertEquals(1, handler2.handledMessages.size());
	}

	@DistributedEventHandler(value = DistributedMode.PARTITION, processing = ProcessingMode.DIRECT)
	public static class TestEventHandler implements BasicEventHandler, AggregatingEventHandler<TestMessage> {

		private Event handledEvent;
		private final List<TestMessage> handledMessages = new ArrayList<>();

		@Override
		public void handleEvent(Event event) {
			this.handledEvent = event;
		}

		@Override
		public TestMessage send() {
			return new TestMessage("test");
		}

		@Override
		public void receive(List<TestMessage> messages) {
			this.handledMessages.addAll(messages);
		}

		@Override
		public double getSyncInterval() {
			return 3;
		}
	}

	public record TestMessage(String data) implements Message {
	}

}
