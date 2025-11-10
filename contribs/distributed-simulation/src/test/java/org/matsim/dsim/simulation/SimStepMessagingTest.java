package org.matsim.dsim.simulation;

import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.api.core.v01.messages.EmptyMessage;
import org.matsim.api.core.v01.network.NetworkPartitioning;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.dsim.SimStepMessage;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.dsim.MessageBroker;
import org.matsim.dsim.TestUtils;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SimStepMessagingTest {

	@Test
	public void init() {

		var network = TestUtils.createDistributedThreeLinkNetwork();
		network.setPartitioning(new NetworkPartitioning(ComputeNode.SINGLE_INSTANCE, network));
		MessageBroker messageBroker = mock(MessageBroker.class);

		var messaging = new SimStepMessaging(network, network.getPartitioning().getPartition(1), messageBroker);

		assertFalse(messaging.isLocal(Id.createLinkId("l1")));
		assertTrue(messaging.isLocal(Id.createLinkId("l2")));
		assertFalse(messaging.isLocal(Id.createLinkId("l3")));
	}

	@Test
	public void sendNullMessage() {

		var network = TestUtils.createDistributedThreeLinkNetwork();
		network.setPartitioning(new NetworkPartitioning(ComputeNode.SINGLE_INSTANCE, network));
		var messageBroker = mock(MessageBroker.class);
		var part = 0;

		var messaging = new SimStepMessaging(network, network.getPartitioning().getPartition(part), messageBroker);
		messaging.sendMessages(0);
		ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
		ArgumentCaptor<Integer> targetPartCaptor = ArgumentCaptor.forClass(Integer.class);

		verify(messageBroker, times(1)).send(messageCaptor.capture(), targetPartCaptor.capture());
		assertEquals(1, targetPartCaptor.getValue());
		assertInstanceOf(SimStepMessage.class, messageCaptor.getValue());

		SimStepMessage simStepMessage = (SimStepMessage) messageCaptor.getValue();
		assertEquals(0, simStepMessage.capUpdates().size());
		assertEquals(0, simStepMessage.teleportations().size());
		assertEquals(0, simStepMessage.vehicles().size());
	}

	@Test
	public void collectTeleportation() {
		var network = TestUtils.createDistributedThreeLinkNetwork();
		network.setPartitioning(new NetworkPartitioning(ComputeNode.SINGLE_INSTANCE, network));
		var messageBroker = mock(MessageBroker.class);
		var part = 0;

		var messaging = new SimStepMessaging(network, network.getPartitioning().getPartition(part), messageBroker);

		var remotePerson = mock(DistributedMobsimAgent.class);
		when(remotePerson.getDestinationLinkId()).thenReturn(Id.createLinkId("l3"));
		when(remotePerson.toMessage()).thenReturn(EmptyMessage.INSTANCE);

		var neighborPerson = mock(DistributedMobsimAgent.class);
		when(neighborPerson.getDestinationLinkId()).thenReturn(Id.createLinkId("l2"));
		when(neighborPerson.toMessage()).thenReturn(EmptyMessage.INSTANCE);

		var localPerson = mock(DistributedMobsimAgent.class);
		when(localPerson.getDestinationLinkId()).thenReturn(Id.createLinkId("l1"));
		when(localPerson.toMessage()).thenReturn(EmptyMessage.INSTANCE);

		messaging.collectTeleportation(remotePerson, 10);
		messaging.collectTeleportation(neighborPerson, 10);
		messaging.collectTeleportation(localPerson, 10);
		messaging.sendMessages(0);

		verify(messageBroker, times(3)).send(assertArg(message -> {
			assertInstanceOf(SimStepMessage.class, message);
			SimStepMessage simStepMessage = (SimStepMessage) message;
			//assertEquals(1, simStepMessage.teleportations().size());
			System.out.println(simStepMessage.teleportations().size());
		}), anyInt());
	}

	@Test
	public void collectVehicle() {

		var network = TestUtils.createDistributedThreeLinkNetwork();
		network.setPartitioning(new NetworkPartitioning(ComputeNode.SINGLE_INSTANCE, network));
		var messageBroker = mock(MessageBroker.class);
		var part = 0;

		var messaging = new SimStepMessaging(network, network.getPartitioning().getPartition(part), messageBroker);
		var vehicle = mock(DistributedMobsimVehicle.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
		when(vehicle.getCurrentLinkId()).thenReturn(Id.createLinkId("l2"));
		when(vehicle.toMessage()).thenReturn(EmptyMessage.INSTANCE);
		when(vehicle.getDriver()).thenReturn(mock(MobsimDriverAgent.class, withSettings().extraInterfaces(DistributedMobsimAgent.class)));
		when(vehicle.getPassengers()).thenReturn(List.of());

		messaging.collectVehicle(vehicle);
		verify(messageBroker, times(0)).send(any(), anyInt());

		messaging.sendMessages(0);
		verify(messageBroker, times(1)).send(assertArg(message -> {
			var simStepMessage = (SimStepMessage) message;
			assertEquals(1, simStepMessage.vehicles().size());
			assertEquals(EmptyMessage.INSTANCE, simStepMessage.vehicles().getFirst().vehicle());

		}), anyInt());
	}

	@Test
	public void collectStorageCapacityUpdates() {

		var simNode = ComputeNode.builder().parts(IntList.of(0, 1, 2)).build();
		var network = TestUtils.createDistributedThreeLinkNetwork();
		network.setPartitioning(new NetworkPartitioning(simNode, network));
		var messageBroker = mock(MessageBroker.class);
		var part = 1;

		var messaging = new SimStepMessaging(network, network.getPartitioning().getPartition(part), messageBroker);

		messaging.collectStorageCapacityUpdate(Id.createLinkId("l1"), 10, 5, 0);
		verify(messageBroker, times(0)).send(any(), anyInt());

		var messagePassedToBroker = new AtomicBoolean(false);
		messaging.sendMessages(0);
		verify(messageBroker, times(2)).send(assertArg(message -> {
			var simStepMessage = (SimStepMessage) message;
			if (simStepMessage.capUpdates().size() == 1) {
				var update = simStepMessage.capUpdates().getFirst();
				assertEquals(10, update.released());
				assertEquals(5, update.consumed());
				assertEquals(Id.createLinkId("l1"), update.linkId());
				messagePassedToBroker.set(true);
			}
		}), anyInt());

		assertTrue(messagePassedToBroker.get(), "Message broker did not receive message with capacity updates.");
	}

	@Test
	public void clearMessages() {

		var network = TestUtils.createDistributedThreeLinkNetwork();
		network.setPartitioning(new NetworkPartitioning(ComputeNode.SINGLE_INSTANCE, network));
		var messageBroker = mock(MessageBroker.class);
		var part = 2;

		var messaging = new SimStepMessaging(network, network.getPartitioning().getPartition(part), messageBroker);
		messaging.collectStorageCapacityUpdate(Id.createLinkId("l2"), 10, 5, 1);
		verify(messageBroker, times(0)).send(any(), anyInt());

		messaging.sendMessages(0);
		verify(messageBroker, times(1)).send(assertArg(message -> {
			var simStepMessage = (SimStepMessage) message;
			assertEquals(1, simStepMessage.capUpdates().size());
			assertEquals(0, simStepMessage.teleportations().size());
			assertEquals(0, simStepMessage.vehicles().size());
		}), anyInt());

		reset(messageBroker);
		messaging.sendMessages(0);
		ArgumentCaptor<Integer> targetPartCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(messageBroker, times(1)).send(assertArg(message -> {
			var simStepMessage = (SimStepMessage) message;
			assertEquals(0, simStepMessage.capUpdates().size());
			assertEquals(0, simStepMessage.teleportations().size());
			assertEquals(0, simStepMessage.vehicles().size());
		}), targetPartCaptor.capture());
		assertEquals(1, targetPartCaptor.getValue());
	}
}
