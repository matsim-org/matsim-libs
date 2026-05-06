package org.matsim.testcases.utils;

import java.util.Collection;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class DistributedExecution {

	public static <T> void execute(Collection<T> fromCollection, Consumer<T> consumer) {
		execute(fromCollection, 120, consumer);
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
						tryClose(fromCollection);
						pool.shutdownNow();
					} else {
						done.get();
					}
				} catch (Exception e) {
					tryClose(fromCollection);
					pool.shutdownNow();
					throw new RuntimeException("Error while executing task. Caused by: ", e);
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
					// nothing
				}
			}
		}
	}
}
