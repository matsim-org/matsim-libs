package org.matsim.core.communication;

import org.matsim.api.core.v01.Message;
import org.matsim.core.serialization.SerializationProvider;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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

			ByteBuffer bb = data.asByteBuffer();
			// tag, sender, receiver
			bb.putInt(0, tag);
			bb.putInt(Integer.BYTES, getRank());
			bb.putInt(Integer.BYTES * 2, BROADCAST_TO_ALL);
			bb.put(Integer.BYTES * 3, bytes);

			send(BROADCAST_TO_ALL, data, 0, data.byteSize());

			recv(() -> messages.size() < getSize(), (buf) -> {

				int t = buf.getInt();
				if (tag != t)
					throw new IllegalStateException("Unexpected tag, got: %d, expected: %d".formatted(t, tag));

				buf.getInt(); // sender
				buf.getInt(); // receiver

				messages.add(provider.parse(buf));
			});

			return messages;
		}
	}

	default void close() throws Exception {
		// do nothing
	}

}

