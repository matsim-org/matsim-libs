package org.matsim.dsim.executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BusyThreadpoolTest {

	private BusyThreadpool pool;

	@BeforeEach
	void setUp() {
		pool = new BusyThreadpool(4, 128);
	}

	@AfterEach
	void tearDown() {
		if (pool != null && !pool.isShutdown()) {
			pool.shutdown();
		}
	}

	@Test
	@Timeout(value = 5, unit = TimeUnit.SECONDS)
	void testSubmitSingleTask() throws InterruptedException, ExecutionException {
		pool.resume();
		AtomicBoolean executed = new AtomicBoolean(false);
		Future<?> future = pool.submit(() -> executed.set(true));
		future.get();
		assertThat(executed.get()).isTrue();
	}

	@Test
	@Timeout(value = 5, unit = TimeUnit.SECONDS)
	void testSubmitMultipleTasks() throws InterruptedException, ExecutionException {
		pool.resume();
		int numTasks = 100;
		AtomicInteger count = new AtomicInteger(0);
		List<Future<?>> futures = new ArrayList<>();
		for (int i = 0; i < numTasks; i++) {
			futures.add(pool.submit(count::incrementAndGet));
		}
		for (Future<?> future : futures) {
			future.get();
		}
		assertThat(count.get()).isEqualTo(numTasks);
	}

	@Test
	@Timeout(value = 5, unit = TimeUnit.SECONDS)
	void testSubmitAll() throws InterruptedException, ExecutionException {
		pool.resume();
		int numTasks = 50;
		AtomicInteger count = new AtomicInteger(0);
		List<Runnable> tasks = new ArrayList<>();
		for (int i = 0; i < numTasks; i++) {
			tasks.add(count::incrementAndGet);
		}
		Future<?> future = pool.submitAll(tasks);
		future.get();
		assertThat(count.get()).isEqualTo(numTasks);
	}

	@Test
	@Timeout(value = 5, unit = TimeUnit.SECONDS)
	void testShutdown() throws ExecutionException, InterruptedException {
		pool.resume();
		AtomicInteger count = new AtomicInteger(0);
		int expectedCount = 1000;

		// Submit a lot of slow work
		var executions = new ArrayList<Future<?>>();
		for (int i = 0; i < expectedCount; i++) {
			var future = pool.submit(() -> {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				count.incrementAndGet();
			});
			executions.add(future);
		}

		pool.shutdown();
		assertThat(pool.isShutdown()).isTrue();
		assertThatThrownBy(() -> pool.submit(() -> {
		}))
			.isInstanceOf(RejectedExecutionException.class);

		for (var f : executions) {
			f.get();
		}
		// all futures should still complete eventually
		assertEquals(expectedCount, count.get());
	}

	@Test
	@Timeout(value = 5, unit = TimeUnit.SECONDS)
	void testPause() throws ExecutionException, InterruptedException {
		pool.resume();
		AtomicInteger count = new AtomicInteger(0);
		int expectedCount = 1000;

		// Submit a lot of slow work
		var executions = new ArrayList<Future<?>>();
		for (int i = 0; i < expectedCount; i++) {
			var future = pool.submit(() -> {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				count.incrementAndGet();
			});
			executions.add(future);
		}

		pool.pause();
		assertTrue(pool.isPaused());
		assertThatThrownBy(() -> pool.submit(() -> {
		}))
			.isInstanceOf(RejectedExecutionException.class);

		for (var f : executions) {
			f.get();
		}
		// all futures should still complete eventually
		assertEquals(expectedCount, count.get());
	}

	@Test
	@Timeout(value = 5, unit = TimeUnit.SECONDS)
	void testTaskException() {
		pool.resume();
		RuntimeException exception = new RuntimeException("Test exception");
		Future<?> future = pool.submit(() -> {
			throw exception;
		});

		assertThatThrownBy(future::get)
			.isInstanceOf(ExecutionException.class)
			.hasCause(exception);
	}

	@Test
	@Timeout(value = 5, unit = TimeUnit.SECONDS)
	void testSubmitAllWithException() {
		pool.resume();
		RuntimeException exception = new RuntimeException("Test exception");
		List<Runnable> tasks = List.of(
			() -> {
			},
			() -> {
				throw exception;
			},
			() -> {
			}
		);

		Future<?> future = pool.submitAll(tasks);

		assertThatThrownBy(future::get)
			.isInstanceOf(ExecutionException.class)
			.hasCause(exception);
	}

	@Test
	@Timeout(value = 10, unit = TimeUnit.SECONDS)
	void testWorkStealing() throws InterruptedException, ExecutionException {
		// To test work stealing, we can saturate one worker's queue and see if others pick it up.
		// However, enqueue() distributes tasks among workers randomly/round-robin.
		// BusyThreadpool.enqueue uses submitIndex to pick a starting worker.

		pool.resume();
		AtomicInteger count = new AtomicInteger(0);

		// We use a large number of tasks to increase chance of work stealing if some workers are slower.
		// But since they all do the same thing, let's try to block some workers.

		CountDownLatch latch = new CountDownLatch(1);

		// Submit a task that blocks
		pool.submit(() -> {
			try {
				latch.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});

		// Now submit many quick tasks.
		// Even if the worker we submitted the blocking task to is busy,
		// other workers should pick up tasks from its queue or be assigned new tasks.

		int quickTasks = 100;
		List<Future<?>> futures = new ArrayList<>();
		for (int i = 0; i < quickTasks; i++) {
			futures.add(pool.submit(count::incrementAndGet));
		}

		// Wait for quick tasks to complete. They should finish even with one worker blocked.
		for (Future<?> f : futures) {
			f.get();
		}

		assertThat(count.get()).isEqualTo(quickTasks);

		latch.countDown();
	}
}
