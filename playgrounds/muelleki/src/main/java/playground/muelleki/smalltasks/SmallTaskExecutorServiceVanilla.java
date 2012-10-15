package playground.muelleki.smalltasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmallTaskExecutorServiceVanilla implements SmallTaskExecutorService {
	private final ExecutorService threadPool;
	private final int nThreads;
	private ArrayList<Callable<Object>> taskList;

	public SmallTaskExecutorServiceVanilla(int nThreads) {
		this.nThreads = nThreads;
		threadPool = Executors.newFixedThreadPool(nThreads);
	}

	@Override
	public void init(Collection<Collection<RunnableCallable>> tasks) {
		if (tasks.size() != nThreads)
			throw new IllegalArgumentException("Size of collection must match number of threads.");
		 taskList = new ArrayList<Callable<Object>>(tasks.size() * tasks.iterator().next().size());
		 for (Collection<RunnableCallable> r : tasks)
			for (RunnableCallable t : r)
				taskList.add(t);
	}

	@Override
	public void invokeAll() {
		try {
			threadPool.invokeAll(taskList);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void shutdown() {
		threadPool.shutdown();
	}
}
