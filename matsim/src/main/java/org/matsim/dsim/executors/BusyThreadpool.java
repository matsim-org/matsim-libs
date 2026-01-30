package org.matsim.dsim.executors;

import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ManyToManyConcurrentArrayQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class BusyThreadpool implements ExecutorService {

	private static final Logger log = LogManager.getLogger(BusyThreadpool.class);

	private final Worker[] workers;
	private final AtomicInteger submitIndex = new AtomicInteger(0);
	private final IdleStrategy submitIdleStrategy = new BusySpinIdleStrategy();

	private volatile boolean shutdown;

	public BusyThreadpool(int size) {
		this(size, size * 128);
	}

	public BusyThreadpool(int size, int perWorkerQueueCapacity) {
		log.info("Initializing BusyThreadpool with {} workers; per-worker queue capacity={}", size, perWorkerQueueCapacity);

		this.workers = new Worker[size];
		for (int i = 0; i < size; i++) {
			workers[i] = new Worker("busy-threadpool-" + i, i, perWorkerQueueCapacity);
		}
		for (Worker w : workers) {
			w.start();
		}
	}

	private final class Worker implements Runnable {
		private final Thread thread;
		private final int workerId;
		private final IdleStrategy idleStrategy = new BusySpinIdleStrategy();

		/**
		 * Per-worker task queue.
		 * Owned by this worker, but other workers may "steal" via poll().
		 */
		private final ManyToManyConcurrentArrayQueue<Runnable> queue;

		private Worker(String name, int workerId, int queueCapacity) {
			this.thread = new Thread(this, name);
			this.workerId = workerId;
			this.thread.setDaemon(true); // optional
			this.queue = new ManyToManyConcurrentArrayQueue<>(queueCapacity);
		}

		void start() {
			thread.start();
		}

		boolean offer(Runnable task) {
			return queue.offer(task);
		}

		Runnable pollLocal() {
			return queue.poll();
		}

		Runnable stealFrom(Worker victim) {
			return victim.queue.poll();
		}

		@Override
		public void run() {
			final int n = workers.length;

			while (!shutdown) {
				// 1) Prefer local work
				Runnable task = pollLocal();
				if (task != null) {
					runTask(task);
					idleStrategy.reset();
					continue;
				}

				// 2) Try stealing (randomized start reduces herding)
				int start = ThreadLocalRandom.current().nextInt(n);
				for (int k = 0; k < n; k++) {
					int victimIdx = (start + k) % n;
					if (victimIdx == workerId) continue;

					task = stealFrom(workers[victimIdx]);
					if (task != null) {
						runTask(task);
						idleStrategy.reset();
						break;
					}
				}

				// 3) Nothing found: busy spin
				if (task == null) {
					idleStrategy.idle();
				}
			}
		}

		private void runTask(Runnable task) {
			try {
				task.run();
			} catch (Throwable t) {
				log.error("Error while executing task", t);
			}
		}
	}

	@Override
	public void shutdown() {
		shutdown = true;
	}

	@Override
	public List<Runnable> shutdownNow() {
		throw new UnsupportedOperationException("shutdownNow not supported yet");
	}

	@Override
	public boolean isShutdown() {
		return shutdown;
	}

	@Override
	public boolean isTerminated() {
		throw new UnsupportedOperationException("isTerminated not supported yet");
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		throw new UnsupportedOperationException("awaitTermination not supported yet");
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		throw new UnsupportedOperationException("submit(Callable) not supported yet");
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		throw new UnsupportedOperationException("submit(Runnable,T) not supported yet");
	}

	public Future<?> submitAll(final Collection<? extends Runnable> tasks) {
		if (shutdown) {
			throw new RejectedExecutionException("BusyThreadpool is shutdown");
		}
		if (tasks.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}

		var result = new CompletableFuture<>();
		var remainingTasks = new AtomicInteger(tasks.size());
		var firstError = new AtomicReference<Throwable>(null);

		for (var task : tasks) {
			Runnable wrapped = () -> {
				try {
					task.run();
				} catch (Throwable t) {
					firstError.compareAndSet(null, t);
				} finally {
					if (remainingTasks.decrementAndGet() == 0) {
						Throwable err = firstError.get();
						if (err == null) result.complete(null);
						else result.completeExceptionally(err);
					}
				}
			};
			enqueue(wrapped);
		}
		return result;
	}

	private void enqueue(Runnable wrapped) {

		// Select a random worker to submit a task to.
		int start = submitIndex.getAndIncrement();

		do {
			if (shutdown) {
				throw new RejectedExecutionException("BusyThreadpool is shutdown");
			}

			// if we cannot submit to the first worker directly, try all others.
			for (var i = start; i < workers.length; i++) {
				if (workers[i].offer(wrapped)) {
					return;
				}
			}
			// if we haven't submitted to any worker, start from the first one next time.
			start = 0;
			submitIdleStrategy.idle();
		} while (true);
	}

	@Override
	public Future<?> submit(Runnable task) {
		if (shutdown) {
			throw new RejectedExecutionException("BusyThreadpool is shutdown");
		}

		//var future = new FutureTask<Void>(task, null);
		CompletableFuture<Void> future = new CompletableFuture<>();

		Runnable wrapped = () -> {
			try {
				task.run();
				future.complete(null);
			} catch (Throwable t) {
				future.completeExceptionally(t);
			}
		};

		enqueue(wrapped);
		return future;
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		throw new UnsupportedOperationException("invokeAll not supported yet");
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
		throw new UnsupportedOperationException("invokeAll not supported yet");
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		throw new UnsupportedOperationException("invokeAny not supported yet");
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		throw new UnsupportedOperationException("invokeAny not supported yet");
	}

	@Override
	public void execute(Runnable command) {
		// Keep a simple implementation for callers expecting Executor semantics
		submit(command);
	}
}
