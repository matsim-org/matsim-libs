package org.matsim.dsim;


import com.google.inject.Inject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.HdrHistogram.Histogram;
import org.agrona.BitUtil;
import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;
import org.apache.fory.memory.MemoryBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.LP;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.api.core.v01.messages.EmptyMessage;
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
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Class responsible for routing messages to the correct recipient.
 */
public final class MessageBroker implements MessageConsumer, MessageReceiver {

	/**
	 * This class has a few 'trace' statements, which can be enabled by passing -Dlog4j2.level=TRACE to the VM
	 */
	private static final Logger log = LogManager.getLogger(MessageBroker.class);

	private static final boolean CHECK_SEQ = Objects.equals(System.getenv("CHECK_SEQ"), "1");

	/**
	 * Indicates that a message is sent to the node inbox.
	 */
	private static final int NODE_MESSAGE = Integer.MIN_VALUE;

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
	 * Set of node ranks to wait for.
	 */
	private final IntSet waitForRanks = new IntOpenHashSet();

	/**
	 * Set of partitions for which a null message has to be sent this sync step. These partitions will wait for a message.
	 */
	private final IntSet sendToRanks = new IntOpenHashSet();

	/**
	 * Partitions on this node.
	 */
	private final IntSet ownParts;

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

	/**
	 * Stores messages sent to this node, mapped by event type.
	 */
	private final Int2ObjectMap<Queue<Message>> nodesMessages = new Int2ObjectOpenHashMap<>();

	private final Histogram histogram = new Histogram(TimeUnit.SECONDS.toNanos(1), 3);

	private final Histogram sizes = new Histogram(Integer.MAX_VALUE, 0);

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
			ComputeNode n = topology.getNodeByIndex(i);
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

		this.ownParts = new IntOpenHashSet(topology.getNodeByIndex(comm.getRank()).getParts());
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

	void register(SimTask task, int part) {

		tasks.add(task);
		for (int type : task.getSupportedMessages()) {
			long address = address(part, type);
			byAddress.computeIfAbsent(address, _ -> new ArrayList<>()).add(task);
			byType.computeIfAbsent(type, _ -> new ArrayList<>()).add(task);
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
	 * Send a message to a specific node. These messages are eventually delivery during sync steps.
	 * There are no delivery guarantees, when these messages will arrive.
	 * Such messages need to be polled using {@link #receiveNodeMessages(Class, Consumer)}.
	 */
	public void sendToNode(Message msg, int receiverNode) {

		if (receiverNode == Communicator.BROADCAST_TO_ALL) {

			for (int i = 0; i < comm.getSize(); i++) {
				if (i == comm.getRank())
					nodesMessages.computeIfAbsent(msg.getType(), _ -> new ManyToOneConcurrentLinkedQueue<>()).add(msg);
				else {
					queueSend(msg, i, NODE_MESSAGE);
				}
			}

		} else if (receiverNode == comm.getRank()) {
			nodesMessages.computeIfAbsent(msg.getType(), _ -> new ManyToOneConcurrentLinkedQueue<>()).add(msg);
		} else {
			queueSend(msg, receiverNode, NODE_MESSAGE);
		}
	}

	/**
	 * Receive all messages that have been sent to this node via {@link #sendToNode(Message, int)}.
	 *
	 * @param type     message type
	 * @param consumer method to consume the messages
	 */
	@SuppressWarnings("unchecked")
	public <T extends Message> void receiveNodeMessages(Class<T> type, Consumer<T> consumer) {

		int t = serialization.getType(type);
		Queue<Message> messages = nodesMessages.get(t);
		if (messages != null) {
			Message msg;
			while ((msg = messages.poll()) != null) {
				consumer.accept((T) msg);
			}
		}
	}

	/**
	 * Registers a rank which the message broker should synchronize with during the next sync.
	 * <p>
	 * This works in combination with {@link #syncToRank(int)}. This message broker will await messages from all registered ranks during
	 * the next sync. The message broker blocks until it has received a message from all registered ranks.
	 */
	public void syncFromRank(int rank) {

		if (getRank() != rank) {
			waitForRanks.add(rank);
		}
	}

	/**
	 * Register a partition which the message broker should synchronize with during the next sync.
	 * <p>
	 * The message broker determines the rank of the supplied partition and behaves like {@link #syncToRank(int)}.
	 */
	public void syncToPart(int partition) {

		// add the mapped rank if the partition is on another compute node
		var rank = addresses[partition];
		syncToRank(rank);
	}

	/**
	 * Register a rank which the message broker should synchronize with during the next sync.
	 * <p>
	 * The message broker will send a message to the supplied rank during the next sync in any case, signaling the other rank, that this rank has
	 * progressed to the next timestep. To actually synchronize, the other rank must wait for a message from this rank. This can be achieved by adding
	 * a rank via {@link #syncFromRank(int)}.
	 * <p>
	 * Internally, the message broker will either send a message which was queued by the simulation or an {@link EmptyMessage}.
	 */
	public void syncToRank(int rank) {

		// no need to wait for ourselves
		if (getRank() != rank) {
			sendToRanks.add(rank);
		}
	}

	/**
	 * Put message into local queues of the tasks.
	 */
	private void sendLocal(Message msg, int receiverPartition) {

		// Empty messages are not sent anywhere
		if (msg instanceof EmptyMessage)
			return;

		List<SimTask> list;
		if (receiverPartition == Communicator.BROADCAST_TO_ALL) {
			list = byType.getOrDefault(msg.getType(), List.of());
		} else {
			list = byAddress.get(address(receiverPartition, msg.getType()));
		}
		list.forEach(t -> t.add(msg));
	}

	void beforeSimStep(double time) {
		// Offset the sequence to avoid interference with other seq ids
		// to trigger final sync, we pass Infinity as time. To avoid overflow and a negative sequence value,
		// get the absolute value.
		seq = Math.abs(1000 + (int) (time * 100));
	}

	void syncTimestep(double time, boolean last) {

		// TODO remove
		this.currentTime = time;

		if (comm.getSize() == 1) {
			return;
		}

		long t = System.nanoTime();

		for (SimTask task : tasks) {
			IntSet otherParts = task.waitForOtherParts(time);

			// On last iteration the lps are not executed
			if (last && task instanceof LPTask)
				continue;

			// event manager also determines when to wait for ranks,
			// this might be not necesarry for event handler here
			if (otherParts == LP.ALL_PARTS_BROADCAST) {
				for (int i = 0; i < topology.getNodesCount(); i++) {
					syncFromRank(i);
				}
			} else {
				for (int partition : otherParts) {
					var rank = addresses[partition];
					syncFromRank(rank);
				}
			}
		}

		waitForRanks.remove(comm.getRank());

//		for (int rank : nullMessages) {
//			// No need to send a heartbeat to ourselves; waitFor already removes own partitions.
//			if (rank == comm.getRank()) continue;
//			int length = dataSize[rank + 1].get();
//			if (length == 0) {
//				// sendNullMsgs stores node ranks; send() expects a partition index.
//				// Use the first partition of that node as the routing address.
//				var parts = topology.getNodeByIndex(rank).getParts();
//				if (!parts.isEmpty()) {
//					send(EmptyMessage.INSTANCE, parts.getInt(0));
//				}
//			}
//		}
//
//		nullMessages.clear();
		sendRecvMessages();

		try {
			histogram.recordValue(System.nanoTime() - t);
		} catch (ArrayIndexOutOfBoundsException e) {
			// Ignore
		}
	}

	void afterSim() {
		waitForRanks.clear();
		nodesMessages.clear();
		histogram.reset();
		sizes.reset();
	}

	/**
	 * Serialize message and put it into the outgoing buffer. This method can be called concurrently.
	 *
	 * @param toPartition receiving partition, if {@link Integer#MIN_VALUE} the message is send to the node inbox.
	 */
	private void queueSend(Message message, int toRank, int toPartition) {

		log.trace("#{} queue send to {}/{}: {}", this.getRank(), toRank, toPartition, message);

		writeHeader(toRank);
		writeMessage(toRank, toPartition, message);
	}

	/**
	 * Writes header bytes to the outBuffer of 'toRank'. The header is only written if no header is present yet.
	 */
	private void writeHeader(int toRank) {
		// we only want to write a header if the outBuffer for 'toRank' is empty
		var outBufPosition = dataSize[toRank + 1];
		if (outBufPosition.get() == 0) {
			// we allocate the bytes once, and then we do a cas loop in case another thread also tries to add a header.
			// This is done by atomically updating the buffer position to after the header. If we succeed, the next
			// thread will write its bytes after the header. We can safely operate on the buffer between 0 and header.position
			// in the meantime.
			var headerBuffer = headerFor(seq, getRank(), toRank);
			while (true) {
				if (outBufPosition.compareAndSet(0, headerBuffer.limit())) {
					var outBuffer = outgoing[toRank + 1].asByteBuffer();
					headerBuffer.flip();
					outBuffer.put(headerBuffer);
					break;
				}
			}
		}
	}

	/**
	 * Writes message bytes to the outBuffer of 'toRank'. {@link #writeHeader(int)} must have been called before. To ensure that the message
	 * can be read on the receiver side.
	 */
	private void writeMessage(int toRank, int toPartition, Message message) {

		// allocate the message bytes once
		var messageBuf = serialize(toPartition, message, this.serialization);
		var messageBufSize = messageBuf.limit();
		var outBufPosition = dataSize[toRank + 1];

		while (true) {
			var currentOutBufPosition = outBufPosition.get();
			var finishedOutBufPosition = currentOutBufPosition + messageBufSize;

			if (currentOutBufPosition + messageBufSize > outgoing[toRank + 1].byteSize()) {
				throw new IllegalStateException("Outgoing buffer is full. Increase buffer size or reduce message size.");
			}

			// reserve a part in the out buffer by atomically setting the data size property for this buffer to the position of the
			// buffer that points 'behind' the data we are about to write.
			// if another thread has updated the dataSize in the meantime, we'll do another round in the while loop.
			if (outBufPosition.compareAndSet(currentOutBufPosition, finishedOutBufPosition)) {
				var outBuffer = outgoing[toRank + 1].asByteBuffer();
				messageBuf.flip();
				outBuffer.put(currentOutBufPosition, messageBuf, 0, messageBufSize);
				break;
			}
		}
	}

	private void sendRecvMessages() {

		// add null messages if that is necessary.
		queueNullMessages();

		for (int i = 0; i < outgoing.length; i++) {
			int length = dataSize[i].get();
			if (length > 0) {
				int receiver = i - 1;
				sizes.recordValue(length);
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

	private void queueNullMessages() {
		for (int rank : sendToRanks) {
			// No need to send a heartbeat to ourselves; waitFor already removes own partitions.
			if (rank == comm.getRank()) continue;
			int length = dataSize[rank + 1].get();
			if (length == 0) {

				// we want to send a null/sync message to the compute node. We don't care which partition receives it on the other side.
				var parts = topology.getNodeByIndex(rank).getParts();
				var anyPartition = parts.getInt(0);
				if (!parts.isEmpty()) {
					queueSend(EmptyMessage.INSTANCE, rank, anyPartition);
				}
			}
		}
		sendToRanks.clear();
	}

	private double currentTime = -1;

	@Override
	public boolean expectsMoreMessages() {
		if (!waitForRanks.isEmpty()) {
			log.trace(this::traceWaiting);
		}
		return !waitForRanks.isEmpty();
	}

	private String traceWaiting() {
		var waitForParts = waitForRanks.intStream().mapToObj(String::valueOf).collect(Collectors.joining(","));
		return "#" + getRank() + " at t:" + currentTime + " waiting for: [" + waitForParts + "]";
	}

	@Override
	public void consume(ByteBuffer data) throws IOException {

		// we have written little endian, hence read little endian.
		int length = data.limit();

		MemoryBuffer in = MemoryBuffer.fromByteBuffer(data);
		int tag = in.readInt32();
		int sender = in.readInt32();
		int receiver = in.readInt32();

		// Ignore messages that are not for this rank
		// This might happen depending on the underlying communicator
		if (receiver != comm.getRank() && receiver != Communicator.BROADCAST_TO_ALL)
			return;

		if (tag < seq && CHECK_SEQ) {
			String error = "Out of order received sequence current seq: %d, received seq: %d".formatted(seq, tag);
			log.error(error);
			log.error("Sender node: {}, Receiver node: {}", sender, receiver);
			log.error("Node {} contains partitions: {}", comm.getRank(), ownParts);
			log.error("Node {} currently supposed to wait for partitions: {}", comm.getRank(), waitForRanks);
			log.error("Message covered partitions: {}", topology.getNodeByIndex(sender).getParts());
			log.error("Message contents:");

			while (in.readerIndex() < length) {
				int partition = in.readInt32();
				int type = in.readInt32();
				int _ = in.readInt32();

				FuryBufferParser parser = serialization.getFuryParser(type);
				Message msg = parser.parse(in);
				log.error("#Partition {}: {}", partition, msg.toString());
			}
			log.error("End of message contents");

			throw new IllegalStateException(error);
		}

		if (tag > seq || tag < 0) {
			log.trace("#{} on seq {} received ahead msg from #{} for seq {}", comm.getRank(), seq, sender, tag);
			aheadMsgs.add(clone(data));
			return;
		}

		while (in.readerIndex() < length) {
			int partition = in.readInt32();
			int type = in.readInt32();
			int _ = in.readInt32();

			FuryBufferParser parser = serialization.getFuryParser(type);
			Message msg = parser.parse(in);
			log.trace("#{} received from {}: {}", receiver, sender, msg);

			if (partition == NODE_MESSAGE) {
				nodesMessages.computeIfAbsent(type, _ -> new ManyToOneConcurrentLinkedQueue<>()).add(msg);
				continue;
			}

			if (msg instanceof Event m) {
				events.add(m);
			} else {
				sendLocal(msg, partition);
			}
		}

		waitForRanks.remove(sender);
	}

	public Histogram getRuntime() {
		return histogram;
	}

	public Histogram getSizes() {
		return sizes;
	}

	static ByteBuffer headerFor(int seq, int sRank, int rRank) {

		var buf = ByteBuffer.allocate(3 * Integer.BYTES);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		// header data: sequence (depends on timestep, this rank, target rank)
		buf.putInt(seq);
		buf.putInt(sRank);
		buf.putInt(rRank);

		return buf;
	}

	static ByteBuffer serialize(int toPartition, Message message, SerializationProvider serializer) {

		// important to use serializeJavaObject, so that we can parse with deserializeFromJavaObject on receive.
		var msgBytes = serializer.getFory().serializeJavaObject(message);
		var msgSize = msgBytes.length;
		ByteBuffer buf = ByteBuffer.allocate(msgSize + 3 * Integer.BYTES);
		buf.order(ByteOrder.LITTLE_ENDIAN);

		// header data: toPartition, msgType, msgSize
		buf.putInt(toPartition);
		buf.putInt(message.getType());
		buf.putInt(msgSize);

		// msg data
		buf.put(msgBytes);

		return buf;
	}
}
