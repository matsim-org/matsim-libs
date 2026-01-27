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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

public class BusyThreadpool implements ExecutorService {

	private static final Logger log = LogManager.getLogger(BusyThreadpool.class);
	private final ManyToManyConcurrentArrayQueue<Runnable> tasks;
	private final Worker[] workers;
	private final IdleWorkerStack idleWorkerStack;

	private volatile boolean shutdown;

	public BusyThreadpool(int size) {

		log.info("Initializing BusyThreadpool with " + size + " workers");
		this.tasks = new ManyToManyConcurrentArrayQueue<>(size * 2);
		this.workers = new Worker[size];
		this.idleWorkerStack = new IdleWorkerStack(size);

		for (int i = 0; i < size; i++) {
			workers[i] = new Worker("busy-threadpool-" + i, i);
			workers[i].start();
		}
	}

	private final class Worker implements Runnable {
		private final Thread thread;
		private final IdleStrategy idleStrategy = new BusySpinIdleStrategy();
		private final int workerId;

		private final AtomicReference<Runnable> currentTask = new AtomicReference<>(null);
		private boolean isInIdleStack = false;

		private Worker(String name, int workerId) {
			this.thread = new Thread(this, name);
			this.workerId = workerId;
			this.thread.setDaemon(true); // optional: choose based on your application lifecycle expectations
		}

		void start() {
			thread.start();
		}

		int id() {
			return workerId;
		}

		boolean runDirect(Runnable task) {
			return currentTask.compareAndSet(null, task);
		}

		@Override
		public void run() {
			while (!shutdown) {

				// directly try to run the direct task with out polling from the queue
				if (currentTask.get() != null) {
					runCurrentTask();
					idleStrategy.reset();
					continue;
				}

				// try the queue next
				var queueTask = tasks.poll();
				if (queueTask != null) {
//					runTask(queueTask);
//					idleStrategy.reset();
//					continue;
					// we try to set the queue task as the current task. If someone else has set the current task directly,
					// we resubmit the task to the queue.
					if (!currentTask.compareAndSet(null, queueTask)) {
						resubmitTask(queueTask);
					}
					// in any case we have a current task set and we can execute it.
					runCurrentTask();
					idleStrategy.reset();
					continue;
				}

				// no direct task and no task in the queue. Idle and then try again.
				if (!isInIdleStack) {
					idleWorkerStack.pushIdle(workerId);
					isInIdleStack = true;
				}
				idleStrategy.idle();
			}
		}

		private void runTask(Runnable task) {
			try {
				task.run();
			} catch (Throwable t) {
				log.error("Error while executing task. This error should be caught by the future in the threadpool?", t);
			}
		}

		private void runCurrentTask() {
			try {
				isInIdleStack = false;
				currentTask.get().run();
			} catch (Throwable t) {
				log.error("Error while executing task. This error should be caught by the future in the threadpool?", t);
			} finally {
				// clear the current task in any case, signaling, that the worker is open for work again.
				currentTask.set(null);
			}
		}

		private void resubmitTask(Runnable task) {
			// reuse idle strategy, but restart it.
			idleStrategy.reset();
			while (!tasks.offer(task)) {
				this.idleStrategy.idle();
			}
			// and reset it again, before we go back into the 'fetch task loop'
			idleStrategy.reset();
		}
	}

	/**
	 * Many producers (workers) push their workerId when they become idle.
	 * Single consumer (submitter thread) pops a workerId to try direct handoff.
	 * <p>
	 * This is an "opportunity list": entries can be stale. Validate by runDirect(CAS).
	 * <p>
	 * Suggested by GPT 5.2
	 */
	public static final class IdleWorkerStack {
		public static final int NONE = -1;

		private final AtomicInteger head = new AtomicInteger(NONE);
		private final int[] next;                     // next[workerId] = next workerId in stack
		private final AtomicIntegerArray inStack;     // 0/1 guard: prevents duplicate push of same workerId

		public IdleWorkerStack(int workerCount) {
			this.next = new int[workerCount];
			this.inStack = new AtomicIntegerArray(workerCount);
			for (int i = 0; i < workerCount; i++) {
				next[i] = NONE;
			}
		}

		/**
		 * Called by worker thread when it transitions to idle (about to back off / park).
		 * Safe to call multiple times; duplicates are ignored.
		 */
		public void pushIdle(int workerId) {
			if (!inStack.compareAndSet(workerId, 0, 1)) {
				return; // already advertised
			}

			int h;
			do {
				h = head.get();
				next[workerId] = h;
			} while (!head.compareAndSet(h, workerId));
		}

		/**
		 * Called by the single submitter thread.
		 *
		 * @return a workerId candidate or NONE if empty.
		 */
		public int popIdleCandidate() {
			int h;
			int nh;
			do {
				h = head.get();
				if (h == NONE) return NONE;
				nh = next[h];
			} while (!head.compareAndSet(h, nh));

			// allow this worker to advertise again later
			inStack.set(h, 0);
			next[h] = NONE; // optional hygiene
			return h;
		}
	}


	@Override
	public void shutdown() {
		shutdown = true;
	}

	@Override
	public List<Runnable> shutdownNow() {
		throw new RuntimeException("not supported yet");
	}

	@Override
	public boolean isShutdown() {
		return shutdown;
	}

	@Override
	public boolean isTerminated() {
		throw new RuntimeException("not supported yet");
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		throw new RuntimeException("not supported yet");
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		throw new RuntimeException("not supported yet");
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		throw new RuntimeException("not supported yet");
	}

	@Override
	public Future<?> submit(Runnable task) {
		if (shutdown) {
			throw new RejectedExecutionException("BusyThreadpool is shutdown");
		}

		CompletableFuture<Void> future = new CompletableFuture<>();

		Runnable wrapped = () -> {
			try {
				task.run();
				future.complete(null);
			} catch (Throwable t) {
				future.completeExceptionally(t);
			}
		};

		// Busy-wait until the bounded queue accepts the task
		final var idleStrategy = new BackoffIdleStrategy();

		// try handing off the task directly.
		int workerIndex;
		while ((workerIndex = idleWorkerStack.popIdleCandidate()) != IdleWorkerStack.NONE) {
			if (workers[workerIndex].runDirect(wrapped)) {
				return future;
			}
		}
		// if we don't succeed, put it in the tasks queue to be executed later.
		while (!tasks.offer(wrapped)) {
			if (shutdown) {
				throw new RejectedExecutionException("BusyThreadpool is shutdown");
			}
			idleStrategy.idle(0);
		}

		return future;
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		throw new RuntimeException("not supported yet");
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
		throw new RuntimeException("not supported yet");
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		throw new RuntimeException("not supported yet");
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		throw new RuntimeException("not supported yet");
	}

	@Override
	public void execute(Runnable command) {
		throw new RuntimeException("not supported yet");
	}
}
