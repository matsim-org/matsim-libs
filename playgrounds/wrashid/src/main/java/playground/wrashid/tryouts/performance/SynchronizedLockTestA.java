package playground.wrashid.tryouts.performance;

import org.matsim.core.mobsim.jdeqsim.util.Timer;

public class SynchronizedLockTestA {
	public static void main(String[] args) {
		int endTime = 10000000;

		Object object = new Object();
		int numberOfThreads = 2;//Integer.parseInt(args[0]);

		// synchronized (score: 10.7sec)
		Thread[] threads = new Thread[numberOfThreads];

		for (int i = 0; i < numberOfThreads; i++) {
			threads[i] = new Thread(new ARunnable(object, endTime));
		}

		for (int i = 0; i < numberOfThreads; i++) {
			threads[i].start();
		}

	}

	static class ARunnable implements Runnable {
		private Object lock;
		private int endTime;
		private static int globalCounter;

		public void run() {
			Timer timer = new Timer();
			timer.startTimer();
			int counter = 0;
			while (globalCounter < endTime) {
				synchronized (lock) {
					// Satawal needs for this code much less, than local my host...
					// in particular, as the number is varying.
					
					// need to take the average of the ratios between the threads, as fluctuating.
					// satwal is much fairer in the long run -> we have less fluctuations there...
					if (globalCounter < endTime) {
						counter++;
						globalCounter += 1;
					}

				}
			}
			timer.endTimer();
			timer.printMeasuredTime("time for ARunnable: ");
			System.out.println(Thread.currentThread().getId() + ":" + counter);
		}

		public ARunnable(Object lock, int endTime) {
			super();
			this.lock = lock;
			this.endTime = endTime;
		}
	}

}
