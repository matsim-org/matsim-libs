package org.matsim.dsim.simulation;

import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.api.core.v01.network.NetworkPartitioning;
import org.matsim.dsim.MessageBroker;
import org.matsim.dsim.TestUtils;
import org.matsim.dsim.messages.SimStepMessage2;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link PartitionTransfer}.
 *
 * <p>The distributed three-link network from {@link TestUtils} assigns:
 * l1 → partition 0, l2 → partition 1, l3 → partition 2.
 * Node rank 0 owns partition 0.
 */
class PartitionTransferTest {

	// Two minimal Message implementations with distinct types (different class → different hashCode).
	record MessageA() implements Message {}

	record MessageB() implements Message {}

	private MessageBroker broker;
	private PartitionTransfer transfer;

	@BeforeEach
	void setUp() {
		var network = TestUtils.createDistributedThreeLinkNetwork();
		var node0 = ComputeNode.builder().rank(0).parts(IntList.of(0)).cores(1).hostname("localhost").build();
		var partitioning = new NetworkPartitioning(node0, network);

		broker = mock(MessageBroker.class);
		when(broker.getRank()).thenReturn(0);

		transfer = new PartitionTransfer(partitioning, broker);
	}

	// --- collect(Message, Id<Link>) ---

	@Test
	void collectByLink_resolvesPartitionAndSendsImmediately() {
		var msg = new MessageA();
		// l2 belongs to partition 1
		transfer.collect(msg, Id.createLinkId("l2"));

		verify(broker).send(msg, 1);
		verifyNoMoreInteractions(broker);
	}

	@Test
	void collectByLink_usesCorrectPartitionForEachLink() {
		var msg = new MessageA();
		// l3 belongs to partition 2
		transfer.collect(msg, Id.createLinkId("l3"));

		verify(broker).send(msg, 2);
	}

	// --- collect(Message, int) + send() ---

	@Test
	void collectByRank_singleMessage_sentOnFlush() {
		var msg = new MessageA();
		transfer.collect(msg, 1);
		transfer.send(42.0);

		var captor = ArgumentCaptor.forClass(Message.class);
		verify(broker).send(captor.capture(), eq(1));

		var sent = (SimStepMessage2) captor.getValue();
		assertEquals(42.0, sent.timeStep(), 1e-9);
		assertEquals(msg.getType(), sent.messageType());
		assertEquals(1, sent.messages().size());
		assertSame(msg, sent.messages().getFirst());
	}

	@Test
	void collectByRank_multipleMessagesOfSameType_batchedIntoOneSimStepMessage() {
		var m1 = new MessageA();
		var m2 = new MessageA();
		var m3 = new MessageA();
		transfer.collect(m1, 1);
		transfer.collect(m2, 1);
		transfer.collect(m3, 1);
		transfer.send(10.0);

		var captor = ArgumentCaptor.forClass(Message.class);
		verify(broker, times(1)).send(captor.capture(), eq(1));

		var sent = (SimStepMessage2) captor.getValue();
		assertEquals(3, sent.messages().size());
		assertTrue(sent.messages().containsAll(java.util.List.of(m1, m2, m3)));
	}

	@Test
	void collectByRank_messagesOfDifferentTypes_sentAsSeparateSimStepMessages() {
		var msgA = new MessageA();
		var msgB = new MessageB();
		transfer.collect(msgA, 1);
		transfer.collect(msgB, 1);
		transfer.send(5.0);

		var captor = ArgumentCaptor.forClass(Message.class);
		verify(broker, times(2)).send(captor.capture(), eq(1));

		var sentMessages = captor.getAllValues();
		assertEquals(2, sentMessages.size());
		// Each SimStepMessage2 must contain exactly one message of the correct type
		var types = sentMessages.stream()
			.map(m -> (SimStepMessage2) m)
			.map(SimStepMessage2::messageType)
			.toList();
		assertTrue(types.contains((long) msgA.getType()));
		assertTrue(types.contains((long) msgB.getType()));
	}

	@Test
	void collectByRank_messagesToDifferentRanks_sentPerRank() {
		var msgToRank1 = new MessageA();
		var msgToRank2 = new MessageA();
		transfer.collect(msgToRank1, 1);
		transfer.collect(msgToRank2, 2);
		transfer.send(0.0);

		var rankCaptor = ArgumentCaptor.forClass(Integer.class);
		var msgCaptor = ArgumentCaptor.forClass(Message.class);
		verify(broker, times(2)).send(msgCaptor.capture(), rankCaptor.capture());

		var ranks = rankCaptor.getAllValues();
		assertTrue(ranks.contains(1));
		assertTrue(ranks.contains(2));
	}

	// --- send() edge cases ---

	@Test
	void send_withNoMessages_doesNotCallBroker() {
		transfer.send(99.0);
		verify(broker, never()).send(any(), anyInt());
	}

	@Test
	void send_clearsBufferAfterFlushing() {
		transfer.collect(new MessageA(), 1);
		transfer.send(1.0);
		transfer.send(2.0);  // second send — buffer should be empty

		// send() called for SimStepMessage2 only once (from first send)
		verify(broker, times(1)).send(any(SimStepMessage2.class), anyInt());
	}

	// --- isLocal() ---

	@Test
	void isLocal_linkInOwnPartition_returnsTrue() {
		// l1 is partition 0, node rank 0 owns partition 0
		assertTrue(transfer.isLocal(Id.createLinkId("l1")));
	}

	@Test
	void isLocal_linkInRemotePartition_returnsFalse() {
		// l2 is partition 1, not owned by rank 0
		assertFalse(transfer.isLocal(Id.createLinkId("l2")));
	}

	// --- getRank() ---

	@Test
	void getRank_returnsCorrectPartitionForLink() {
		assertEquals(1, transfer.getRank(Id.createLinkId("l2")));
		assertEquals(2, transfer.getRank(Id.createLinkId("l3")));
		assertEquals(0, transfer.getRank(Id.createLinkId("l1")));
	}
}
