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
import org.matsim.core.serialization.ForyBufferParser;
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

/**
 * Class responsible for routing messages to the correct recipient.
 */
public final class MessageBroker implements MessageConsumer, MessageReceiver {

	/**
	 * This class has a few 'trace' statements, which can be enabled by passing -Dlog4j2.level=TRACE to the VM
	 */
	private static final Logger log = LogManager.getLogger(MessageBroker.class);

	static final int ANY_PARTITION = -42;

	/**
	 * Indicates that a message is sent to the node inbox.
	 */
	private static final int NODE_MESSAGE = Integer.MIN_VALUE;

	/**
	 * Communicator instance.
	 */
	private final Communicator comm;
	private final Topology topology;
	private final SerializationProvider serialization;

	/**
	 * For all partitions, the outgoing rank is stored.
	 */
	private final int[] addresses;

	/**
	 * We store outgoing messages as plain bytes backed by a MemorySegment. This contains one buffer for each other rank, plus
	 * one buffer (0th) for broadcast messages.
	 */
	private final MessageBuffer[] outgoing;

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
	private final ConcurrentRankSet waitForRanks = new ConcurrentRankSet();

	/**
	 * Set of partitions for which a null message has to be sent this sync step. These partitions will wait for a message.
	 */
	private final ConcurrentRankSet sendToRanks = new ConcurrentRankSet();

	/**
	 * Partitions on this node.
	 */
	private final IntSet ownParts;

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
	public MessageBroker(Communicator comm, Topology topology, SerializationProvider serialization) {
		this.comm = comm;
		this.topology = topology;
		this.serialization = serialization;

		this.addresses = new int[topology.getTotalPartitions()];
		for (int i = 0; i < topology.getNodesCount(); i++) {
			ComputeNode n = topology.getNodeByIndex(i);
			for (int p : n.getParts()) {
				addresses[p] = n.getRank();
			}
		}

		// reserve some memory for each other rank and one for broadcast messages.
		this.outgoing = new MessageBuffer[topology.getNodesCount() + 1];
		for (int i = 0; i < outgoing.length; i++) {
			String bufferSize = System.getenv("MSG_BUFFER_SIZE");
			var segmentSize = bufferSize != null ? Integer.parseInt(bufferSize) : 32 * 1024 * 1024;
			var segment = Arena.ofAuto().allocate(segmentSize, BitUtil.CACHE_LINE_LENGTH);
			// sender is always our rank. Receiver is i -1, as we reserve the 0th slot for broadcasts.
			outgoing[i] = new MessageBuffer(getRank(), i - 1, serialization, segment);
		}

		this.ownParts = new IntOpenHashSet(topology.getNodeByIndex(comm.getRank()).getParts());
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

	void beforeSimStep(double time) {
		seq = seqFrom(time);
	}

	void syncTimestep(double now, boolean last) {

		if (comm.getSize() == 1) {
			return;
		}

		long t = System.nanoTime();

		determineWaitFor(now, last);
		queueNullMessages();
		sendOutBuffers();
		processAheadMessages();
		comm.recv(this, this);

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

	/**
	 * Serialize message and put it into the outgoing buffer. This method can be called concurrently.
	 *
	 * @param toPartition receiving partition, if {@link Integer#MIN_VALUE} the message is send to the node inbox.
	 */
	private void queueSend(Message message, int toRank, int toPartition) {

		log.trace("#{} at t:{} queue send to {}/{}: {}", this.getRank(), timeFrom(seq), toRank, toPartition, message);

		// 0th slot is for broadcasts
		var buffer = outgoing[toRank + 1];
		buffer.add(message, seq, toPartition);
	}

	private void determineWaitFor(double now, boolean last) {
		for (SimTask task : tasks) {
			IntSet otherParts = task.waitForOtherParts(now);

			// On last iteration the lps are not executed
			if (last && task instanceof LPTask)
				continue;

			// event manager also determines when to wait for ranks,
			// this might be not necesarry for event handler here
			if (otherParts == LP.ALL_PARTS_BROADCAST) {
				for (int i = 0; i < topology.getNodesCount(); i++) {
					syncFromRank(broadcastRank(i));
				}
			} else {
				for (int partition : otherParts) {
					var rank = addresses[partition];
					syncFromRank(rank);
				}
			}
		}

		// we don't want to wait for messages from ourselves
		waitForRanks.remove(getRank());
		waitForRanks.remove(broadcastRank(getRank()));
	}

	private void queueNullMessages() {
		sendToRanks.forEach(rank -> {
			// No need to send a heartbeat to ourselves; waitFor already removes own partitions.
			if (rank == comm.getRank()) return;

			// 0th slot is for broadcasts
			var buffer = outgoing[rank + 1];
			if (buffer.isEmpty()) {
				queueSend(EmptyMessage.INSTANCE, rank, ANY_PARTITION);
			}
		});
		sendToRanks.clear();
	}

	private void sendOutBuffers() {

		for (var buffer : outgoing) {
			if (buffer.isEmpty()) continue;

			sizes.recordValue(buffer.size());
			comm.send(buffer.toReceiver(), buffer.segment(), 0, buffer.size());
			buffer.clear();
		}
	}

	private void processAheadMessages() {
		int size = aheadMsgs.size();
		int i = 0;
		// Process already received messages
		// Need to check at most the size of the queue
		ByteBuffer data;
		while ((data = aheadMsgs.poll()) != null && i++ < size) {
			try {
				log.trace("#{} at t:{} polled ahead message", comm.getRank(), timeFrom(seq));
				consume(data);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Override
	public boolean expectsMoreMessages() {
		if (!waitForRanks.isEmpty()) {
			log.trace(this::traceWaiting);
		}
		return !waitForRanks.isEmpty();
	}

	private String traceWaiting() {
		var parts = new StringBuilder();
		waitForRanks.forEach(rank -> parts.append(rank).append(","));
		return "#" + getRank() + " at t:" + timeFrom(seq) + " waiting for: [" + parts + "]";
	}

	@Override
	public void consume(ByteBuffer data) throws IOException {

		int length = data.limit();

		MemoryBuffer in = MemoryBuffer.fromByteBuffer(data);
		int tag = in.readInt32();
		int sender = in.readInt32();
		int receiver = in.readInt32();

		// Ignore messages that are not for this rank
		// This might happen depending on the underlying communicator
		if (receiver != comm.getRank() && receiver != Communicator.BROADCAST_TO_ALL)
			return;

		// we don't want messages from the past from ranks we sync with.
		// we do allow messages from the past for ranks we don't sync with
		if (tag < seq && waitForRanks.contains(sender)) {
			String error = "#%d Out of order received sequence current seq: %d, received seq: %d".formatted(getRank(), seq, tag);
			log.error(error);
			log.error("#{} Sender node: {}, Receiver node: {}", getRank(), sender, receiver);
			log.error("#{} contains partitions: {}", getRank(), ownParts);
			log.error("#{} currently supposed to wait for partitions: {}", getRank(), waitForRanks);
			log.error("#{} Message covered partitions: {}", getRank(), topology.getNodeByIndex(sender).getParts());
			log.error("#{} Message contents:", getRank());

			while (in.readerIndex() < length) {
				int partition = in.readInt32();
				int type = in.readInt32();
				int _ = in.readInt32();

				ForyBufferParser parser = serialization.getForyParser(type);
				Message msg = parser.parse(in);
				log.error("#{}: {}", partition, msg.toString());
			}
			log.error("#{} End of message contents", getRank());

			throw new IllegalStateException(error);
		}

		// in any case, we allow messages from the future and store them for later.
		if (tag > seq || tag < 0) {
			log.trace("#{} on seq {} received ahead msg from #{} for seq {}", comm.getRank(), seq, sender, tag);
			aheadMsgs.add(clone(data));
			return;
		}

		// the standard case is that we have messages for the current time. Deserialize and dispatch them to message processors.
		while (in.readerIndex() < length) {
			int partition = in.readInt32();
			int type = in.readInt32();
			int _ = in.readInt32();

			ForyBufferParser parser = serialization.getForyParser(type);
			Message msg = parser.parse(in);
			log.trace("#{} at t:{} received from {}: {}", getRank(), timeFrom(seq), sender, msg);

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

		// remove the sender rank from the ranks we expect messages from.
		var rankForRemoval = receiver == Communicator.BROADCAST_TO_ALL ? broadcastRank(sender) : sender;
		waitForRanks.remove(rankForRemoval);
	}

	public Histogram getRuntime() {
		return histogram;
	}

	public Histogram getSizes() {
		return sizes;
	}

	static int seqFrom(double time) {
		// Offset the sequence to avoid interference with other seq ids
		// to trigger final sync, we pass Infinity as time. To avoid overflow and a negative sequence value,
		// get the absolute value.
		return Math.abs(1000 + (int) (time * 100));
	}

	static double timeFrom(int seq) {
		return (seq - 1000) / 100.0;
	}

	static long address(int part, int type) {
		return (((long) part) << 32) | (type & 0xffffffffL);
	}

	static int broadcastRank(int rank) {
		// shift rank by one, because broadcasts from 0 cannot be turned into a negative.
		return (rank + 1) * -1;
	}

	static ByteBuffer clone(ByteBuffer original) {
		ByteBuffer clone = ByteBuffer.allocate(original.capacity());
		clone.put(original);
		clone.flip();
		return clone;
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

	private static class MessageBuffer {

		// this is our rank
		private final int fromSender;
		// this is the rank the messages are supposed to go to.
		private final int toReceiver;
		private final SerializationProvider serialization;

		private final MemorySegment segment;
		private final AtomicInteger size = new AtomicInteger(0);

		private MessageBuffer(int fromSender, int toReceiver, SerializationProvider serialization, MemorySegment segment) {
			this.fromSender = fromSender;
			this.toReceiver = toReceiver;
			this.serialization = serialization;
			this.segment = segment;
		}

		boolean isEmpty() {
			return size.get() == 0;
		}

		void clear() {
			size.set(0);
		}

		int toReceiver() {
			return toReceiver;
		}

		int size() {
			return size.get();
		}

		MemorySegment segment() {
			return segment;
		}

		/**
		 * The outfacing api to add messages to this buffer.
		 */
		void add(Message message, int seq, int toPartition) {
			writeHeader(seq);
			writeMessage(toPartition, message);
		}

		/**
		 * Writes header bytes to the outBuffer of 'toRank'. The header is only written if no header is present yet.
		 * DON'T call it from outside the buffer. Use {@link #add(Message, int, int) instead.}
		 */
		private void writeHeader(int seq) {
			// we only want to write a header if the outBuffer for 'toRank' is empty
			if (size.get() != 0) return;

			// we allocate the bytes once, and then we do a cas loop in case another thread also tries to add a header.
			// This is done by atomically updating the buffer position to after the header. If we succeed, the next
			// thread will write its bytes after the header. We can safely operate on the buffer between 0 and header.position
			// in the meantime.
			var headerBuffer = headerFor(seq, fromSender, toReceiver);
			while (true) {
				if (size.compareAndSet(0, headerBuffer.limit())) {
					var outBuffer = segment.asByteBuffer();
					headerBuffer.flip();
					outBuffer.put(headerBuffer);
					break;
				}
			}
		}

		/**
		 * Writes message bytes to the memory segment of this buffer. {@link #writeHeader(int)} must have been called before.
		 * To ensure that the message can be read on the receiver side.
		 * DON'T call it from outside the buffer. Use {@link #add(Message, int, int)} instead.
		 */
		private void writeMessage(int toPartition, Message message) {
			var messageBuf = serialize(toPartition, message, serialization);
			var messageBufSize = messageBuf.limit();

			while (true) {
				var currentOutBufPosition = size.get();
				var finishedOutBufPosition = currentOutBufPosition + messageBufSize;

				if (finishedOutBufPosition > segment.byteSize()) {
					throw new IllegalStateException("Outgoing buffer is full. Increase buffer size or reduce message size.");
				}

				if (size.compareAndSet(currentOutBufPosition, finishedOutBufPosition)) {
					var outBuffer = segment.asByteBuffer();
					messageBuf.flip();
					outBuffer.put(currentOutBufPosition, messageBuf, 0, messageBufSize);
					return;
				}
			}
		}
	}

	/**
	 * We allow multiple processes to store which rank they want to sync from and to. We use this concurrent set for this purpose.
	 * It is backed by an AtomicLong, allowing lock-free updates. Since it is backed by a long and we have positive and negative ranks
	 * (for broadcasts), the message broker is limited to work with at most 32 ranks. If more is needed, an AtomicLongArray could be used
	 * instead, which would make the number of compute nodes added to one simulation run unbounded.
	 * <p>
	 * Mostly generated by GPT-5.4 mini, revised by @janekdererste.
	 */
	private static final class ConcurrentRankSet {

		private static final int VALUE_COUNT = Long.SIZE;
		private static final int OFFSET = VALUE_COUNT / 2; // 32
		private final java.util.concurrent.atomic.AtomicLong bits = new java.util.concurrent.atomic.AtomicLong(0L);

		void add(int value) {
			int bit = toBitIndex(value);
			long mask = 1L << bit;
			while (true) {
				long current = bits.get();
				long updated = current | mask;
				if (current == updated || bits.compareAndSet(current, updated)) {
					return;
				}
			}
		}

		boolean contains(int value) {
			int bit = toBitIndex(value);
			long mask = 1L << bit;
			return (bits.get() & mask) != 0L;
		}

		void remove(int value) {
			int bit = toBitIndex(value);
			long mask = ~(1L << bit);
			while (true) {
				long current = bits.get();
				long updated = current & mask;
				if (current == updated || bits.compareAndSet(current, updated)) {
					return;
				}
			}
		}

		void clear() {
			bits.set(0L);
		}

		@SuppressWarnings("BooleanMethodIsAlwaysInverted")
		boolean isEmpty() {
			return bits.get() == 0L;
		}

		void forEach(java.util.function.IntConsumer action) {
			long snapshot = bits.get();
			while (snapshot != 0L) {
				int bit = Long.numberOfTrailingZeros(snapshot);
				action.accept(bit - OFFSET);
				snapshot &= (snapshot - 1);
			}
		}

		private static int toBitIndex(int value) {
			int bit = value + OFFSET;
			if (bit < 0 || bit >= VALUE_COUNT) {
				throw new IllegalArgumentException(
					"ConcurrentRankSet supports values in range [-32, 31], but got " + value +
						". This implementation uses a single 64-bit bitset."
				);
			}
			return bit;
		}
	}
}
