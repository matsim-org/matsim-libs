package playground.muelleki.smalltasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SmallTaskExecutorServicePrefWithAtomicInteger implements SmallTaskExecutorService {
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
				AtomicInteger currentTask = currentTasks.get(i);
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
	private ArrayList<ArrayList<Runnable>> taskLists;
	private ArrayList<AtomicInteger> currentTasks;
	private final int nThreads;

	public SmallTaskExecutorServicePrefWithAtomicInteger(int nThreads) {
		this.nThreads = nThreads;
		threadPool = Executors.newFixedThreadPool(nThreads);
		currentTasks = new ArrayList<AtomicInteger>(nThreads);
	}

	/* (non-Javadoc)
	 * @see playground.muelleki.perftests.SmallTaskExecutorService#invokeAll(java.util.Collection)
	 */
	@Override
	public void invokeAll(Collection<Collection<RunnableCallable>> tasks) throws InterruptedException {
		if (tasks.size() != nThreads)
			throw new IllegalArgumentException("Size of collection must match number of threads.");
		this.taskLists = new ArrayList<ArrayList<Runnable>>(tasks.size());
		for (Collection<RunnableCallable> r : tasks)
			this.taskLists.add(new ArrayList<Runnable>(r));
		for (int i = 0; i < nThreads; i++)
			currentTasks.add(new AtomicInteger(0));

		Collection<Callable<Integer>> superTasks = new ArrayList<Callable<Integer>>(nThreads);
		for (int i = 0; i < nThreads; i++)
			superTasks.add(new SuperTask(i));
		threadPool.invokeAll(superTasks);
	}

	/* (non-Javadoc)
	 * @see playground.muelleki.perftests.SmallTaskExecutorService#shutdown()
	 */
	@Override
	public void shutdown() {
		threadPool.shutdown();
	}
}
