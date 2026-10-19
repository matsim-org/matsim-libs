package org.matsim.testcases.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class DistributedExecution {

	private static final Logger log = LogManager.getLogger(DistributedExecution.class);

	public static <T> void execute(Collection<T> fromCollection, Consumer<T> consumer) {
		execute(fromCollection, 300, consumer);
	}

	public static <T> void execute(Collection<T> fromCollection, int timeoutInSeconds, Consumer<T> consumer) {
		try (var pool = Executors.newFixedThreadPool(fromCollection.size())) {
			var completionService = new ExecutorCompletionService<Void>(pool);
			for (var e : fromCollection) {
				completionService.submit(() -> {
					consumer.accept(e);
					return null;
				});
			}

			for (var i = 0; i < fromCollection.size(); i++) {
				try {
					var done = completionService.poll(timeoutInSeconds, TimeUnit.SECONDS);
					if (done == null) {
						log.error("Task timed out after {} seconds. Shutting down threadpool.", timeoutInSeconds);
						tryClose(fromCollection);
						pool.shutdownNow();
					} else {
						done.get();
					}
				} catch (Exception e) {
					log.error("Future returned error state. Shutting down threadpool and rethrowing exception.");
					tryClose(fromCollection);
					pool.shutdownNow();
					throw new RuntimeException(e);
				}
			}
		}
	}

	private static <T> void tryClose(Collection<T> items) {
		for (var item : items) {
			if (item instanceof AutoCloseable ac) {
				try {
					ac.close();
				} catch (Exception e) {
					// nothing, try to close others as well
				}
			}
		}
	}
}
