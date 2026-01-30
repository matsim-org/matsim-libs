package org.matsim.dsim.executors;

import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ManyToManyConcurrentArrayQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

class BusyThreadpool {

	private static final Logger log = LogManager.getLogger(BusyThreadpool.class);

	public enum ExecutionMode {
		PAUSED,
		BOOST,
		SHUTDOWN
	}

	private final Worker[] workers;
	private final AtomicLong submitIndex = new AtomicLong(0);
	private final IdleStrategy submitIdleStrategy = new BusySpinIdleStrategy();

	private volatile ExecutionMode executionMode = ExecutionMode.PAUSED;

	BusyThreadpool(int size) {
		this(size, 128);
	}

	BusyThreadpool(int size, int perWorkerQueueCapacity) {
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
		private final Random rnd;
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
			this.rnd = new Random(workerId);
		}

		void start() {
			thread.start();
		}

		void unpark() {
			LockSupport.unpark(thread);
		}

		boolean offer(Runnable task) {
			return queue.offer(task);
		}

		@Override
		public void run() {

			while (true) {

				// fetch work from our queue or from someone else
				if (doWork() || stealWork()) {
					idleStrategy.reset();
					continue;
				}

				// if we didn't have any work to do check for shutdown or pause.
				if (executionMode == ExecutionMode.SHUTDOWN) {
					scanForWorkOnShutdown();
					return;
				} else if (executionMode == ExecutionMode.PAUSED) {
					park();
					continue;
				}
				// no work and not shutdown or pause? idle!
				idleStrategy.idle();
			}
		}

		private boolean doWork() {
			Runnable task = queue.poll();
			if (task != null) {
				runTask(task);
				return true;
			}
			return false;
		}

		private boolean stealWork() {
			final int n = workers.length;
			if (n == 1) return false;

			int start = rnd.nextInt(n);
			int stride = 1 + rnd.nextInt(n - 1);
			int stealAttempts = Math.min(4, n);

			// only attempt a few times to steal from others before we try our own queue again.
			for (var i = 0; i < stealAttempts; i++) {
				int victimIdx = (start + stride * i) % n;
				if (victimIdx == workerId) continue;

				var task = workers[victimIdx].queue.poll();
				if (task != null) {
					runTask(task);
					return true;
				}
			}
			return false;
		}

		private void scanForWorkOnShutdown() {
			final int n = workers.length;

			for (var i = 0; i < n; i++) {
				var task = workers[i].queue.poll();
				if (task != null) {
					runTask(task);
					i = -1; // rescan from the beginning (the loop will set i to 0).
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

		private void park() {
			while (true) {
				switch (executionMode) {
					case PAUSED -> LockSupport.park(this);
					case BOOST, SHUTDOWN -> {
						return;
					}
				}
			}
		}
	}

	public void pause() {
		executionMode = ExecutionMode.PAUSED;
	}

	/**
	 * Transition the pool into BOOST mode and unpark all workers.
	 */
	public void resume() {
		executionMode = ExecutionMode.BOOST;
		for (Worker w : workers) {
			w.unpark();
		}
	}

	public void shutdown() {
		executionMode = ExecutionMode.SHUTDOWN;
		for (Worker w : workers) {
			w.unpark();
		}
	}

	public boolean isPaused() {
		return executionMode == ExecutionMode.PAUSED;
	}

	public boolean isShutdown() {
		return executionMode == ExecutionMode.SHUTDOWN;
	}

	public Future<?> submitAll(final Collection<? extends Runnable> tasks) {
		if (executionMode == ExecutionMode.SHUTDOWN || executionMode == ExecutionMode.PAUSED) {
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

	public Future<?> submit(Runnable task) {
		if (executionMode == ExecutionMode.SHUTDOWN || executionMode == ExecutionMode.PAUSED) {
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

	private void enqueue(Runnable wrapped) {

		// Select a random worker to submit a task to.
		var start = submitIndex.getAndIncrement();

		do {
			if (executionMode == ExecutionMode.SHUTDOWN) {
				throw new RejectedExecutionException("BusyThreadpool is shutdown");
			}

			// if we cannot submit to the first worker directly, try all others.
			for (var attempts = 0; attempts < workers.length; attempts++) {
				int workerIdx = (int) ((start + attempts) % workers.length);
				if (workers[workerIdx].offer(wrapped)) {
					submitIdleStrategy.reset();
					return;
				}
			}
			// if we haven't submitted to any worker, try again next time, starting with another one.
			start++;
			submitIdleStrategy.idle();
		} while (true);
	}
}
