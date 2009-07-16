package playground.wrashid.tryouts.performance;

import org.matsim.core.mobsim.jdeqsim.util.Timer;


/*
 * Local machine: endTime=10M, threads=4, time needed: 79.4 seconds
 * =====================================
10 - (number of times aquired the lock): 6099482
9 - (number of times aquired the lock): 5298961
8 - (number of times aquired the lock): 5918216
7 - (number of times aquired the lock): 5690584


TODO: auf satawal same experiment...

 */
public class SatwalLockTest1 {
	public static void main(String[] args) {
		int endTime = 10000000;
		int totalNumberOfThreads=4;

		Test2 test = new Test2();

		Thread t0 = new Thread(new CRunnable(test, 0, endTime, totalNumberOfThreads));
		Thread t1 = new Thread(new CRunnable(test, 1, endTime, totalNumberOfThreads));
		Thread t2 = new Thread(new CRunnable(test, 2, endTime, totalNumberOfThreads));
		Thread t3 = new Thread(new CRunnable(test, 3, endTime, totalNumberOfThreads));

		t0.start();
		t1.start();
		t2.start();
		t3.start();
	}
	
	static class CRunnable implements Runnable {
		private Test2 t2;
		private int parameter;
		private int endTime;
		private int totalNumberOfThreads;

		public void run() {
			Timer timer = new Timer();
			timer.startTimer();
			int counter = 0;
			while (t2.nv < endTime) {
				synchronized (t2) {
					counter++;
					if (t2.nv % totalNumberOfThreads == parameter) {
						t2.nv += 1;
					}
				}
				
				// some task to do (e.g. processing events).
				Math.log(99);
				Math.log(99);
				Math.log(99);
				Math.log(99);
			}
			
			
			timer.endTimer();
			timer.printMeasuredTime("time for BRunnable: ");
			System.out.println(Thread.currentThread().getId() + " - (number of times aquired the lock): " + counter);
		}

		public CRunnable(Test2 t2, int parameter, int endTime, int totalNumberOfThreads) {
			super();
			this.t2 = t2;
			this.parameter = parameter;
			this.endTime = endTime;
			this.totalNumberOfThreads=totalNumberOfThreads;
		}
	}

	
}
