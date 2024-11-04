package org.matsim.dsim;


import com.google.inject.Inject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.HdrHistogram.Histogram;
import org.agrona.BitUtil;
import org.apache.fury.ThreadSafeFury;
import org.apache.fury.memory.MemoryBuffer;
import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;
import org.matsim.api.LP;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.messages.Empty;
import org.matsim.api.core.v01.messages.SimulationNode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.communication.Communicator;
import org.matsim.core.communication.MessageConsumer;
import org.matsim.core.communication.MessageReceiver;
import org.matsim.core.serialization.FuryBufferParser;
import org.matsim.core.serialization.SerializationProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class responsible for routing messages to the correct recipient.
 */
@Log4j2
public final class MessageBroker implements MessageConsumer, MessageReceiver {

	private static final boolean CHECK_SEQ = Objects.equals(System.getenv("CHECK_SEQ"), "1");

	/**
	 * Communicator instance.
	 */
	private final Communicator comm;
	private final Topology topology;

	/**
	 * For all partitions, the outgoing rank is stored.
	 */
	private final int[] addresses;

	/**
	 * Outgoing data for the broadcast channel + all other nodes.
	 */
	private final MemorySegment[] outgoing;

	/**
	 * Length of the outgoing data.
	 */
	private final AtomicInteger[] dataSize;

	/**
	 * All local tasks.
	 */
	private final List<SimTask> tasks = new ArrayList<>();

	/**
	 * Task mapped by partition and supported event type.
	 */
	private final Long2ObjectMap<List<SimTask>> byAddress = new Long2ObjectOpenHashMap<>(1024);

	/**
	 * Tasks mapped by type.
	 */
	private final Int2ObjectMap<List<SimTask>> byType = new Int2ObjectOpenHashMap<>(1024);

	/**
	 * Set of partitions to wait for.
	 */
	private final IntSet waitFor = new IntOpenHashSet();

	/**
	 * Set of partitions for which a null message has to be sent this sync step. These partitions will wait for a message.
	 */
	private final IntSet sendNullMsgs = new IntOpenHashSet();

	/**
	 * Partitions on this node.
	 */
	private final IntSet ownParts;

	/**
	 * All other partitions on different nodes.
	 */
	private final IntSet otherParts;

	/**
	 * Serialization provider.
	 */
	private final SerializationProvider serialization = new SerializationProvider();

	/**
	 * Store events that have been received. These will be accessed and cleared by the {@link EventsManager}.
	 */
	private final List<Event> events = new ArrayList<>();

	/**
	 * Stores messages that arrived from partitions that are already ahead in the sequence.
	 */
	private final Queue<ByteBuffer> aheadMsgs = new ManyToOneConcurrentLinkedQueue<>();

	private final Histogram histogram = new Histogram(TimeUnit.SECONDS.toNanos(1), 3);

	/**
	 * Current sequence number, used as tag.
	 */
	private volatile int seq = 0;

	@Inject
	public MessageBroker(Communicator comm, Topology topology) {
		this.comm = comm;
		this.topology = topology;

		this.addresses = new int[topology.getTotalPartitions()];
		for (int i = 0; i < topology.getNodesCount(); i++) {
			SimulationNode n = topology.getNode(i);
			for (int p : n.getParts()) {
				addresses[p] = n.getRank();
			}
		}

		this.outgoing = new MemorySegment[topology.getNodesCount() + 1];
		this.dataSize = new AtomicInteger[topology.getNodesCount() + 1];
		for (int i = 0; i < outgoing.length; i++) {
			String bufferSize = System.getenv("MSG_BUFFER_SIZE");
			outgoing[i] = Arena.ofAuto().allocate(bufferSize != null ? Integer.parseInt(bufferSize) : 32 * 1024 * 1024,
				BitUtil.CACHE_LINE_LENGTH);
			dataSize[i] = new AtomicInteger(0);
		}

		this.ownParts = new IntOpenHashSet(topology.getNode(comm.getRank()).getParts());
		this.otherParts = new IntOpenHashSet();
		for (int i = 0; i < topology.getTotalPartitions(); i++) {
			if (!ownParts.contains(i)) {
				otherParts.add(broadcastAddress(i));
			}
		}
	}

	/**
	 * Internal id of a partition that used the broadcast to all.
	 */
	static int broadcastAddress(int partition) {
		return -(partition + 1);
	}

	static long address(int part, int type) {
		return (((long) part) << 32) | (type & 0xffffffffL);
	}

	static ByteBuffer clone(ByteBuffer original) {
		ByteBuffer clone = ByteBuffer.allocate(original.capacity());
		clone.put(original);
		clone.flip();
		return clone;
	}

	/**
	 * Return events. This list needs to be mutable, as events will be cleared.
	 */
	List<Event> getEvents() {
		return events;
	}

	/**
	 * Returns the message queue for unprompted messages.
	 */
	Queue<ByteBuffer> getMessageQueue() {
		return aheadMsgs;
	}

	void register(SimTask task, int part) {

		tasks.add(task);
		for (int type : task.getSupportedMessages()) {
			long address = address(part, type);
			byAddress.computeIfAbsent(address, k -> new ArrayList<>()).add(task);
			byType.computeIfAbsent(type, k -> new ArrayList<>()).add(task);
		}
	}

	/**
	 * Deregister a task from the broker.
	 */
	void deregister(SimTask task) {
		tasks.remove(task);

		byAddress.values().forEach(l -> l.remove(task));
		byType.values().forEach(l -> l.remove(task));
	}

	public int getRank() {
		return comm.getRank();
	}

	/**
	 * Queues a message for sending.
	 *
	 * @param msg               piece of information to send
	 * @param receiverPartition partition of the receiver, the broker will determine if the message needs to be sent remotely
	 */
	public void send(Message msg, int receiverPartition) {

		Objects.requireNonNull(msg, "Message cannot be null");

		if (receiverPartition != Communicator.BROADCAST_TO_ALL && addresses[receiverPartition] == comm.getRank()) {
			List<SimTask> list = byAddress.get(address(receiverPartition, msg.getType()));
			if (list == null)
				throw new IllegalStateException("No task registered for message %s (%d)".formatted(msg.getClass(), msg.getType()));

			list.forEach(t -> t.add(msg));
		} else {
			if (receiverPartition == Communicator.BROADCAST_TO_ALL) {
				sendLocal(msg, Communicator.BROADCAST_TO_ALL);

				if (comm.getSize() > 1)
					queueSend(msg, Communicator.BROADCAST_TO_ALL, receiverPartition);
			} else {
				int rank = addresses[receiverPartition];
				queueSend(msg, rank, receiverPartition);
			}
		}
	}

	/**
	 * Add a rank that should be waited for in the next synchronization step.
	 */
	public void addWaitForRank(int rank) {
		// Copy all partitions of the rank to wait list
		waitFor.addAll(topology.getNode(rank).getParts());
	}

	/**
	 * Register a rank to be sent a null message if no other messages are sent to it.
	 */
	public void addNullMessage(int partition) {
		// This is inefficient, but should be a small loop
		for (SimulationNode node : topology) {
			if (node.getParts().contains(partition)) {
				sendNullMsgs.add(node.getRank());
			}
		}
	}

	/**
	 * Put message into local queues of the tasks.
	 */
	private void sendLocal(Message msg, int receiverPartition) {

		// Empty messages are not sent anywhere
		if (msg instanceof Empty)
			return;

		List<SimTask> list;
		if (receiverPartition == Communicator.BROADCAST_TO_ALL) {
			list = byType.getOrDefault(msg.getType(), List.of());
		} else {
			list = byAddress.get(address(receiverPartition, msg.getType()));
		}
		//log.info("#{} received message to send local. {}", getRank(), msg.toDebugString());
		list.forEach(t -> t.add(msg));
	}

	void beforeSimStep(double time) {
		// Offset the sequence to avoid interference with other seq ids
		seq = 1000 + (int) (time * 100);
	}

	void syncTimestep(double time, boolean last) {
		if (comm.getSize() == 1) {
			return;
		}

		long t = System.nanoTime();

		for (SimTask task : tasks) {
			IntSet others = task.waitForOtherRanks(time);

			// On last iteration the lps are not executed
			if (last && task instanceof LPTask)
				continue;

			if (others == LP.ALL_NODES_BROADCAST) {
				waitFor.addAll(otherParts);
			} else
				waitFor.addAll(others);
		}

		// remove all that are on the same partition
		waitFor.removeAll(ownParts);

//        log.debug("Node {} waiting for partitions: {}", comm.getRank(), waitFor);

		for (int rank : sendNullMsgs) {
			int length = dataSize[rank + 1].get();
			if (length == 0) {
//                log.info("Node {} sending null message to {}", comm.getRank(), rank);
				send(Empty.INSTANCE, rank);
			}
		}

		sendNullMsgs.clear();
		sendRecvMessages();

		try {
			histogram.recordValue(System.nanoTime() - t);
		} catch (ArrayIndexOutOfBoundsException e) {
			// Ignore
		}
	}

	/**
	 * Serialize message and put it into the outgoing buffer. This method can be called concurrently.
	 *
	 * @param partition receiving partition
	 */
	@SneakyThrows
	private void queueSend(Message msg, int rank, int partition) {

		AtomicInteger oldPos = dataSize[rank + 1];

		ThreadSafeFury fury = serialization.getFury();
		MemoryBuffer buf = MemoryBuffer.newHeapBuffer(1024);

		while (true) {

			int pos = oldPos.get();

			// Reset buffer
			buf.writerIndex(0);

			// Add sender information at the beginning
			if (pos == 0) {
				// sequence number /  tag
				buf.writeInt32(seq);
				// sender rank
				buf.writeInt32(comm.getRank());
				// receiver rank
				buf.writeInt32(rank);
			}

			// message type
			buf.writeInt32(partition);
			buf.writeInt32(msg.getType());

			// Put message size after writing
			int sizeIdx = buf.writerIndex();
			buf.writeInt32(0);

			fury.serializeJavaObject(buf, msg);
			// Serialized size
			buf.putInt32(sizeIdx, buf.writerIndex() - sizeIdx - Integer.BYTES);

			int length = buf.writerIndex();

			if (pos + length > outgoing[rank + 1].byteSize()) {
				throw new IllegalStateException("Outgoing buffer is full. Increase buffer size or reduce message size.");
			}

			// Update length position of the buffer
			if (!oldPos.compareAndSet(pos, pos + length)) {
				continue;
			}

			ByteBuffer buffer = outgoing[rank + 1].asByteBuffer();
			buf.copyTo(0, MemoryBuffer.fromByteBuffer(buffer), pos, buf.writerIndex());

			break;
		}
	}

	private void sendRecvMessages() {

		for (int i = 0; i < outgoing.length; i++) {
			int length = dataSize[i].get();
			if (length > 0) {
				int receiver = i - 1;
//                log.debug("Rank #{}, seq{}: Send message to {}", comm.getRank(), seq, receiver);
				comm.send(receiver, outgoing[i], 0, length);
				dataSize[i].set(0);
			}
		}

		int size = aheadMsgs.size();
		int i = 0;
		// Process already received messages
		// Need to check at most the size of the queue
		ByteBuffer data;
		while ((data = aheadMsgs.poll()) != null && i++ < size) {
			try {
				consume(data);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		comm.recv(this, this);
	}

	@Override
	public boolean expectsMoreMessages() {
		if (seq >= 8281000) {
			var time = seq / 100 - 1000;
			log.debug("#" + this.getRank() + ":" + time + " expecting " + waitFor.size() + " more messages");
		}
		return !waitFor.isEmpty();
	}

	@Override
	public void consume(ByteBuffer data) throws IOException {

		int length = data.limit();

		MemoryBuffer in = MemoryBuffer.fromByteBuffer(data);
		int tag = in.readInt32();
		int sender = in.readInt32();
		int receiver = in.readInt32();

//        log.debug("Rank #{}, seq{}: Received message from {} to {}", comm.getRank(), tag, sender, receiver);

		// Ignore messages that are not for this rank
		// This might happen depending on the underlying communicator
		if (receiver != comm.getRank() && receiver != Communicator.BROADCAST_TO_ALL)
			return;

		if (tag < seq && CHECK_SEQ) {
			String error = "Out of order received sequence current seq: %d, received seq: %d".formatted(seq, tag);
			log.error(error);
			log.error("Sender node: {}, Receiver node: {}", sender, receiver);
			log.error("Node {} contains partitions: {}", comm.getRank(), ownParts);
			log.error("Node {} currently supposed to wait for partitions: {}", comm.getRank(), waitFor);
			log.error("Message covered partitions: {}", topology.getNode(sender).getParts());
			log.error("Message contents:");

			while (in.readerIndex() < length) {
				int partition = in.readInt32();
				int type = in.readInt32();
				int size = in.readInt32();

				FuryBufferParser parser = serialization.getFuryParser(type);
				Message msg = parser.parse(in);
				log.error("#Partition {}: {}", partition, msg.toString());
			}
			log.error("End of message contents");

			throw new IllegalStateException(error);
		}

		if (tag > seq) {
			aheadMsgs.add(clone(data));
			return;
		}

		while (in.readerIndex() < length) {
			int partition = in.readInt32();
			int type = in.readInt32();
			int size = in.readInt32();

			FuryBufferParser parser = serialization.getFuryParser(type);
			Message msg = parser.parse(in);

			if (msg instanceof Event m) {
				events.add(m);
			} else {
				//log.info("#{} received message: {}", getRank(), msg.toDebugString());
				sendLocal(msg, partition);
			}
		}

		// The communication between two nodes may be split up into two separate messages.
		// Need to differentiate if a message iy received as broadcast to all or as directed message
		if (receiver == Communicator.BROADCAST_TO_ALL) {
			topology.getNode(sender).getParts().forEach(p -> waitFor.remove(broadcastAddress(p)));
		} else
			topology.getNode(sender).getParts().forEach(waitFor::remove);

//        log.debug("Rank #{}, t{}: Waiting for message from partitions: {}", comm.getRank(), tag, waitFor);
	}

	public Histogram getRuntime() {
		return histogram;
	}
}
