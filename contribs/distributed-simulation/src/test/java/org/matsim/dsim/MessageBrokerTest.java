package org.matsim.dsim;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.apache.fory.memory.MemoryBuffer;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.LP;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.api.core.v01.messages.EmptyMessage;
import org.matsim.core.communication.Communicator;
import org.matsim.core.communication.MessageConsumer;
import org.matsim.core.communication.MessageReceiver;
import org.matsim.core.serialization.SerializationProvider;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MessageBrokerTest {

	// Topology: rank 0 → partitions [0,1], rank 1 → partitions [2,3], rank 2 → partitions [4,5]
	private static final Topology TOPOLOGY = buildTopology();
	private static final SerializationProvider serializer = SerializationProvider.getInstance();

	private static Topology buildTopology() {
		var node0 = ComputeNode.builder().distributed(true).rank(0).parts(IntList.of(0, 1)).cores(1).hostname("localhost").build();
		var node1 = ComputeNode.builder().distributed(true).rank(1).parts(IntList.of(2, 3)).cores(1).hostname("localhost").build();
		var node2 = ComputeNode.builder().distributed(true).rank(2).parts(IntList.of(4, 5)).cores(1).hostname("localhost").build();

		return Topology.builder()
			.totalPartitions(6)
			.computeNodes(List.of(node0, node1, node2))
			.build();
	}

	private static Communicator mockComm() {
		var comm = mock(Communicator.class);
		when(comm.getRank()).thenReturn(0);
		when(comm.getSize()).thenReturn(TOPOLOGY.getNodesCount());
		return comm;
	}

	@Test
	void waitForRank() throws IOException {
		var comm = mockComm();
		var broker = new MessageBroker(comm, TOPOLOGY, serializer);

		// register rank to wait for
		var waitForRank = 1;
		broker.syncFromRank(waitForRank);
		assertTrue(broker.expectsMoreMessages());

		// clear the rank by simulating message receive
		broker.beforeSimStep(0);
		var msg = MessageBroker.headerFor(seq(0), waitForRank, broker.getRank());
		msg.flip();
		broker.consume(msg);
		assertFalse(broker.expectsMoreMessages());
	}

	@Test
	void dontWaitForOwnRank() {
		var comm = mockComm();
		var broker = new MessageBroker(comm, TOPOLOGY, serializer);

		broker.syncFromRank(broker.getRank());
		assertFalse(broker.expectsMoreMessages());
	}

	@Test
	void sendEmptyMessageToOtherRank() {
		var comm = mockComm();
		var broker = new MessageBroker(comm, TOPOLOGY, serializer);

		broker.beforeSimStep(0);
		broker.syncToRank(1);
		broker.syncTimestep(0, false);

		verify(comm, times(1)).send(eq(1), any(), anyLong(), anyLong());
	}

	@Test
	void sendOneEmptyMessageToRemotePart() {
		var comm = mockComm();
		var broker = new MessageBroker(comm, TOPOLOGY, serializer);

		broker.beforeSimStep(0);
		// add both partitions of the 'otherRank'
		var parts = TOPOLOGY.getNodeByIndex(1).getParts();
		broker.syncToPart(parts.getFirst());
		broker.syncToPart(parts.getInt(1));
		broker.syncTimestep(0, false);

		// the message broker should only send one message to rank 1
		verify(comm, times(1)).send(eq(1), any(), anyLong(), anyLong());
	}

	@Test
	void sendNoEmptyMessageToOwnRank() {
		var comm = mockComm();
		var broker = new MessageBroker(comm, TOPOLOGY, serializer);

		broker.beforeSimStep(0);
		broker.syncToPart(TOPOLOGY.getNodeByIndex(broker.getRank()).getParts().getFirst());
		broker.syncToRank(broker.getRank());
		broker.syncTimestep(0, false);

		verify(comm, never()).send(eq(1), any(), anyLong(), anyLong());
	}

	@Test
	void receiveEmptyMessage() {
		var otherRank = 1;
		var time = 43;
		var receivedBytes = msgBytes(otherRank, 0, MessageBroker.ANY_PARTITION, EmptyMessage.INSTANCE, MessageBroker.seqFrom(time));
		var comm = mockComm();
		doAnswer(i -> {
			MessageReceiver expectsNext = i.getArgument(0);
			MessageConsumer msgConsumer = i.getArgument(1);

			// the communicator expects a sync message
			assertTrue(expectsNext.expectsMoreMessages());
			msgConsumer.consume(receivedBytes);
			// once it has received the sync message, the communicator should have cleared its waitForRank list.
			assertFalse(expectsNext.expectsMoreMessages());

			return null;
		}).when(comm).recv(any(), any());

		var task = mock(LPTask.class);
		when(task.getSupportedMessages()).thenReturn(IntSet.of(EmptyMessage.INSTANCE.getType()));
		var waitParts = new IntArraySet(TOPOLOGY.getNodeByIndex(otherRank).getParts());
		when(task.waitForOtherParts(anyDouble())).thenReturn(waitParts);

		var broker = new MessageBroker(comm, TOPOLOGY, serializer);
		broker.register(task, 0);
		broker.beforeSimStep(time);
		broker.syncFromRank(otherRank);

		broker.syncTimestep(time, false);

		// the broker should not send any messages, as we didn't register any ranks to sync to
		verify(comm, never()).send(anyInt(), any(), anyLong(), anyLong());
		// still, the broker should call recv once. This will trigger the 'doAnswer' on top.
		verify(comm, times(1)).recv(any(), any());
		// the recv should not pass any messages to the task, as empty messages are not dispatched to tasks
		verify(task, never()).add(any());
		// the broker should have cleared the wait ranks
		assertFalse(broker.expectsMoreMessages());
	}

	@Test
	void sendReceiveEmptyMessages() {
		var otherRank = 1;
		var receivedBytes = msgBytes(otherRank, 0, MessageBroker.ANY_PARTITION, EmptyMessage.INSTANCE, seq(0));

		var comm = mockComm();
		doAnswer(i -> {
			MessageReceiver expectsNext = i.getArgument(0);
			MessageConsumer msgConsumer = i.getArgument(1);

			// the receiver task has registered one wait rank
			assertTrue(expectsNext.expectsMoreMessages());
			// this should clear the wait ranks
			msgConsumer.consume(receivedBytes);
			assertFalse(expectsNext.expectsMoreMessages());

			return null;
		}).when(comm).recv(any(), any());

		var broker = new MessageBroker(comm, TOPOLOGY, serializer);
		var receiverTask = mock(LPTask.class);
		when(receiverTask.getSupportedMessages()).thenReturn(IntSet.of(serializer.getType(MessageA.class)));
		var waitParts = new IntArraySet(TOPOLOGY.getNodeByIndex(otherRank).getParts());
		when(receiverTask.waitForOtherParts(anyDouble())).thenReturn(waitParts);
		broker.register(receiverTask, 0);
		broker.beforeSimStep(0);
		broker.syncToRank(otherRank);

		broker.syncTimestep(0, false);

		// we expect one empty message to be sent
		verify(comm, times(1)).send(eq(otherRank), any(), anyLong(), anyLong());
		// we expect the broker to call recv once. This will trigger the 'doAnswer' on top.
		verify(comm, times(1)).recv(any(), any());
		// since we are only receiving empty messages, it should never be dispatched to a task
		verify(receiverTask, never()).add(any());
	}

	@Test
	void sendMultipleRanks() {
		var comm = mockComm();
		var broker = new MessageBroker(comm, TOPOLOGY, serializer);

		var receiverTask = mock(LPTask.class);
		when(receiverTask.getSupportedMessages()).thenReturn(IntSet.of(serializer.getType(MessageA.class)));
		// we want to wait for / sync from both other ranks
		var waitParts = IntSet.of(2, 3, 4, 5);
		when(receiverTask.waitForOtherParts(anyDouble())).thenReturn(waitParts);
		broker.register(receiverTask, 0);
		broker.beforeSimStep(0);
		// we want to sync to both other ranks
		broker.syncToRank(1);
		broker.syncToRank(2);

		// we register one message to rank 1
		var msgToPart2 = new MessageA("message to part 2 on rank 1");
		broker.send(msgToPart2, 2);

		broker.syncTimestep(0, false);

		var msgCaptor = ArgumentCaptor.forClass(MemorySegment.class);
		var rankCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(comm, times(2)).send(rankCaptor.capture(), msgCaptor.capture(), anyLong(), anyLong());

		// verify ranks
		var rankArgs = rankCaptor.getAllValues();
		assertEquals(2, rankArgs.size());
		assertTrue(rankArgs.contains(1));
		assertTrue(rankArgs.contains(2));
		var iRank1 = rankArgs.indexOf(1);
		var iRank2 = rankArgs.indexOf(2);

		// verify messages
		var msgArgs = msgCaptor.getAllValues();
		assertEquals(2, msgArgs.size());

		MessageA msg1 = verifyMsg(seq(0), 1, 2, msgToPart2.getType(), msgArgs.get(iRank1).asByteBuffer());
		assertEquals(msgToPart2.payload(), msg1.payload());

		EmptyMessage msg2 = verifyMsg(seq(0), 2, MessageBroker.ANY_PARTITION, EmptyMessage.INSTANCE.getType(), msgArgs.get(iRank2).asByteBuffer());
		assertNotNull(msg2);
	}

	@Test
	void recvMultipleRanks() {
		var msgFromRank1 = EmptyMessage.INSTANCE;
		var msgFromRank2 = new MessageA("message from rank 2 to part 1");
		var bytesRank1 = msgBytes(1, 0, 0, msgFromRank1, seq(0));
		var bytesRank2 = msgBytes(2, 0, 1, msgFromRank2, seq(0));

		var comm = mockComm();
		doAnswer(i -> {
			MessageReceiver expectsNext = i.getArgument(0);
			MessageConsumer msgConsumer = i.getArgument(1);

			// the receiver task has registered two wait ranks
			assertTrue(expectsNext.expectsMoreMessages());
			msgConsumer.consume(bytesRank1);

			assertTrue(expectsNext.expectsMoreMessages());
			msgConsumer.consume(bytesRank2);

			assertFalse(expectsNext.expectsMoreMessages());

			return null;
		}).when(comm).recv(any(), any());

		var broker = new MessageBroker(comm, TOPOLOGY, serializer);
		var supportedMessages = IntSet.of(msgFromRank1.getType(), msgFromRank2.getType());
		var waitForParts = IntSet.of(2, 3, 4, 5);

		var receiverTask = mock(LPTask.class);
		when(receiverTask.getSupportedMessages()).thenReturn(supportedMessages);
		when(receiverTask.waitForOtherParts(anyDouble())).thenReturn(waitForParts);
		broker.register(receiverTask, 1);

		var nonReceivingTask = mock(LPTask.class);
		when(nonReceivingTask.getSupportedMessages()).thenReturn(supportedMessages);
		when(nonReceivingTask.waitForOtherParts(anyDouble())).thenReturn(waitForParts);
		broker.register(nonReceivingTask, 0);

		broker.beforeSimStep(0);
		broker.syncToRank(1);
		broker.syncToRank(2);

		broker.syncTimestep(0, false);

		// we expect two empty messages to be sent
		verify(comm, times(2)).send(anyInt(), any(), anyLong(), anyLong());
		// we expect the broker to call recv once. This will trigger the 'doAnswer' on top.
		verify(comm, times(1)).recv(any(), any());

		// receiver task should have received one message
		var captor = ArgumentCaptor.forClass(Message.class);
		verify(receiverTask, times(1)).add(captor.capture());
		MessageA received = (MessageA) captor.getValue();
		assertEquals(msgFromRank2.payload(), received.payload());

		// non receiver task should have received no messages, as the empty message should not be dispatched to tasks
		verify(nonReceivingTask, never()).add(any());
	}

	@Test
	public void sendBroadcast() {
		var comm = mockComm();
		var broker = new MessageBroker(comm, TOPOLOGY, serializer);

		var msgA = new MessageA("broadcast payload");
		broker.beforeSimStep(0);
		broker.send(msgA, Communicator.BROADCAST_TO_ALL);
		broker.syncTimestep(0, false);

		// Broadcast is queued into the shared slot and sent once with receiver == BROADCAST_TO_ALL.
		var msgCaptor = ArgumentCaptor.forClass(MemorySegment.class);
		verify(comm, times(1)).send(eq(Communicator.BROADCAST_TO_ALL), msgCaptor.capture(), anyLong(), anyLong());

		var type = serializer.getType(MessageA.class);
		MessageA received = verifyMsg(seq(0), Communicator.BROADCAST_TO_ALL, Communicator.BROADCAST_TO_ALL, type,
			msgCaptor.getValue().asByteBuffer());
		assertEquals(msgA.payload(), received.payload());
	}

	@Test
	public void recvBroadcast() {

		var msgFromRank1 = new MessageA("incoming broadcast from rank 1");
		var bytesFromRank1 = msgBytes(1, Communicator.BROADCAST_TO_ALL, Communicator.BROADCAST_TO_ALL, msgFromRank1, seq(0));

		var msgFromRank2 = new MessageA("incoming broadcast from rank 2");
		var bytesFromRank2 = msgBytes(2, Communicator.BROADCAST_TO_ALL, Communicator.BROADCAST_TO_ALL, msgFromRank2, seq(0));

		var comm = mockComm();
		doAnswer(i -> {
			MessageReceiver expectsNext = i.getArgument(0);
			MessageConsumer msgConsumer = i.getArgument(1);

			// since we are expecting an all parts broadcast, we expect message from both other ranks
			assertTrue(expectsNext.expectsMoreMessages());
			msgConsumer.consume(bytesFromRank1);

			assertTrue(expectsNext.expectsMoreMessages());
			msgConsumer.consume(bytesFromRank2);

			return null;
		}).when(comm).recv(any(), any());

		var task0 = mock(LPTask.class);
		when(task0.getSupportedMessages()).thenReturn(IntSet.of(serializer.getType(MessageA.class)));
		when(task0.waitForOtherParts(anyDouble())).thenReturn(LP.ALL_PARTS_BROADCAST);

		var task1 = mock(LPTask.class);
		when(task1.getSupportedMessages()).thenReturn(IntSet.of(serializer.getType(MessageA.class)));
		when(task1.waitForOtherParts(anyDouble())).thenReturn(LP.ALL_PARTS_BROADCAST);

		var broker = new MessageBroker(comm, TOPOLOGY, serializer);
		broker.register(task0, 0);
		broker.register(task1, 1);
		broker.beforeSimStep(0);

		broker.syncTimestep(0, false);

		verify(comm, never()).send(anyInt(), any(), anyLong(), anyLong());
		verify(comm, times(1)).recv(any(), any());

		// now, both tasks should receive both messages from rank 1 and from rank2
		var captor0 = ArgumentCaptor.forClass(Message.class);
		verify(task0, times(2)).add(captor0.capture());
		assertEquals(2, captor0.getAllValues().size());
		assertEquals(msgFromRank1.payload(), ((MessageA) captor0.getAllValues().getFirst()).payload());
		assertEquals(msgFromRank2.payload(), ((MessageA) captor0.getAllValues().getLast()).payload());

		var captor1 = ArgumentCaptor.forClass(Message.class);
		verify(task1, times(2)).add(captor1.capture());
		assertEquals(2, captor1.getAllValues().size());
		assertEquals(msgFromRank1.payload(), ((MessageA) captor1.getAllValues().getFirst()).payload());
		assertEquals(msgFromRank2.payload(), ((MessageA) captor1.getAllValues().getLast()).payload());
	}

	@Test
	public void sendBroadcastMessageAndEmptyMessage() {
		var comm = mockComm();
		var broker = new MessageBroker(comm, TOPOLOGY, serializer);

		var msgA = new MessageA("broadcast with empty");
		broker.beforeSimStep(0);
		broker.send(msgA, Communicator.BROADCAST_TO_ALL);
		broker.syncToRank(1);
		broker.syncToRank(2);
		broker.syncTimestep(0, false);

		// Broadcast goes into the shared slot (1 send to BROADCAST_TO_ALL).
		// syncToRank(1/2) each add a heartbeat because the per-rank buffers are empty → 2 more sends.
		var rankCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(comm, times(3)).send(rankCaptor.capture(), any(), anyLong(), anyLong());

		var rankArgs = rankCaptor.getAllValues();
		assertTrue(rankArgs.contains(Communicator.BROADCAST_TO_ALL));
		assertTrue(rankArgs.contains(1));
		assertTrue(rankArgs.contains(2));
	}

	@Test
	public void recvBroadcastMessageAndEmptyMessage() {

		var msgFromRank1 = new MessageA("incoming broadcast from rank 1");
		var bytesFromRank1 = msgBytes(1, Communicator.BROADCAST_TO_ALL, Communicator.BROADCAST_TO_ALL, msgFromRank1, seq(0));

		var msgFromRank2 = new MessageA("incoming broadcast from rank 2");
		var bytesFromRank2 = msgBytes(2, Communicator.BROADCAST_TO_ALL, Communicator.BROADCAST_TO_ALL, msgFromRank2, seq(0));

		var msgBFromRank2 = new MessageB("message B from rank 2");
		var msgBBytes = msgBytes(2, 0, 1, msgBFromRank2, seq(0));

		var comm = mockComm();
		doAnswer(i -> {

			MessageReceiver expectsNext = i.getArgument(0);
			MessageConsumer msgConsumer = i.getArgument(1);

			assertTrue(expectsNext.expectsMoreMessages());
			msgConsumer.consume(bytesFromRank1);

			assertTrue(expectsNext.expectsMoreMessages());
			msgConsumer.consume(msgBBytes);

			assertTrue(expectsNext.expectsMoreMessages());
			msgConsumer.consume(bytesFromRank2);

			assertFalse(expectsNext.expectsMoreMessages());

			return null;
		}).when(comm).recv(any(), any());

		var task0 = mock(LPTask.class);
		when(task0.getSupportedMessages()).thenReturn(IntSet.of(serializer.getType(MessageA.class)));
		when(task0.waitForOtherParts(anyDouble())).thenReturn(LP.ALL_PARTS_BROADCAST);

		var task1 = mock(LPTask.class);
		when(task1.getSupportedMessages()).thenReturn(IntSet.of(serializer.getType(MessageB.class)));
		// wait for partition 4 on rank 2
		when(task1.waitForOtherParts(anyDouble())).thenReturn(IntSet.of(4));

		var broker = new MessageBroker(comm, TOPOLOGY, serializer);
		broker.register(task0, 0);
		broker.register(task1, 1);
		broker.beforeSimStep(0);

		broker.syncTimestep(0, false);

		verify(comm, never()).send(anyInt(), any(), anyLong(), anyLong());
		verify(comm, times(1)).recv(any(), any());

		// now, task0 should receive the broadcast
		var captor0 = ArgumentCaptor.forClass(Message.class);
		verify(task0, times(2)).add(captor0.capture());
		assertEquals(2, captor0.getAllValues().size());
		assertEquals(msgFromRank1.payload(), ((MessageA) captor0.getAllValues().getFirst()).payload());
		assertEquals(msgFromRank2.payload(), ((MessageA) captor0.getAllValues().getLast()).payload());

		// now, task1 should receive the message from part2
		var captor1 = ArgumentCaptor.forClass(Message.class);
		verify(task1, times(1)).add(captor1.capture());
		assertEquals(msgBFromRank2.payload(), ((MessageB) captor1.getValue()).payload());
	}

	@Test
	void recvAheadMessage() {
		var time = 43;
		var aheadTime = time + 5;
		var aheadMsg = new MessageA("ahead message");
		var aheadBytes = msgBytes(1, 0, 0, aheadMsg, seq(aheadTime));
		var inSequenceMessage = new MessageA("in sequence message");
		var inSequenceBytes = msgBytes(1, 0, 0, inSequenceMessage, seq(time));

		var comm = mockComm();
		doAnswer(i -> {
			MessageReceiver expectsNext = i.getArgument(0);
			MessageConsumer msgConsumer = i.getArgument(1);

			// first message goes into ahead buffer
			assertTrue(expectsNext.expectsMoreMessages());
			msgConsumer.consume(aheadBytes);

			// second message is processed and the expected ranks are cleared.
			assertTrue(expectsNext.expectsMoreMessages());
			msgConsumer.consume(inSequenceBytes);
			assertFalse(expectsNext.expectsMoreMessages());

			return null;
		})
			// the second invocation does not have messages for the broker.
			.doAnswer(i -> {
				MessageReceiver expectsNext = i.getArgument(0);

				// the ahead message should be processed before receive and the ahead message should have cleared
				// the expected ranks.
				assertFalse(expectsNext.expectsMoreMessages());
				return null;
			})
			.when(comm).recv(any(), any());

		var task0 = mock(LPTask.class);
		when(task0.getSupportedMessages()).thenReturn(IntSet.of(serializer.getType(MessageA.class)));
		when(task0.waitForOtherParts(anyDouble())).thenReturn(IntSet.of(2));

		var broker = new MessageBroker(comm, TOPOLOGY, serializer);
		broker.register(task0, 0);

		broker.beforeSimStep(time);
		broker.syncTimestep(time, false);

		broker.beforeSimStep(aheadTime);
		broker.syncTimestep(aheadTime, false);

		var inOrder = inOrder(task0);
		inOrder.verify(task0).add(argThat(m ->
			m instanceof MessageA(String payload) && payload.equals(inSequenceMessage.payload()))
		);
		inOrder.verify(task0).add(argThat(m ->
			m instanceof MessageA(String payload) && payload.equals(aheadMsg.payload()))
		);
	}

	@Test
	void recvUnexpectedPastMessage() {

		// it is allowed that partitions which are not direct neighbors, send messages with a timestamp older than the current one.
		// this can happen when some partition, which is not a neighbor, sends a teleported agent. Since the synchronization is done
		// with direct neighbors, partitions can divert in time with each partition in between them. This means it is not allowed for
		// neighbor partitions (syncFrom) to send out-of-sequence messages. Non-neighbor partitions may send such messages though.

		var time = 43;
		var pastTime = time - 5;
		var syncRank = 1;
		var nonSyncRank = 2;
		var unexpectedPastMessage = new MessageA("ahead message");
		var unexpectedPastBytes = msgBytes(nonSyncRank, 0, 0, unexpectedPastMessage, seq(pastTime));
		var inSequenceMessage = new MessageA("in sequence message");
		var inSequenceBytes = msgBytes(syncRank, 0, 0, inSequenceMessage, seq(time));

		var comm = mockComm();
		doAnswer(i -> {
			MessageReceiver expectsNext = i.getArgument(0);
			MessageConsumer msgConsumer = i.getArgument(1);

			// first message is unexpected but is processed
			assertTrue(expectsNext.expectsMoreMessages());
			msgConsumer.consume(unexpectedPastBytes);

			// second message is processed and the expected ranks are cleared.
			assertTrue(expectsNext.expectsMoreMessages());
			msgConsumer.consume(inSequenceBytes);
			assertFalse(expectsNext.expectsMoreMessages());

			return null;
		}).when(comm).recv(any(), any());

		var task0 = mock(LPTask.class);
		when(task0.getSupportedMessages()).thenReturn(IntSet.of(serializer.getType(MessageA.class)));
		when(task0.waitForOtherParts(anyDouble())).thenReturn(IntSet.of(2));

		var broker = new MessageBroker(comm, TOPOLOGY, serializer);
		broker.register(task0, 0);

		broker.beforeSimStep(time);
		broker.syncTimestep(time, false);

		var inOrder = inOrder(task0);
		inOrder.verify(task0).add(argThat(m ->
			m instanceof MessageA(String payload) && payload.equals(unexpectedPastMessage.payload()))
		);
		inOrder.verify(task0).add(argThat(m ->
			m instanceof MessageA(String payload) && payload.equals(inSequenceMessage.payload()))
		);
	}

	@Test
	void crashOnExpectedPastMessage() {

		// Maybe find a better name? Neighbor partitions, or those the MessageBroker is supposed to sync with may not send
		// messages with a timestep smaller than the current one. Ranks, that sync are expected to proceed in lock step.

		var time = 43;
		var pastTime = time - 5;
		var syncRank = 1;
		var pastMessageFromSyncedRank = new MessageA("illegal past message");
		var pastBytesFromSyncRank = msgBytes(syncRank, 0, 0, pastMessageFromSyncedRank, seq(pastTime));

		var comm = mockComm();
		doAnswer(i -> {
			MessageReceiver expectsNext = i.getArgument(0);
			MessageConsumer msgConsumer = i.getArgument(1);

			// first message is unexpected but is processed
			assertTrue(expectsNext.expectsMoreMessages());
			assertThrows(IllegalStateException.class, () -> msgConsumer.consume(pastBytesFromSyncRank));

			return null;
		}).when(comm).recv(any(), any());

		var broker = new MessageBroker(comm, TOPOLOGY, serializer);
		broker.beforeSimStep(time);
		broker.syncFromRank(syncRank);
		broker.syncTimestep(time, false);

		// verify that the do answer has been invoked!
		verify(comm, times(1)).recv(any(), any());
	}

	@SuppressWarnings("unchecked")
	private <T extends Message> T verifyMsg(int expectedTag, int expectedReceiver, int expectedPartition, int expectedType, ByteBuffer actualBytes) {
		actualBytes.order(ByteOrder.LITTLE_ENDIAN);
		var memBuf = MemoryBuffer.fromByteBuffer(actualBytes);

		// verify rank header
		assertEquals(expectedTag, memBuf.readInt32());
		assertEquals(0, memBuf.readInt32());
		assertEquals(expectedReceiver, memBuf.readInt32());

		// verify part header
		assertEquals(expectedPartition, memBuf.readInt32());
		assertEquals(expectedType, memBuf.readInt32());
		memBuf.readInt32(); // we don't care about the message size

		return (T) serializer.deserialize(memBuf, expectedType);
	}

	private ByteBuffer msgBytes(int sender, int receiver, int partition, Message msg, int seq) {
		var headerBytes = MessageBroker.headerFor(seq, sender, receiver);
		var msgBytes = MessageBroker.serialize(partition, msg, serializer);

		var buf = ByteBuffer.allocate(headerBytes.capacity() + msgBytes.capacity()).order(ByteOrder.LITTLE_ENDIAN);
		headerBytes.flip();
		buf.put(headerBytes);
		msgBytes.flip();
		buf.put(msgBytes);
		buf.flip();
		return buf;
	}

	/**
	 * Technical debt: duplicates the sequence number formula from {@link MessageBroker#beforeSimStep(double)}.
	 * If that formula changes, this must be updated too.
	 */
	private static int seq(double time) {
		return Math.abs(1000 + (int) (time * 100));
	}

	public record MessageA(String payload) implements Message {}

	public record MessageB(String payload) implements Message {}
}
