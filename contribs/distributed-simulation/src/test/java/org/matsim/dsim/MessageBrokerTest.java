package org.matsim.dsim;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.apache.fory.memory.MemoryBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

	private static final SerializationProvider serializer = new SerializationProvider();

	private MessageBroker broker;
	private Topology topology;

	@BeforeEach
	void setUp() {
		Communicator communicator = mock(Communicator.class);
		when(communicator.getRank()).thenReturn(0);
		when(communicator.getSize()).thenReturn(3);

		var node0 = ComputeNode.builder().distributed(true).rank(0).parts(IntList.of(0, 1)).cores(1).hostname("localhost").build();
		var node1 = ComputeNode.builder().distributed(true).rank(1).parts(IntList.of(2, 3)).cores(1).hostname("localhost").build();
		var node2 = ComputeNode.builder().distributed(true).rank(2).parts(IntList.of(4, 5)).cores(1).hostname("localhost").build();

		topology = Topology.builder()
			.totalPartitions(6)
			.computeNodes(List.of(node0, node1, node2))
			.build();

		this.broker = new MessageBroker(communicator, topology);
	}

	@Test
	void waitForRank() throws IOException {

		// register rank to wait for
		var waitForRank = 1;
		var time = 0;
		broker.syncFromRank(waitForRank);
		assertTrue(broker.expectsMoreMessages());

		// clear the rank by simulating message receive
		broker.beforeSimStep(time);
		var msg = MessageBroker.headerFor(1000, waitForRank, broker.getRank());
		msg.flip();
		broker.consume(msg);
		assertFalse(broker.expectsMoreMessages());
	}

	@Test
	void dontWaitForOwnRank() {
		broker.syncFromRank(broker.getRank());
		assertFalse(broker.expectsMoreMessages());
	}

	@Test
	void sendEmptyMessageToOtherRank() throws Exception {

		var otherRank = 1;
		try (var comm = mock(Communicator.class)) {
			var broker = new MessageBroker(comm, topology);
			broker.beforeSimStep(0);
			broker.syncToRank(otherRank);
			broker.syncTimestep(0, false);
			verify(comm, times(1)).send(eq(otherRank), any(), anyLong(), anyLong());
		}
	}

	@Test
	void sendOneEmptyMessageToRemotePart() throws Exception {
		var otherRank = 1;
		try (var comm = mock(Communicator.class)) {
			var broker = new MessageBroker(comm, topology);
			broker.beforeSimStep(0);
			// add both partitions of the 'otherRank'
			var parts = topology.getNodeByIndex(otherRank).getParts();
			broker.syncToPart(parts.getFirst());
			broker.syncToPart(parts.getInt(1));
			broker.syncTimestep(0, false);

			// the message broker should only send one message to the 'otherRank'
			verify(comm, times(1)).send(eq(otherRank), any(), anyLong(), anyLong());
		}
	}

	@Test
	void sendNoEmptyMessageToOwnRank() throws Exception {
		var otherRank = 1;
		try (var comm = mock(Communicator.class)) {
			var broker = new MessageBroker(comm, topology);
			broker.beforeSimStep(0);
			// add both partitions of the 'otherRank'
			broker.syncToPart(topology.getNodeByIndex(broker.getRank()).getParts().getFirst());
			broker.syncToRank(broker.getRank());
			broker.syncTimestep(0, false);

			// the message broker should only send one message to the 'otherRank'
			verify(comm, never()).send(eq(otherRank), any(), anyLong(), anyLong());
		}
	}

	@Test
	void sendReceiveEmtpyMessages() throws Exception {

		var otherRank = 1;
		var receivedBytes = msgBytes(otherRank, broker.getRank(), 0, EmptyMessage.INSTANCE, seq(0));
		receivedBytes.flip();

		try (var comm = mock(Communicator.class)) {
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

			var broker = new MessageBroker(comm, topology);
			var receiverTask = mock(LPTask.class);
			when(receiverTask.getSupportedMessages()).thenReturn(IntSet.of(serializer.getType(MessageA.class)));
			var waitParts = new IntArraySet(topology.getNodeByIndex(otherRank).getParts());
			when(receiverTask.waitForOtherParts(anyDouble())).thenReturn(waitParts);
			broker.register(receiverTask, 0);
			broker.beforeSimStep(0);
			broker.syncToRank(otherRank);

			broker.syncTimestep(0, false);

			// we expect one emtpy message to be sent
			verify(comm, times(1)).send(eq(otherRank), any(), anyLong(), anyLong());
			// we expect the broker to call recv once. This will trigger the 'doAnswer' on top.
			verify(comm, times(1)).recv(any(), any());
			// since we are only receiving empty messages, it should never be dispatched to a task
			verify(receiverTask, never()).add(any());
		}
	}

	@Test
	void sendMultipleRanks() throws Exception {

		try (var comm = mock(Communicator.class)) {

			var broker = new MessageBroker(comm, topology);
			var receiverTask = mock(LPTask.class);
			when(receiverTask.getSupportedMessages()).thenReturn(IntSet.of(serializer.getType(MessageA.class)));
			// we want to wait for/ sync from both other ranks
			var waitParts = IntSet.of(2, 3, 4, 5);
			when(receiverTask.waitForOtherParts(anyDouble())).thenReturn(waitParts);
			broker.register(receiverTask, 0);
			broker.beforeSimStep(0);
			// we want to synt to both other ranks
			broker.syncToRank(1);
			broker.syncToRank(2);

			// we register one message to rank 1
			var msgToPart2 = new MessageA("message to part 2 on rank 1");
			broker.send(msgToPart2, 2);

			broker.syncTimestep(0, false);

			var msgCaptor = ArgumentCaptor.forClass(MemorySegment.class);
			var rankCaptor = ArgumentCaptor.forClass(Integer.class);
			verify(comm, times(2)).send(
				rankCaptor.capture(), msgCaptor.capture(), anyLong(), anyLong());

			// verify ranks
			var rankArgs = rankCaptor.getAllValues();
			assertEquals(2, rankArgs.size());
			assertTrue(rankArgs.contains(1));
			assertTrue(rankArgs.contains(2));
			var iRank1 = rankArgs.indexOf(1);
			var iank2 = rankArgs.indexOf(2);

			// verify messages
			var msgArgs = msgCaptor.getAllValues();
			assertEquals(2, msgArgs.size());

			var bytes1 = msgArgs.get(iRank1);
			var buf1 = bytes1.asByteBuffer();
			var msg1 = verifyMsg(seq(0), 0, 1, 2, msgToPart2.getType(), buf1);
			assertEquals(msgToPart2.payload(), ((MessageA) msg1).payload());

			var bytes2 = msgArgs.get(iank2);
			var buf2 = bytes2.asByteBuffer();
			var msg2 = verifyMsg(seq(0), 0, 2, 4, EmptyMessage.INSTANCE.getType(), buf2);
			assertInstanceOf(EmptyMessage.class, msg2); // nothin else to compare
		}
	}

	@Test
	void recvMultipleRanks() throws Exception {

		var msgFromRank1 = EmptyMessage.INSTANCE;
		var msgFromRank2 = new MessageA("message from rank 2 to part 1");
		var bytesRank1 = msgBytes(1, 0, 0, msgFromRank1, seq(0));
		bytesRank1.flip();
		var bytesRank2 = msgBytes(2, 0, 1, msgFromRank2, seq(0));
		bytesRank2.flip();
		try (var comm = mock(Communicator.class)) {
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

			var broker = new MessageBroker(comm, topology);
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

			// we expect two emtpy message to be sent
			verify(comm, times(2)).send(anyInt(), any(), anyLong(), anyLong());
			// we expect the broker to call recv once. This will trigger the 'doAnswer' on top.
			verify(comm, times(1)).recv(any(), any());

			// receiver task should have received one message
			var captor = ArgumentCaptor.forClass(Message.class);
			verify(receiverTask, times(1)).add(captor.capture());
			var receivedMesssage = captor.getValue();
			assertInstanceOf(MessageA.class, receivedMesssage);
			assertEquals(msgFromRank2.payload(), ((MessageA) receivedMesssage).payload());

			// non receiver task should have received no messages, as the empty message should not be dispatched to tasks
			verify(nonReceivingTask, never()).add(any());
		}
	}

	private Message verifyMsg(int expectedTag, int expectedSender, int expectedReceiver, int expectedPartition, int expectedType, ByteBuffer actualBytes) throws IOException {

		actualBytes.order(ByteOrder.LITTLE_ENDIAN);
		var memBuf = MemoryBuffer.fromByteBuffer(actualBytes);

		// verify rank header
		assertEquals(expectedTag, memBuf.readInt32());
		assertEquals(expectedSender, memBuf.readInt32());
		assertEquals(expectedReceiver, memBuf.readInt32());

		//verify part header
		assertEquals(expectedPartition, memBuf.readInt32());
		assertEquals(expectedType, memBuf.readInt32());
		memBuf.readInt32(); // we don't care about the message size

		// verify msg
		return serializer.getFuryParser(expectedType).parse(memBuf);
	}

	private ByteBuffer msgBytes(int sender, int receiver, int partition, Message msg, int seq) {

		var headerBytes = MessageBroker.headerFor(seq, sender, receiver);
		var msgBytes = MessageBroker.serialize(partition, msg, serializer);

		var buf = ByteBuffer.allocate(headerBytes.capacity() + msgBytes.capacity()).order(ByteOrder.LITTLE_ENDIAN);
		headerBytes.flip();
		buf.put(headerBytes);
		msgBytes.flip();
		buf.put(msgBytes);
		return buf;
	}

	private static int seq(double time) {
		return Math.abs(1000 + (int) (time * 100));
	}

	public record MessageA(String payload) implements Message {}
}
