package org.matsim.dsim.simulation;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.messages.Empty;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.framework.DistributedMobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.DistributedMobsimVehicle;
import org.matsim.dsim.MessageBroker;
import org.matsim.dsim.QSimCompatibility;
import org.matsim.dsim.TestUtils;
import org.matsim.dsim.messages.SimStepMessage;
import org.matsim.dsim.messages.VehicleContainer;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Log4j2
class SimStepMessagingTest {

    @Test
    public void init() {

		Network network = TestUtils.createDistributedThreeLinkNetwork();
		MessageBroker messageBroker = mock(MessageBroker.class);
		QSimCompatibility qsim = mock(QSimCompatibility.class);
		IntOpenHashSet neighbors = new IntOpenHashSet(new int[]{0, 2});
		int part = 1;

        var messaging = new SimStepMessaging.SimStepMessagingImpl(
                network, messageBroker, qsim, neighbors, part);

        assertEquals(neighbors, messaging.getNeighbors());
        assertEquals(part, messaging.getPart());
        assertFalse(messaging.isLocal(Id.createLinkId("l1")));
        assertTrue(messaging.isLocal(Id.createLinkId("l2")));
        assertFalse(messaging.isLocal(Id.createLinkId("l3")));
    }

    @Test
    public void sendNullMessage() {

        var network = TestUtils.createDistributedThreeLinkNetwork();
        var messageBroker = mock(MessageBroker.class);
		var qsim = mock(QSimCompatibility.class);
        var neighbors = new IntOpenHashSet(new int[]{1});
        var part = 0;

        var messaging = new SimStepMessaging.SimStepMessagingImpl(
                network, messageBroker, qsim, neighbors, part);
        messaging.sendMessages(0);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        ArgumentCaptor<Integer> targetPartCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(messageBroker, times(1)).send(messageCaptor.capture(), targetPartCaptor.capture());
        assertEquals(1, targetPartCaptor.getValue());
        assertInstanceOf(SimStepMessage.class, messageCaptor.getValue());

        SimStepMessage simStepMessage = (SimStepMessage) messageCaptor.getValue();
        assertEquals(0, simStepMessage.getCapacityUpdateMsgsCount());
        assertEquals(0, simStepMessage.getTeleportationMsgsCount());
        assertEquals(0, simStepMessage.getVehicleMsgsCount());
    }

    @Test
    public void collectTeleportation() {
        var network = TestUtils.createDistributedThreeLinkNetwork();
        var messageBroker = mock(MessageBroker.class);
		var qsim = mock(QSimCompatibility.class);
		var neighbors = new IntOpenHashSet(new int[]{1});
        var part = 0;

        var messaging = SimStepMessaging.create(network, messageBroker, qsim, neighbors, part);

        var remotePerson = mock(DistributedMobsimAgent.class);
        when(remotePerson.getDestinationLinkId()).thenReturn(Id.createLinkId("l3"));
        when(remotePerson.toMessage()).thenReturn(Empty.INSTANCE);

        var neighborPerson = mock(DistributedMobsimAgent.class);
        when(neighborPerson.getDestinationLinkId()).thenReturn(Id.createLinkId("l2"));
        when(neighborPerson.toMessage()).thenReturn(Empty.INSTANCE);

		var localPerson = mock(DistributedMobsimAgent.class);
        when(localPerson.getDestinationLinkId()).thenReturn(Id.createLinkId("l1"));
        when(localPerson.toMessage()).thenReturn(Empty.INSTANCE);

        messaging.collectTeleportation(remotePerson, 10);
        messaging.collectTeleportation(neighborPerson, 10);
        messaging.collectTeleportation(localPerson, 10);
        messaging.sendMessages(0);

        verify(messageBroker, times(3)).send(assertArg(message -> {
            assertInstanceOf(SimStepMessage.class, message);
			SimStepMessage simStepMessage = (SimStepMessage) message;
            assertEquals(1, simStepMessage.getTeleportationMsgsCount());
        }), anyInt());
    }

    @Test
    public void collectVehicle() {
        var network = TestUtils.createDistributedThreeLinkNetwork();
        var messageBroker = mock(MessageBroker.class);
		var qsim = mock(QSimCompatibility.class);
		var neighbors = new IntOpenHashSet(new int[]{1});
        var part = 0;

        var messaging = SimStepMessaging.create(network, messageBroker, qsim, neighbors, part);
        var vehicle = mock(DistributedMobsimVehicle.class);

		when(qsim.vehicleToContainer(any())).thenReturn(VehicleContainer.builder().setVehicle(Empty.INSTANCE).build());
        when(vehicle.getCurrentLinkId()).thenReturn(Id.createLinkId("l2"));
        when(vehicle.toMessage()).thenReturn(Empty.INSTANCE);

        messaging.collectVehicle(vehicle);
        verify(messageBroker, times(0)).send(any(), anyInt());

        messaging.sendMessages(0);
        verify(messageBroker, times(1)).send(assertArg(message -> {
            var simStepMessage = (SimStepMessage) message;
            assertEquals(1, simStepMessage.getVehicleMsgsCount());
            assertEquals(Empty.INSTANCE, simStepMessage.getVehicles().getFirst().getVehicle());

        }), anyInt());
    }

    @Test
    public void collectStorageCapacityUpdates() {

        var network = TestUtils.createDistributedThreeLinkNetwork();
        var messageBroker = mock(MessageBroker.class);
		var qsim = mock(QSimCompatibility.class);
		var neighbors = new IntOpenHashSet(new int[]{0, 2});
        var part = 1;

        var messaging = SimStepMessaging.create(network, messageBroker, qsim, neighbors, part);

        messaging.collectStorageCapacityUpdate(Id.createLinkId("l1"), 10, 5, 0);
        verify(messageBroker, times(0)).send(any(), anyInt());

        var messagePassedToBroker = new AtomicBoolean(false);
        messaging.sendMessages(0);
        verify(messageBroker, times(2)).send(assertArg(message -> {
            var simStepMessage = (SimStepMessage) message;
            if (simStepMessage.getCapacityUpdateMsgsCount() == 1) {
                var update = simStepMessage.getCapacityUpdates().get(0);
                assertEquals(10, update.getReleased());
                assertEquals(5, update.getConsumed());
                assertEquals(Id.createLinkId("l1"), update.getLinkId());
                messagePassedToBroker.set(true);
            }
        }), anyInt());

        assertTrue(messagePassedToBroker.get(), "Message broker did not receive message with capacity updates.");
    }

    @Test
    public void clearMessages() {

        var network = TestUtils.createDistributedThreeLinkNetwork();
        var messageBroker = mock(MessageBroker.class);
		var qsim = mock(QSimCompatibility.class);
		var neighbors = new IntOpenHashSet(new int[]{1});
        var part = 2;

        var messaging = SimStepMessaging.create(network, messageBroker, qsim, neighbors, part);
        messaging.collectStorageCapacityUpdate(Id.createLinkId("l2"), 10, 5, 1);
        verify(messageBroker, times(0)).send(any(), anyInt());

        messaging.sendMessages(0);
        verify(messageBroker, times(1)).send(assertArg(message -> {
            var simStepMessage = (SimStepMessage) message;
            assertEquals(1, simStepMessage.getCapacityUpdateMsgsCount());
            assertEquals(0, simStepMessage.getTeleportationMsgsCount());
            assertEquals(0, simStepMessage.getVehicleMsgsCount());
        }), anyInt());

        reset(messageBroker);
        messaging.sendMessages(0);
        ArgumentCaptor<Integer> targetPartCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(messageBroker, times(1)).send(assertArg(message -> {
            var simStepMessage = (SimStepMessage) message;
            assertEquals(0, simStepMessage.getCapacityUpdateMsgsCount());
            assertEquals(0, simStepMessage.getTeleportationMsgsCount());
            assertEquals(0, simStepMessage.getVehicleMsgsCount());
        }), targetPartCaptor.capture());
        assertEquals(1, targetPartCaptor.getValue());
    }
}
