package playground.muelleki.smalltasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SmallTaskExecutorServiceSimple implements SmallTaskExecutorService {
	private final class SuperTask implements Callable<Integer> {
		private final int thread;

		public SuperTask(int thread) {
			this.thread = thread;
		}

		@Override
		public Integer call() throws Exception {
			int nTasks = 0;
			for (int i = thread;;) {
				int task;

				while ((task = currentTask.getAndIncrement()) < taskList.size()) {
					taskList.get(task).run();
					nTasks++;
				}

				i = (i + 1) % nThreads;
				if (i == thread)
					break;
			}

			return nTasks;
		}
	}

	private final ExecutorService threadPool;
	private ArrayList<Runnable> taskList;
	private AtomicInteger currentTask;
	private final int nThreads;

	public SmallTaskExecutorServiceSimple(int nThreads) {
		this.nThreads = nThreads;
		threadPool = Executors.newFixedThreadPool(nThreads);
		currentTask = new AtomicInteger();
	}

	public void invokeAll(Collection<Collection<RunnableCallable>> tasks) throws InterruptedException {
		if (tasks.size() != nThreads)
			throw new IllegalArgumentException("Size of collection must match number of threads.");
		this.taskList = new ArrayList<Runnable>(tasks.size());
		for (Collection<RunnableCallable> r : tasks)
			for (Runnable t : r)
				this.taskList.add(t);
		currentTask.set(0);

		Collection<Callable<Integer>> superTasks = new ArrayList<Callable<Integer>>(nThreads);
		for (int i = 0; i < nThreads; i++)
			superTasks.add(new SuperTask(i));
		threadPool.invokeAll(superTasks);
	}

	public void shutdown() {
		threadPool.shutdown();
	}
}
