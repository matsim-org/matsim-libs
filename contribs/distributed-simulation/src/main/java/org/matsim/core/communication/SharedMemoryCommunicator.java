package org.matsim.core.communication;

import io.aeron.CommonContext;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.YieldingIdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Communicates using shared memory.
 */
@Log4j2
public class SharedMemoryCommunicator implements Communicator {

	private final int rank;
	private final int size;

	private final IdleStrategy idle = new YieldingIdleStrategy();

	private final IPC subscription;
	private final IPC[] others;

	@SneakyThrows
	public SharedMemoryCommunicator(int rank, int size) {
		this.rank = rank;
		this.size = size;

		File name = getName(rank);
		log.info("Serving on {}", name);

		this.subscription = new IPC(name, size, true);
		this.others = new IPC[size];
	}

	public static void main(String[] args) {
		SharedMemoryCommunicator comm = new SharedMemoryCommunicator(0, 1);

		comm.connect();
	}

	private File getName(int rank) {
		return new File(CommonContext.getAeronDirectoryName() + "/q-" + rank);
	}

	@SneakyThrows
	public void connect() {

		for (int i = 0; i < size; i++) {
			if (i != rank) {

				File name = getName(i);
				while (!name.exists())
					idle.idle();

				others[i] = new IPC(name, size, false);
			}
		}
	}

	@Override
	public void close() throws Exception {

		subscription.close(true);
		for (int i = 0; i < size; i++) {
			if (i != rank) {
				others[i].close(false);
			}
		}
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

		if (receiver == rank) {
			throw new IllegalArgumentException("Cannot send to self");
		}

		UnsafeBuffer ub = new UnsafeBuffer(data.address() + offset, (int) length);

		if (receiver == -1) {
			for (int i = 0; i < size; i++) {
				if (i != rank) {
					sendInternal(others[i], ub, length);
				}
			}
		} else {
			sendInternal(others[receiver], ub, length);
		}
	}

	private void sendInternal(IPC receiver, UnsafeBuffer ub, long length) {

		while (true) {
			int idx = receiver.rb.tryClaim(1, (int) length);
			if (idx <= 0) {
				idle.idle();
				continue;
			}

			receiver.rb.buffer().putBytes(idx, ub, 0, (int) length);
			receiver.rb.commit(idx);
			break;
		}

		idle.reset();
	}

	@Override
	public void recv(MessageReceiver expectsNext, MessageConsumer handleReceive) {
		while (expectsNext.expectsMoreMessages()) {
			int read = subscription.rb.read((msgTypeId, buffer, index, length) -> {
				ByteBuffer bb = buffer.byteBuffer();
				if (bb == null)
					bb = ByteBuffer.wrap(buffer.byteArray(), index, length);
				else
					bb = bb.slice(index, length);

				try {
					handleReceive.consume(bb);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});

			if (read == 0) {
				idle.idle();
			}
		}

		idle.reset();
	}

	private static final class IPC {

		final ManyToOneRingBuffer rb;
		private final File path;
		private final RandomAccessFile file;
		private final FileChannel channel;
		private final MappedByteBuffer buffer;
		private final UnsafeBuffer ub;

		@SneakyThrows
		public IPC(File path, long total, boolean clear) {

			this.path = path;
			file = new RandomAccessFile(path, "rw");
			channel = file.getChannel();

			String bufferSize = System.getenv("MSG_BUFFER_SIZE");
			int s = bufferSize != null ? Integer.parseInt(bufferSize) : 32 * 1024 * 1024;

			// Create a memory-mapped file
			buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, total * s + RingBufferDescriptor.TRAILER_LENGTH);

			// Zero the buffer before using it
			if (clear) {
				while (buffer.hasRemaining()) {
					buffer.put((byte) 0);
				}
				buffer.clear();
			}

			ub = new UnsafeBuffer(buffer);
			rb = new ManyToOneRingBuffer(ub);
		}

		@SneakyThrows
		public void close(boolean delete) {
			channel.close();
			file.close();

			if (delete) {
				delete();
			}
		}

		private void delete() {
			boolean deleted = path.delete();
			if (!deleted) {
				log.warn("Could not delete file {}", path);
			}
		}
	}

}
