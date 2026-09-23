package org.matsim.contrib.pseudosimulation.distributed;

class MemoryUsageCalculator {

	private static final long SLEEP_INTERVAL_MILLIS = 100;

	interface MemoryRuntime {
		long totalMemory();

		long freeMemory();

		void collectGarbage();

		void runFinalization();
	}

	interface Sleeper {
		void sleep(long milliseconds) throws InterruptedException;
	}

	private final MemoryRuntime runtime;
	private final Sleeper sleeper;

	MemoryUsageCalculator() {
		this(new SystemMemoryRuntime(), Thread::sleep);
	}

	MemoryUsageCalculator(MemoryRuntime runtime, Sleeper sleeper) {
		this.runtime = runtime;
		this.sleeper = sleeper;
	}

	long getMemoryUse() {
		putOutTheGarbage();
		long totalMemory = runtime.totalMemory();
		putOutTheGarbage();
		long freeMemory = runtime.freeMemory();
		return totalMemory - freeMemory;
	}

	private void putOutTheGarbage() {
		collectGarbage();
		collectGarbage();
	}

	private void collectGarbage() {
		try {
			runtime.collectGarbage();
			sleeper.sleep(SLEEP_INTERVAL_MILLIS);
			runtime.runFinalization();
			sleeper.sleep(SLEEP_INTERVAL_MILLIS);
		} catch (InterruptedException exception) {
			exception.printStackTrace();
		}
	}

	private static final class SystemMemoryRuntime implements MemoryRuntime {

		@Override
		public long totalMemory() {
			return Runtime.getRuntime().totalMemory();
		}

		@Override
		public long freeMemory() {
			return Runtime.getRuntime().freeMemory();
		}

		@Override
		public void collectGarbage() {
			System.gc();
		}

		@Override
		@SuppressWarnings("removal") // Preserve the legacy measurement sequence until finalization disappears from Java.
		public void runFinalization() {
			System.runFinalization();
		}
	}
}
