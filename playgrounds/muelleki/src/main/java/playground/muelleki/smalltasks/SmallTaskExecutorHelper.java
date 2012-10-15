package playground.muelleki.smalltasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmallTaskExecutorHelper {
	protected final ExecutorService threadPool;
	protected ArrayList<ArrayList<Runnable>> taskLists;
	private final int nThreads;

	public SmallTaskExecutorHelper(int nThreads) {
		super();
		this.nThreads = nThreads;
		threadPool = Executors.newFixedThreadPool(nThreads);
	}

	public void doInit(Collection<Collection<RunnableCallable>> tasks) {
		if (tasks.size() != nThreads)
			throw new IllegalArgumentException("Size of collection must match number of threads.");
		this.taskLists = new ArrayList<ArrayList<Runnable>>(tasks.size());
		for (Collection<RunnableCallable> r : tasks)
			this.taskLists.add(new ArrayList<Runnable>(r));
	}

	public void doInvokeAll(Collection<Callable<Integer>> superTasks) {
		try {
			threadPool.invokeAll(superTasks);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void doShutdown() {
		threadPool.shutdown();
	}

	public int getNThreads() {
		return nThreads;
	}
}