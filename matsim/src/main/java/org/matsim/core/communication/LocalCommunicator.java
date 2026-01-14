package org.matsim.core.communication;

import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Communicator that works exclusively in the same process. It's main use is for testing purposes.
 */
public class LocalCommunicator implements Communicator {

	/**
	 * This class has a few 'trace' statements, which can be enabled by passing -Dlog4j2.level=TRACE to the VM
	 */
	private static final Logger log = LogManager.getLogger(LocalCommunicator.class);

	private final int rank;
	private final int size;

	private final IdleStrategy idle = new BackoffIdleStrategy();

	/**
	 * The incoming queues of all other ranks.
	 */
	private final List<ManyToOneConcurrentLinkedQueue<ByteBuffer>> queues;

	public LocalCommunicator(int rank, int size, List<ManyToOneConcurrentLinkedQueue<ByteBuffer>> queues) {
		this.rank = rank;
		this.size = size;
		this.queues = queues;
	}

	/**
	 * Create a set of communicators that can exchange messages with each other.
	 */
	public static List<Communicator> create(int size) {

		List<Communicator> communicators = new ArrayList<>(size);
		List<ManyToOneConcurrentLinkedQueue<ByteBuffer>> queues = new ArrayList<>(size);

		// Create a queue for each communicator
		for (int i = 0; i < size; i++) {
			queues.add(new ManyToOneConcurrentLinkedQueue<>());
		}

		for (int i = 0; i < size; i++) {
			communicators.add(new LocalCommunicator(i, size, queues));
		}

		return communicators;
	}

	static ByteBuffer clone(MemorySegment data, long offset, long length) {
		ByteBuffer clone = ByteBuffer.allocate(Math.toIntExact(length));
		clone.put(data.asByteBuffer().position(Math.toIntExact(offset)).limit(Math.toIntExact(offset + length)));
		clone.flip();
		return clone;
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

		ByteBuffer buf = clone(data, offset, length);
		// Every receiver needs a copy of the buffer
		if (receiver == Communicator.BROADCAST_TO_ALL) {
			log.trace(() -> traceMsg(rank, "broadcast", data.asByteBuffer()));
			for (int i = 0; i < size; i++) {
				if (i != rank) {
					queues.get(i).offer(buf.duplicate());
				}
			}
		} else {
			log.trace(() -> traceMsg(rank, "send", data.asByteBuffer()));
			queues.get(receiver).offer(buf.duplicate());
		}
	}

	@Override
	public void recv(MessageReceiver expectsNext, MessageConsumer handleReceive) {

		ManyToOneConcurrentLinkedQueue<ByteBuffer> self = queues.get(rank);
		while (expectsNext.expectsMoreMessages()) {

			ByteBuffer poll = self.poll();
			if (poll == null) {
				idle.idle();
				continue;
			}
			log.trace(() -> traceMsg(rank, "recv", poll));
			try {
				handleReceive.consume(poll);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			idle.reset();
		}
	}

	private static String traceMsg(int selfRank, String method, ByteBuffer mem) {
		// make a defensive copy, and reset the needle in the buffer.
		var buf = mem.duplicate().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().rewind();
		if (buf.limit() >= 3) {
			var seq = buf.get();
			var senderRank = buf.get();
			var receiverRank = buf.get();
			return "#" + selfRank + " " + method + ": { seq: " + seq + ", sRank: " + senderRank + ", rRank: " + receiverRank + "}";
		} else {
			var b = new StringBuilder("#" + selfRank + " " + method + ": [");
			while (buf.hasRemaining()) {
				b.append(buf.get());
			}
			b.append("]");
			return b.toString();
		}
	}
}
