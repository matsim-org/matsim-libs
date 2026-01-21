package org.matsim.core.communication;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.matsim.api.core.v01.Message;
import org.matsim.core.serialization.SerializationProvider;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interface for the communicator that is used to send and receive messages between processes.
 */
public interface Communicator extends AutoCloseable {

	/**
	 * Constant used as a receiver to broadcast messages to all other processes.
	 */
	int BROADCAST_TO_ALL = -1;

	/**
	 * Parse node list given in the slurm format.
	 */
	static List<String> parseNodeList(String address, String values) {

		Pattern p = Pattern.compile("([a-z]+)\\[([0-9,-]+)]", Pattern.CASE_INSENSITIVE);

		Matcher m = p.matcher(values);
		List<String> nodes = new ArrayList<>();

		if (m.find()) {
			String[] split = m.group(2).split(",");

			for (String s : split) {
				if (s.contains("-")) {
					String[] range = s.split("-");
					int start = Integer.parseInt(range[0]);
					int end = Integer.parseInt(range[1]);

					for (int i = start; i <= end; i++) {
						nodes.add(m.group(1) + i);
					}
				} else {
					nodes.add(m.group(1) + s);
				}
			}
		}

		if (nodes.isEmpty()) {
			nodes.addAll(Arrays.asList(values.split(",")));
		}

		nodes.removeIf(s -> s.equals(address));
		return nodes;
	}

	/**
	 * Initializes and connects the communicator.
	 */
	default void connect() throws Exception {
	}

	/**
	 * @return the rank of the communicator in the communication context
	 */
	int getRank();

	/**
	 * @return the number of communicators in this communication context
	 */
	int getSize();

	/**
	 * Send the data in the memory segment to the specified receiver.
	 * The data is sent from the specified offset and has the specified length.
	 */
	void send(int receiver, MemorySegment data, long offset, long length);


	/**
	 * Receive messages from other processes. This method may also ensure that async write operations are performed.
	 */
	void recv(MessageReceiver expectsNext, MessageConsumer handleReceive);

	/**
	 * Takes the data and sends them away. It then blocks to receive
	 * envelope and passes envelope to the 'handleReceive' callback. Based on the result
	 * of the callback, the communicator awaits more messages or finishes the communication
	 *
	 * @param envelope      envelope sent to other processes
	 * @param expectsNext   tells the communicator whether it should wait for another message
	 * @param handleReceive callback to process received envelope. Result of the callback
	 *                      indicates, whether to expect more envelope.
	 */
	default void sendReceive(Collection<Envelope> envelope, MessageReceiver expectsNext, MessageConsumer handleReceive) {

		for (var env : envelope) {
			send(env.receiver(), env.data(), env.offset(), env.length());
		}

		recv(expectsNext, handleReceive);
	}

	default <T extends Message> void send(int toRank, T msg, SerializationProvider provider) {
		try (Arena arena = Arena.ofConfined()) {
			var msgBytes = provider.toBytes(msg);
			var msgData = arena.allocate(msgBytes.length + Integer.BYTES * 3);
			ByteBuffer bb = msgData.asByteBuffer().order(ByteOrder.LITTLE_ENDIAN);
			bb.putInt(msg.getType());
			bb.putInt(getRank());
			bb.putInt(toRank);
			bb.put(msgBytes);
			send(toRank, msgData, 0, msgData.byteSize());
		}
	}

	/**
	 * Sends the msg to all other processes and receives messages from all other processes.
	 *
	 * @param msg Message to be sent
	 * @param tag Tag of the message, only message with the same tag will be received
	 * @return All received messages, including the one sent
	 */
	default <T extends Message> List<T> allGather(T msg, int tag, SerializationProvider provider) {
		List<T> messages = new ArrayList<>(getSize());
		messages.add(msg);

		byte[] bytes = provider.toBytes(msg);
		try (Arena arena = Arena.ofConfined()) {

			MemorySegment data = arena.allocate(bytes.length + Integer.BYTES * 3);

			ByteBuffer bb = data.asByteBuffer().order(ByteOrder.LITTLE_ENDIAN);
			// tag, sender, receiver
			bb.putInt(0, tag);
			bb.putInt(Integer.BYTES, getRank());
			bb.putInt(Integer.BYTES * 2, BROADCAST_TO_ALL);
			bb.put(Integer.BYTES * 3, bytes);

			send(BROADCAST_TO_ALL, data, 0, data.byteSize());

			recv(() -> messages.size() < getSize(), (buf) -> {

				buf.order(ByteOrder.LITTLE_ENDIAN);
				int t = buf.getInt();
				if (tag != t)
					throw new IllegalStateException("#%d Unexpected tag, got: %d, expected: %d".formatted(getRank(), t, tag));

				buf.getInt(); // sender
				buf.getInt(); // receiver

				messages.add(provider.parse(buf));
			});

			return messages;
		}
	}

	/**
	 * Similar to MPI all-to-all. Sends messages to all ranks included in msgsToRanks and then awaits messages from the same ranks. It is possible to
	 * include a message to itself. However, this message is going to be ignored.
	 * <p>
	 * This can be used as a barrier, when all other ransk are included in the msgsToRanks parameter.
	 * <p>
	 * This method only checks whether it has received messages from the ranks included in msgsToRanks. If other ranks send messages, it is not guaranteed
	 * that they are processed.
	 *
	 * @param msgsToRanks mapping of ranks->msg. The method waits for messages from all ranks in the keys-set
	 * @return Received messages from all other ranks.
	 */
	default <T extends Message> List<T> allToAll(Map<Integer, T> msgsToRanks, SerializationProvider provider) {

		if (getSize() == 1) return List.of();

		for (var entry : msgsToRanks.entrySet()) {
			var targetRank = entry.getKey();
			if (targetRank != getRank()) {
				send(entry.getKey(), entry.getValue(), provider);
			}
		}

		// wait for data to arrive
		var expectedMsgs = getSize() - 1;
		var expectedSenders = msgsToRanks.keySet();
		var result = new ArrayList<T>(expectedMsgs);
		// we are not expecting empty parameter
		var msgType = msgsToRanks.values().iterator().next().getType();
		recv(() -> !expectedSenders.isEmpty(), (buf) -> {
			buf.order(ByteOrder.LITTLE_ENDIAN);
			int t = buf.getInt();
			if (msgType != t)
				throw new IllegalStateException("Unexpected tag, got: %d, expected: %d".formatted(t, msgType));

			var sender = buf.getInt(); // sender
			buf.getInt(); // receiver
			result.add(provider.parse(buf));
			expectedSenders.remove(sender);
		});
		return result;
	}

	/**
	 * NOTE: This method only works in combination with {@link #gatherFromAll}!
	 * <p>
	 * Similar to MPI_Gather, where all ranks send data to one rank. We split this implementation into two methods. This method is called by the ranks
	 * sending data to another rank. For example, if rank 1 and 2 should send data to rank 0, rank 1 and 2, call this method with toRank=0. Rank 0
	 * should instead use {@link Communicator#gatherFromAll}.
	 * <p>
	 * This is a blocking communication and functions as a barrier. All ranks may only proceed once the `toRank` has received all messages.
	 *
	 * @param toRank   Rank of the compute node, which receives data
	 * @param msg      Message of type T to be sent
	 * @param provider Serialization provider to serialize the message
	 */
	default <T extends Message> void gatherTo(int toRank, T msg, SerializationProvider provider) {

		// send the message
		send(toRank, msg, provider);

		// wait for acknowlegement
		var isAcknowledged = new AtomicBoolean(false);
		var ackType = provider.getType(AckMsg.class);
		recv(() -> !isAcknowledged.get(), (buf) -> {
			buf.order(ByteOrder.LITTLE_ENDIAN);
			int t = buf.getInt();
			if (ackType != t)
				throw new IllegalStateException("Unexpected tag, got: %d, expected: %d".formatted(t, msg.getType()));
			isAcknowledged.set(true);
		});
	}

	/**
	 * NOTE: This method only works in combination with {@link #gatherTo}!
	 * <p>
	 * Similar to MPI_Gather, where all ranks send data to one rank. We split this implementation into two methods. This method is called by the rank
	 * which receives data from the other ranks. For example, if rank 1 and 2 should send data to rank 0, rank 0, calls this method with. Rank 1 and 2
	 * should instead use {@link Communicator#gatherTo}.
	 * <p>
	 * This is a blocking communication and functions as a barrier. All ranks may only proceed once the `toRank` has received all messages.
	 *
	 * @param msgClass Class of expected messages.
	 * @param provider Serialization provider to serialize the message
	 */
	default <T extends Message> List<T> gatherFromAll(Class<T> msgClass, SerializationProvider provider) {
		// wait for msgs
		var msgType = provider.getType(msgClass);
		var expectedNumberOfMsgs = getSize() - 1;
		var messages = new ArrayList<T>(expectedNumberOfMsgs);
		var senders = new IntArrayList(expectedNumberOfMsgs);
		recv(() -> senders.size() < expectedNumberOfMsgs, (buf) -> {
			buf.order(ByteOrder.LITTLE_ENDIAN);
			int t = buf.getInt();
			if (msgType != t)
				throw new IllegalStateException("Unexpected tag, got: %d, expected: %d".formatted(t, msgType));

			var sender = buf.getInt(); // sender
			buf.getInt(); // receiver

			messages.add(provider.parse(buf));
			senders.add(sender);
		});

		// after we have received all messages, we send acknowledgements to the senders, so that they can leave the barrier.
		send(BROADCAST_TO_ALL, new AckMsg(), provider);
		return messages;
	}

	default void close() throws Exception {
		// do nothing
	}

	/**
	 * Message type used to acknowledge that all expected messages have been received. Used internally by {@link Communicator#gatherFromAll}.
	 */
	record AckMsg() implements Message {
	}
}

