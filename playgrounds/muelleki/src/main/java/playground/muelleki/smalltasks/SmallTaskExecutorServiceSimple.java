package playground.muelleki.smalltasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
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
				int nThreads = helper.getNThreads();

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
	private ArrayList<Runnable> taskList;
	private AtomicInteger currentTask;

	public SmallTaskExecutorServiceSimple(int nThreads) {
		helper = new SmallTaskExecutorHelper(nThreads);
		superTasks = new ArrayList<Callable<Integer>>(nThreads);
		for (int i = 0; i < nThreads; i++)
			superTasks.add(new SuperTask(i));

		currentTask = new AtomicInteger();
	}

	@Override
	public void init(Collection<Collection<RunnableCallable>> tasks) {
		if (tasks.size() != helper.getNThreads())
			throw new IllegalArgumentException("Size of collection must match number of threads.");
		this.taskList = new ArrayList<Runnable>(tasks.size());
		for (Collection<RunnableCallable> r : tasks)
			for (Runnable t : r)
				this.taskList.add(t);
	}
	
	@Override
	public void invokeAll() {
		currentTask.set(0);
		helper.doInvokeAll(superTasks);
	}

	@Override
	public void shutdown() {
		helper.doShutdown();
	}
}
