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

/**
 * A thread pool implementation that uses busy-waiting for worker threads to process tasks. This threadpool is optimized for the workload of
 * {@link PoolExecutor} which drives the DSim.
 * <p>
 * The threadpool spawns `size` threads on creation. The threads immediately go into park. Before the pool can be used {@link #resume()} must be
 * called which wakes up all worker threads of the pool. Ater {@link #resume()} has been called, worker threads busy spin for new tasks which will
 * saturate the CPU if sufficient tasks are requested. The busy spinning can be paused by calling {@link #pause()}. When paused, the pool does NOT
 * accept new tasks. It will continue to finish all submitted tasks, before worker threads are parked.
 * <p>
 * NOTE: This pool is optimized for one coordinator thread which submits new tasks. I have not tested how it behaves with multiple threads submitting
 * tasks. I guess it will work 🙃
 * <p>
 * NOTE: This pool busy spins until tasks are submitted. This will fry your CPU and block other threads. Use {@link #pause()} and {@link #resume()} to
 * free CPU ressources
 */
class BusyThreadpool {

	private static final Logger log = LogManager.getLogger(BusyThreadpool.class);

	private enum ExecutionMode {
		PAUSED,
		BUSY,
		SHUTDOWN
	}

	private final Worker[] workers;
	private final AtomicLong submitIndex = new AtomicLong(0);
	private final IdleStrategy submitIdleStrategy = new BusySpinIdleStrategy();

	private volatile ExecutionMode executionMode = ExecutionMode.PAUSED;

	BusyThreadpool(int size) {
		// use 128 tasks per default. Usually, we have less than 10 tasks per sim step.
		this(size, 128);
	}

	BusyThreadpool(int size, int perWorkerQueueCapacity) {
		log.info("Initializing BusyThreadpool with {} workers; per-worker queue capacity={}", size, perWorkerQueueCapacity);

		this.workers = new Worker[size];
		for (int i = 0; i < size; i++) {
			workers[i] = new Worker(i, perWorkerQueueCapacity);
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

		private Worker(int workerId, int queueCapacity) {
			this.thread = new Thread(this, "DSim-Worker-" + workerId);
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
				log.error("Error while executing task. This error should bubble to the caller via its future.", t);
			}
		}

		private void park() {
			// this needs to be in a loop, as threads might wake up from park sporadically
			// in that case we check the pool status and decide what to do.
			while (true) {
				switch (executionMode) {
					case PAUSED -> LockSupport.park(this);
					case BUSY, SHUTDOWN -> {
						return;
					}
				}
			}
		}
	}

	/**
	 * Pause the pool. The pool will finish all submitted tasks. When all tasks are finished, all
	 * worker threads are parked.
	 * <p>
	 * When the pool is paused, no new tasks are accepted.
	 */
	public void pause() {
		executionMode = ExecutionMode.PAUSED;
	}

	/**
	 * Unparks all workers. The pool eagerly awaits tasks after this. Internally,
	 * it uses busy spin to wait for new tasks, which will cause a lot of CPU usage.
	 */
	public void resume() {
		executionMode = ExecutionMode.BUSY;
		for (Worker w : workers) {
			w.unpark();
		}
	}

	/**
	 * Shuts down the pool. The pool will try to finish all submitted tasks before shutting down.
	 */
	public void shutdown() {
		executionMode = ExecutionMode.SHUTDOWN;
		for (Worker w : workers) {
			w.unpark();
		}
	}

	/**
	 * Indicates whether the pool is paused.
	 */
	public boolean isPaused() {
		return executionMode == ExecutionMode.PAUSED;
	}

	/**
	 * Indicates whether the pool is shutdown.
	 */
	public boolean isShutdown() {
		return executionMode == ExecutionMode.SHUTDOWN;
	}

	/**
	 * Submit multiple tasks to the pool. The pool will execute all tasks as soon as there is a free worker thread.
	 * This method is equivalent to calling {@link #submit(Runnable)} for each task but it only produces a single future.
	 * This saves a tiny bit of allocation for tasks, such as the SimProcess, which need to be submitted and awaited
	 * together often.
	 *
	 * @return One future to await them all
	 */
	public Future<?> submitAll(final Collection<? extends Runnable> tasks) {
		if (isShutdown() || isPaused()) {
			throw new RejectedExecutionException("BusyThreadpool is shutdown or paused");
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

	/**
	 * Submits a task to the pool. The pool will execute this task as soon as there is a free worker thread.
	 *
	 * @return A future which completes when the task has been executed.
	 */
	public Future<?> submit(Runnable task) {
		if (isShutdown() || isPaused()) {
			throw new RejectedExecutionException("BusyThreadpool is shutdown or paused.");
		}

		var future = new FutureTask<Void>(task, null);
		enqueue(future);
		return future;
	}

	/**
	 * Internal mechanism to submit a task to the workers. A random worker queue is selected and we try to submit
	 * to its queue. If we fail to do so, we try all other workers. If this fails too, we start another round,
	 * starting from the worker next to the one we tried the very first.
	 */
	private void enqueue(Runnable wrapped) {

		// Select a random worker to submit a task to.
		var start = submitIndex.getAndIncrement();

		do {
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
