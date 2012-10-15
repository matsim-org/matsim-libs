package playground.muelleki.smalltasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

public class SmallTaskExecutorServicePrescheduled implements SmallTaskExecutorService {
	private final class SuperTask implements Callable<Integer> {
		private final int thread;

		public SuperTask(int thread) {
			this.thread = thread;
		}

		@Override
		public Integer call() throws Exception {
			int nTasks = 0;
			ArrayList<ArrayList<Runnable>> taskLists = helper.taskLists;

			int i = thread;
			ArrayList<Runnable> taskList = taskLists.get(i);
			
			for (Runnable task : taskList) {
				task.run();
				nTasks++;
			}

			return nTasks;
		}
	}

	final SmallTaskExecutorHelper helper;
	private Collection<Callable<Integer>> superTasks;

	public SmallTaskExecutorServicePrescheduled(int nThreads) {
		helper = new SmallTaskExecutorHelper(nThreads);
		superTasks = new ArrayList<Callable<Integer>>(nThreads);
		for (int i = 0; i < nThreads; i++)
			superTasks.add(new SuperTask(i));
	}

	@Override
	public void init(Collection<Collection<RunnableCallable>> tasks) {
		helper.doInit(tasks);
	}

	/* (non-Javadoc)
	 * @see playground.muelleki.perftests.SmallTaskExecutorService#invokeAll(java.util.Collection)
	 */
	@Override
	public void invokeAll() {
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
