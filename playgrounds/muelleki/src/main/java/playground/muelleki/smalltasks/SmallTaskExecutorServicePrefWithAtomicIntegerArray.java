package playground.muelleki.smalltasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class SmallTaskExecutorServicePrefWithAtomicIntegerArray implements SmallTaskExecutorService {
	private final class SuperTask implements Callable<Integer> {
		private final int thread;

		public SuperTask(int thread) {
			this.thread = thread;
		}

		@Override
		public Integer call() throws Exception {
			int nTasks = 0;
			for (int i = thread;;) {
				ArrayList<Runnable> taskList = taskLists.get(i);
				int task;

				while ((task = currentTasks.getAndIncrement(i)) < taskList.size()) {
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
	private ArrayList<ArrayList<Runnable>> taskLists;
	private AtomicIntegerArray currentTasks;
	private final int nThreads;

	public SmallTaskExecutorServicePrefWithAtomicIntegerArray(int nThreads) {
		this.nThreads = nThreads;
		threadPool = Executors.newFixedThreadPool(nThreads);
		currentTasks = new AtomicIntegerArray(nThreads);
	}

	public void invokeAll(Collection<Collection<RunnableCallable>> tasks) throws InterruptedException {
		if (tasks.size() != nThreads)
			throw new IllegalArgumentException("Size of collection must match number of threads.");
		this.taskLists = new ArrayList<ArrayList<Runnable>>(tasks.size());
		for (Collection<RunnableCallable> r : tasks)
			this.taskLists.add(new ArrayList<Runnable>(r));
		for (int i = 0; i < nThreads; i++)
			currentTasks.set(i, 0);

		Collection<Callable<Integer>> superTasks = new ArrayList<Callable<Integer>>(nThreads);
		for (int i = 0; i < nThreads; i++)
			superTasks.add(new SuperTask(i));
		threadPool.invokeAll(superTasks);
	}

	public void shutdown() {
		threadPool.shutdown();
	}
}
