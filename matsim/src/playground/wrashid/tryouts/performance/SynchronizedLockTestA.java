package playground.wrashid.tryouts.performance;

import org.matsim.core.mobsim.jdeqsim.util.Timer;

public class SynchronizedLockTestA {
	public static void main(String[] args) {
		int endTime = 10000000;

		Test2 t2 = new Test2();

		// synchronized (score: 10.7sec)
		Thread t0 = new Thread(new ARunnable(t2, 0, endTime));
		Thread t1 = new Thread(new ARunnable(t2, 1, endTime));

		t0.start();
		t1.start();
	}

	static class ARunnable implements Runnable {
		private Test2 t2;
		private int parameter;
		private int endTime;

		public void run() {
			Timer timer = new Timer();
			timer.startTimer();
			int counter = 0;
			while (t2.nv < endTime) {
				synchronized (t2) {
					// satawal needs for this code much longer, than for the
					// code above (loses factor 10)
					if (t2.nv < endTime) {
						counter++;
						t2.nv += 1;
					}

				}
			}
			timer.endTimer();
			timer.printMeasuredTime("time for ARunnable: ");
			System.out.println(Thread.currentThread().getId() + ":" + counter);
		}

		public ARunnable(Test2 t2, int parameter, int endTime) {
			super();
			this.t2 = t2;
			this.parameter = parameter;
			this.endTime = endTime;
		}
	}

}
