package org.matsim.core.communication;

import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.YieldingIdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Communicates using shared memory.
 */
public class SharedMemoryCommunicator implements Communicator {

	private static final Logger log = LogManager.getLogger(SharedMemoryCommunicator.class);

	private final int rank;
	private final int size;
	private final Path tmpDir;

	private final IdleStrategy idle = new YieldingIdleStrategy();

	private final IPC subscription;
	private final IPC[] others;

	public SharedMemoryCommunicator(int rank, int size, Path tmpDir) {
		this.rank = rank;
		this.size = size;
		this.tmpDir = tmpDir;

		var qFilePath = getQFilePath(rank);
		if (Files.exists(qFilePath)) {
			throw new RuntimeException("Shared memory file already exists: " + qFilePath + ". This indicates that a previous run was not properly" +
				" finished. Clean up the directory before starting a simulation to ensure proper message exchange!");
		}
		log.info("Serving on {}", qFilePath);

		try {
			Files.createDirectories(tmpDir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		this.subscription = new IPC(qFilePath, size, true);
		this.others = new IPC[size];
	}

	static void main() {
		var globalTmpDir = System.getProperty("java.io.tmpdir");
		try (SharedMemoryCommunicator comm = new SharedMemoryCommunicator(0, 1, Path.of(globalTmpDir, "dsim-shared-mem-comm"))) {
			comm.connect();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Path getQFilePath(int rank) {
		return tmpDir.resolve("q-" + rank);
	}

	public void connect() {

		for (int i = 0; i < size; i++) {
			if (i != rank) {

				var name = getQFilePath(i);
				while (!Files.exists(name))
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
			int read = subscription.rb.read((_, buffer, index, length) -> {
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
			}, 1);

			if (read == 0) {
				idle.idle();
			}
		}

		idle.reset();
	}

	private static final class IPC {

		final ManyToOneRingBuffer rb;
		private final FileChannel channel;
		private final Path path;

		/**
		 * Compute the next power of two for the given value.
		 */
		private long nextPowerOfTwo(long value) {
			return 1L << (64 - Long.numberOfLeadingZeros(value - 1));
		}

		public IPC(Path path, long total, boolean clear) {

			this.path = path;
			try {
				channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			String bufferSize = System.getenv("MSG_BUFFER_SIZE");
			int s = bufferSize != null ? Integer.parseInt(bufferSize) : 32 * 1024 * 1024;
			long n = nextPowerOfTwo(total * s + RingBufferDescriptor.TRAILER_LENGTH);
			long size = n * s + RingBufferDescriptor.TRAILER_LENGTH;
			while (size > Integer.MAX_VALUE) {
				n = n >> 1;
				size = n * s + RingBufferDescriptor.TRAILER_LENGTH;
			}

			// Create a memory-mapped file
			MappedByteBuffer buffer = mapBuffer(size);

			// Zero the buffer before using it
			if (clear) {
				while (buffer.hasRemaining()) {
					buffer.put((byte) 0);
				}
				buffer.clear();
			}

			UnsafeBuffer ub = new UnsafeBuffer(buffer);
			rb = new ManyToOneRingBuffer(ub);
		}

		private MappedByteBuffer mapBuffer(long size) {
			try {
				return channel.map(FileChannel.MapMode.READ_WRITE, 0, size);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void close(boolean delete) {

			try {
				channel.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			if (delete) {
				delete();
			}
		}

		private void delete() {
			try {
				Files.delete(path);
			} catch (IOException e) {
				log.warn("Could not delete file {}", path);
			}
		}
	}

}
