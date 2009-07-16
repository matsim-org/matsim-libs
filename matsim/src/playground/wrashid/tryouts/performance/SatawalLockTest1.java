package playground.wrashid.tryouts.performance;

import org.matsim.core.mobsim.jdeqsim.util.Timer;


/*
 * 

- this experiment performs good at satawal, if we increase the work after the synchronized block
- the local machine performs better, if we decrease the size of the work.
=> What kind of work do we have in the simulation?
===============================
endTime = 10000; 
for (int i=0;i<5000;i++){
totalNumberOfThreads=4;
local: 22.5 seconds
satawal: 61 ms

================================
endTime = 1000000; 
for (int i=0;i<100;i++){
totalNumberOfThreads=4;
local: 9.1 seconds
satawal: 23.4 seconds



TODO: auf satawal same experiment...

 */
public class SatawalLockTest1 {
	public static void main(String[] args) {
		int endTime = 1000000;
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
				for (int i=0;i<100;i++){
					
				}
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
