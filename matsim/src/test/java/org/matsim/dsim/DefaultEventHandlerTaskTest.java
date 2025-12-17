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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;

class DefaultEventHandlerTaskTest {

	@Test
	public void buildPattern() {

		var handler = new TestEventHandler();
		var em = mock(DistributedEventsManager.class);
		var serializationProvider = new SerializationProvider();
		var computeNode = ComputeNode.builder()
			.rank(0)
			.parts(IntList.of(0, 1))
			.build();
		var topology = Topology.builder()
			.computeNodes(List.of(computeNode))
			.totalPartitions(2)
			.build();
		MessageBroker broker = new MessageBroker(new NullCommunicator(), topology);

		var counter = new AtomicInteger();
		var task1 = new DefaultEventHandlerTask(handler, 0, 2, em, serializationProvider, null);
		task1.setBroker(broker);
		broker.register(task1, 0);
		var task2 = new DefaultEventHandlerTask(handler, 1, 2, em, serializationProvider, null);
		task2.setBroker(broker);
		broker.register(task2, 1);

		task1.add(new GenericEvent("test event 1", 1.));
		task2.add(new GenericEvent("test event 2", 1.));
		task1.beforeExecution();
		task1.run();
		task2.beforeExecution();
		task2.run();
	}

	@DistributedEventHandler(value = DistributedMode.PARTITION, processing = ProcessingMode.DIRECT)
	public static class TestEventHandler implements BasicEventHandler, AggregatingEventHandler<TestMessage> {

		@Override
		public void handleEvent(Event event) {
			System.out.println(event);
		}

		@Override
		public TestMessage send() {
			return new TestMessage("test");
		}

		@Override
		public void receive(List<TestMessage> messages) {
			for (var msg : messages) {
				System.out.println(msg.data());
			}
		}
	}

	public record TestMessage(String data) implements Message {
	}

}
