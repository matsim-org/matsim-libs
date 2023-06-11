package org.matsim.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author mrieser / Simunto GmbH
 */
public class MemoryObserver {

	private final static Logger LOG = LogManager.getLogger(MemoryObserver.class);

	private static Thread thread = null;
	private static MemoryPrinter runnable = null;

	public static void start(int interval_seconds) {
		startMillis(interval_seconds * 1000L);
	}

	static void startMillis(long interval) {
		stop();

		runnable = new MemoryPrinter(interval);
		thread = new Thread(runnable, "MemoryPrinter");
		thread.setDaemon(true);
		thread.start();
	}

	public static void stop() {
		if (thread != null) {
			runnable.stopFlag.set(true);
			thread.interrupt();
		}
	}

	public static void printMemory() {
		long totalMem = Runtime.getRuntime().totalMemory();
		long freeMem = Runtime.getRuntime().freeMemory();
		long usedMem = totalMem - freeMem;
		LOG.info("used RAM: " + (usedMem/1024/1024) + " MB  free: " + (freeMem/1024/1024) + " MB  total: " + (totalMem/1024/1024) + " MB");
	}

	private static class MemoryPrinter implements Runnable {

		private final long millis;
		private final AtomicBoolean stopFlag = new AtomicBoolean(false);

		private MemoryPrinter(long millis) {
			this.millis = millis;
		}

		public void run() {
			while (true) {
				MemoryObserver.printMemory();

				try {
					Thread.sleep(this.millis);
				} catch (InterruptedException e) {
					if (this.stopFlag.get()) {
						return;
					}
					e.printStackTrace();
				}
			}
		}

	}


}
