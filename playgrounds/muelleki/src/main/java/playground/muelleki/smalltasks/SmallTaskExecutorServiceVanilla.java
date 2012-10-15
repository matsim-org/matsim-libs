package playground.muelleki.smalltasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmallTaskExecutorServiceVanilla implements SmallTaskExecutorService {
	private final ExecutorService threadPool;
	private final int nThreads;

	public SmallTaskExecutorServiceVanilla(int nThreads) {
		this.nThreads = nThreads;
		threadPool = Executors.newFixedThreadPool(nThreads);
	}

	public void invokeAll(Collection<Collection<RunnableCallable>> tasks) throws InterruptedException {
		if (tasks.size() != nThreads)
			throw new IllegalArgumentException("Size of collection must match number of threads.");
		 ArrayList<Callable<Object>> taskList = new ArrayList<Callable<Object>>(tasks.size() * tasks.iterator().next().size());
		 for (Collection<RunnableCallable> r : tasks)
			for (RunnableCallable t : r)
				taskList.add(t);

		threadPool.invokeAll(taskList);
	}

	public void shutdown() {
		threadPool.shutdown();
	}
}
