package playground.wrashid.tryouts.performance;

import org.matsim.core.mobsim.jdeqsim.util.Timer;

/*
 * TODO: Do refactoring, so that code from TestA is not duplicated here...
 */
public class SynchronizedLockTestB {
	public static void main(String[] args) {
		int endTime = 10000000;

		Test2 t2 = new Test2();

		Thread t0 = new Thread(new BRunnable(t2, 0, endTime));
		Thread t1 = new Thread(new BRunnable(t2, 1, endTime));

		t0.start();
		t1.start();
	}

	static class BRunnable implements Runnable {
		private Test2 t2;
		private int parameter;
		private int endTime;

		public void run() {
			Timer timer = new Timer();
			timer.startTimer();
			int counter = 0;
			while (t2.nv < endTime) {
				synchronized (t2) {
					// this code really runs bad on satawal: eventhough both
					// threads come into this part in quite fair way
					// perhaps one could look at the sequence of how each of the
					// hosts
					// lets the different threads in here...
					// # the number of times, the threads have been in here
					// (counter)
					// more than needed (endTime), shows this effect.

					// for local host:
					// ================
					// time for BRunnable: 45845
					// time for BRunnable: 45845
					// 8:10252671
					// 7:20939231
					// fairness factor: this would be one, if one and then the
					// other thread would get the lock
					// in this case the fairness factor is
					// endTime/sum(counter)=10000000/31191902=0.320596

					// time for BRunnable: 102737
					// time for BRunnable: 102737
					// 10:186384721
					// 9:150479233
					// fairness factor: 10000000/336863954=0.02968

					// this means, somehow the fairness of threads to get a lock
					// is much better implemented on the local vm / operating
					// system, than on the one on satwal

					// this phenomina should be analyed with more threads
					// also...
					counter++;
					if (t2.nv % 2 == parameter) {
						t2.nv += 1;
					}
				}
			}
			timer.endTimer();
			timer.printMeasuredTime("time for BRunnable: ");
			System.out.println(Thread.currentThread().getId() + ":" + counter);
		}

		public BRunnable(Test2 t2, int parameter, int endTime) {
			super();
			this.t2 = t2;
			this.parameter = parameter;
			this.endTime = endTime;
		}
	}

}
