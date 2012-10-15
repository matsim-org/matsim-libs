package playground.muelleki.smalltasks;

import java.util.Collection;

public interface SmallTaskExecutorService {

	public abstract void invokeAll(Collection<Collection<RunnableCallable>> tasks)
			throws InterruptedException;

	public abstract void shutdown();

}