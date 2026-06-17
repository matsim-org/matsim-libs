package org.matsim.dsim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.communication.SharedMemoryCommunicator;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SharedMemoryCommunicatorTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void createsAndDeletesSharedMemoryFile() throws Exception {
		Path tmpFolder = Path.of(utils.getOutputDirectory());

		try (var _ = new SharedMemoryCommunicator(0, 1, tmpFolder)) {
			assertThat(tmpFolder.resolve("q-0")).exists();
		}

		assertThat(tmpFolder.resolve("q-0")).doesNotExist();
	}

	@Test
	void throwsWhenFileAlreadyExists() throws IOException {
		Path tmpFolder = Path.of(utils.getOutputDirectory());
		Files.createDirectories(tmpFolder);
		Files.createFile(tmpFolder.resolve("q-0"));

		assertThatThrownBy(() -> {
			//noinspection EmptyTryBlock
			try (var _ = new SharedMemoryCommunicator(0, 1, tmpFolder)) {
				// nothing.
			}
		})
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("already exists");
	}

	@Test
	void communicatesBetweenTwoRanks() throws Exception {
		Path tmpFolder = Path.of(utils.getOutputDirectory());

		byte[] payload0 = "hello from rank 0".getBytes(StandardCharsets.UTF_8);
		byte[] payload1 = "hello from rank 1".getBytes(StandardCharsets.UTF_8);

		var received0 = new AtomicReference<byte[]>();
		var received1 = new AtomicReference<byte[]>();

		try (var comm0 = new SharedMemoryCommunicator(0, 2, tmpFolder);
			var comm1 = new SharedMemoryCommunicator(1, 2, tmpFolder);
			var exec = Executors.newVirtualThreadPerTaskExecutor()) {

			var f0 = exec.submit(() -> {
				comm0.connect();
				try (Arena arena = Arena.ofConfined()) {
					MemorySegment seg = arena.allocate(payload0.length);
					seg.asByteBuffer().put(payload0);
					comm0.send(1, seg, 0, payload0.length);
				}
				comm0.recv(() -> received0.get() == null, buf -> {
					byte[] bytes = new byte[buf.remaining()];
					buf.get(bytes);
					received0.set(bytes);
				});
			});

			var f1 = exec.submit(() -> {
				comm1.connect();
				try (Arena arena = Arena.ofConfined()) {
					MemorySegment seg = arena.allocate(payload1.length);
					seg.asByteBuffer().put(payload1);
					comm1.send(0, seg, 0, payload1.length);
				}
				comm1.recv(() -> received1.get() == null, buf -> {
					byte[] bytes = new byte[buf.remaining()];
					buf.get(bytes);
					received1.set(bytes);
				});
			});

			f0.get();
			f1.get();
		}

		assertThat(received0.get()).isEqualTo(payload1);
		assertThat(received1.get()).isEqualTo(payload0);
	}

	@Test
	void preservesMessageOrderWhenSentBeforeReading() throws Exception {
		Path tmpFolder = Path.of(utils.getOutputDirectory());

		byte[] msg1 = "message 1".getBytes(StandardCharsets.UTF_8);
		byte[] msg2 = "message 2".getBytes(StandardCharsets.UTF_8);

		var sendsDone = new CountDownLatch(1);
		var received = new ArrayList<String>();

		try (var comm0 = new SharedMemoryCommunicator(0, 2, tmpFolder);
			var comm1 = new SharedMemoryCommunicator(1, 2, tmpFolder);
			var exec = Executors.newVirtualThreadPerTaskExecutor()) {

			var f0 = exec.submit(() -> {
				comm0.connect();
				try (Arena arena = Arena.ofConfined()) {
					MemorySegment seg = arena.allocate(msg1.length);
					seg.asByteBuffer().put(msg1);
					comm0.send(1, seg, 0, msg1.length);
				}
				try (Arena arena = Arena.ofConfined()) {
					MemorySegment seg = arena.allocate(msg2.length);
					seg.asByteBuffer().put(msg2);
					comm0.send(1, seg, 0, msg2.length);
				}
				sendsDone.countDown();
			});

			var f1 = exec.submit(() -> {
				comm1.connect();
				try {
					sendsDone.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
				comm1.recv(() -> received.size() < 2, buf -> {
					byte[] bytes = new byte[buf.remaining()];
					buf.get(bytes);
					received.add(new String(bytes, StandardCharsets.UTF_8));
				});
			});

			f0.get();
			f1.get();
		}

		assertThat(received).containsExactly("message 1", "message 2");
	}

	@Test
	void noCorruptionWhenConstructedConcurrently() throws Exception {
		Path tmpFolder = Path.of(utils.getOutputDirectory());
		Files.createDirectories(tmpFolder);

		var barrier = new CyclicBarrier(2);
		var comm0Ref = new AtomicReference<SharedMemoryCommunicator>();
		var comm1Ref = new AtomicReference<SharedMemoryCommunicator>();

		try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
			var f0 = exec.submit(() -> {
				try {
					barrier.await();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				comm0Ref.set(new SharedMemoryCommunicator(0, 2, tmpFolder));
			});
			var f1 = exec.submit(() -> {
				try {
					barrier.await();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				comm1Ref.set(new SharedMemoryCommunicator(1, 2, tmpFolder));
			});
			f0.get();
			f1.get();
		}

		byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);
		var received = new AtomicReference<byte[]>();

		try (var comm0 = comm0Ref.get();
			var comm1 = comm1Ref.get();
			var exec = Executors.newVirtualThreadPerTaskExecutor()) {

			var f0 = exec.submit(() -> {
				comm0.connect();
				try (Arena arena = Arena.ofConfined()) {
					MemorySegment seg = arena.allocate(payload.length);
					seg.asByteBuffer().put(payload);
					comm0.send(1, seg, 0, payload.length);
				}
			});
			var f1 = exec.submit(() -> {
				comm1.connect();
				comm1.recv(() -> received.get() == null, buf -> {
					byte[] bytes = new byte[buf.remaining()];
					buf.get(bytes);
					received.set(bytes);
				});
			});

			f0.get();
			f1.get();
		}

		assertThat(received.get()).isEqualTo(payload);
	}
}
