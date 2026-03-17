package org.matsim.dsim;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.apache.fory.memory.MemoryBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.LP;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.MessageProcessor;
import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.core.communication.Communicator;
import org.matsim.core.communication.MessageConsumer;
import org.matsim.core.communication.MessageReceiver;
import org.matsim.core.serialization.SerializationProvider;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MessageBrokerTest {

	// Topology: rank 0 → partitions [0,1], rank 1 → partitions [2,3], rank 2 → partitions [4,5]
	private static final int SEQ_AT_T0 = 1000;

	private static final SerializationProvider serializer = new SerializationProvider();

	private MessageBroker broker;
	private TestCommunicator communicator;
	private Topology topology;

	@BeforeEach
	void setUp() {
		this.communicator = new TestCommunicator(3, 0);

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
		var msg = headerOf(waitForRank, broker.getRank(), time);
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
	void sendReceiveNeighborTasks() throws Exception {

		var otherRank = 1;
		try (var comm = mock(Communicator.class)) {
			var broker = new MessageBroker(comm, topology);
			var lp = new StubLP(topology.getNodeByIndex(otherRank).getParts());
			var receiverTask = lpTask(lp, 0);
			broker.register(receiverTask, 0);
			broker.beforeSimStep(0);
			broker.syncToRank(otherRank);

			assertFalse(broker.expectsMoreMessages());
			broker.syncTimestep(0, false);
			assertTrue(broker.expectsMoreMessages());
			verify(comm, times(1)).send(eq(otherRank), any(), anyLong(), anyLong());

			var msg = msgBytes(otherRank, broker.getRank(), 0, new MessageA("test message"), seq(0));
			msg.flip();
			broker.consume(msg);
			assertFalse(broker.expectsMoreMessages());

			receiverTask.beforeExecution();
			receiverTask.run();
			assertEquals(1, lp.received.size());
			assertEquals("test message", lp.received.getFirst().payload());
		}
	}

	@Test
	void neighborPartitionAddsOwningRankToWaitFor() {
		// Partition 2 is owned by rank 1; the broker should wait for rank 1
		broker.register(lpTask(IntSet.of(2)), 0);

		broker.beforeSimStep(0);
		broker.syncTimestep(0, false);

		assertTrue(broker.expectsMoreMessages());
	}

	@Test
	void ownPartitionIsFilteredFromWaitFor() {
		// Partition 0 is on rank 0 (this node); should not wait for itself
		broker.register(lpTask(IntSet.of(0)), 0);

		broker.beforeSimStep(0);
		broker.syncTimestep(0, false);

		assertFalse(broker.expectsMoreMessages());
	}

	@Test
	void multiplePartitionsOnSameNodeCollapseToOneWait() throws IOException {
		// Partitions 2 and 3 are both on rank 1; a single message from rank 1 should clear the wait
		broker.register(lpTask(IntSet.of(2, 3)), 0);

		broker.beforeSimStep(0);
		broker.syncTimestep(0, false);
		assertTrue(broker.expectsMoreMessages());

		broker.consume(headerOf(1, 0, 0));
		assertFalse(broker.expectsMoreMessages());
	}

	@Test
	void consumeFromNeighborRankClearsWait() throws IOException {
		broker.register(lpTask(IntSet.of(2)), 0);

		broker.beforeSimStep(0);
		broker.syncTimestep(0, false);
		assertTrue(broker.expectsMoreMessages());

		broker.consume(headerOf(1, 0, 0));
		assertFalse(broker.expectsMoreMessages());
	}

	@Test
	void consumeFromUnrelatedRankDoesNotClearWait() throws IOException {
		// Waiting for rank 1 (partition 2), receiving from rank 2 should not clear it
		broker.register(lpTask(IntSet.of(2)), 0);

		broker.beforeSimStep(0);
		broker.syncTimestep(0, false);

		broker.consume(headerOf(2, 0, 0));
		assertTrue(broker.expectsMoreMessages());
	}

	@Test
	void syncToPartSendsHeartbeatToOwningRank() {
		// syncToPart(2) → partition 2 is on rank 1 → rank 1 should receive a heartbeat
		broker.syncToPart(2);

		broker.beforeSimStep(0);
		broker.syncTimestep(0, false);

		assertTrue(communicator.sentRanks.contains(1), "Expected a heartbeat sent to rank 1");
	}

	@Test
	void syncToPartOnOwnPartitionIsIgnored() {
		// Partition 0 is owned by rank 0 (ourselves); no heartbeat should be sent
		broker.syncToPart(0);

		broker.beforeSimStep(0);
		broker.syncTimestep(0, false);

		assertFalse(communicator.sentRanks.contains(0), "Should not send heartbeat to own rank");
	}

	// --- helpers ---

	private LPTask lpTask(LP lp, int partition) {
		return new LPTask(lp, partition, mock(DistributedEventsManager.class), serializer);
	}

	private LPTask lpTask(IntCollection neighborParts) {
		return new LPTask(new StubLP(neighborParts), 0, null, serializer);
	}

	/**
	 * Craft a minimal valid ByteBuffer representing a message header with no payload.
	 * The broker clears the sender rank from waitForRanks after reading the header.
	 */
	private ByteBuffer headerOf(int sender, int receiver, double time) {
		// mimic the sequence of the broker.
		var seq = Math.abs(1000 + (int) (time * 100));
		// MemoryBuffer uses native (little-endian on x86) byte order
		ByteBuffer buf = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
		buf.putInt(seq); // tag must match current seq
		buf.putInt(sender);
		buf.putInt(receiver);
		buf.flip();
		return buf;
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

	private ByteBuffer bytesOf(int sender, int receiver, int partition, Message msg, double time) {
		var buf = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
		var segment = MemoryBuffer.fromByteBuffer(buf);
		var seq = Math.abs(1000 + (int) (time * 100));
		segment.writeInt32(seq);
		segment.writeInt32(sender);
		segment.writeInt32(receiver);
		segment.writeInt32(partition);
		segment.writeInt32(msg.getType());

		var sizeIdx = buf.position();
		buf.putInt(0);

		serializer.getFory().serializeJavaObject(segment, msg);
		segment.putInt32(sizeIdx, buf.position() - sizeIdx - Integer.BYTES);

		return segment.sliceAsByteBuffer();
	}

	// --- stub LP ---

	private static class StubLP implements LP, MessageAProcessor {
		private final IntSet neighborParts;
		final List<MessageA> received = new ArrayList<>();

		StubLP(IntCollection neighborParts) {
			this.neighborParts = new IntOpenHashSet(neighborParts);
		}

		@Override
		public IntSet waitForOtherParts(double time) {
			return neighborParts;
		}

		@Override
		public void process(MessageA msg) {
			received.add(msg);
		}
	}

	private static int seq(double time) {
		return Math.abs(1000 + (int) (time * 100));
	}

	// --- test communicator ---

	private static class TestCommunicator implements Communicator {

		private final int size;
		private final int rank;
		final List<Integer> sentRanks = new ArrayList<>();

		private TestCommunicator(int size, int rank) {
			this.size = size;
			this.rank = rank;
		}

		@Override
		public int getRank() {
			return rank;
		}

		@Override
		public int getSize() {
			return size;
		}

		@Override
		public void send(int receiver, MemorySegment data, long offset, long length) {
			sentRanks.add(receiver);
		}

		@Override
		public void recv(MessageReceiver expectsNext, MessageConsumer handleReceive) {


		}
	}

	public record MessageA(String payload) implements Message {}

	public interface MessageAProcessor extends MessageProcessor {
		void process(MessageA msg);
	}
}
