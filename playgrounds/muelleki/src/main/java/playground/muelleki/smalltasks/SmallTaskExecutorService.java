package playground.muelleki.smalltasks;

import java.util.Collection;

public interface SmallTaskExecutorService {

	public abstract void init(Collection<Collection<RunnableCallable>> tasks);

	public abstract void invokeAll();

	public abstract void shutdown();

}