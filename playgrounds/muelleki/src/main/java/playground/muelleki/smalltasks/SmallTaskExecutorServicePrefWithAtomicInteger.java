package playground.muelleki.smalltasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
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
			int nThreads = helper.getNThreads();
			ArrayList<ArrayList<Runnable>> taskLists = helper.taskLists;

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

	final SmallTaskExecutorHelper helper;
	private Collection<Callable<Integer>> superTasks;

	final ArrayList<AtomicInteger> currentTasks;

	public SmallTaskExecutorServicePrefWithAtomicInteger(int nThreads) {
		helper = new SmallTaskExecutorHelper(nThreads);
		superTasks = new ArrayList<Callable<Integer>>(nThreads);
		for (int i = 0; i < nThreads; i++)
			superTasks.add(new SuperTask(i));
		currentTasks = new ArrayList<AtomicInteger>(nThreads);
		for (int i = 0; i < nThreads; i++)
			currentTasks.add(new AtomicInteger());
	}

	/* (non-Javadoc)
	 * @see playground.muelleki.perftests.SmallTaskExecutorService#invokeAll(java.util.Collection)
	 */
	@Override
	public void init(Collection<Collection<RunnableCallable>> tasks) {
		helper.doInit(tasks);
	}

	@Override
	public void invokeAll() {
		for (int i = 0; i < helper.getNThreads(); i++)
			currentTasks.get(i).set(0);
		helper.doInvokeAll(superTasks);
	}

	/* (non-Javadoc)
	 * @see playground.muelleki.perftests.SmallTaskExecutorService#shutdown()
	 */
	@Override
	public void shutdown() {
		helper.doShutdown();
	}
}
