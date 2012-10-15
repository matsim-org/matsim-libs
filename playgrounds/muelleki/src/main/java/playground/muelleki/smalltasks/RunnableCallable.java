package playground.muelleki.smalltasks;

import java.util.concurrent.Callable;

public abstract class RunnableCallable implements Runnable, Callable<Object> {
	@Override
	public Object call() throws Exception {
		run();
		return null;
	}
}
