package playground.wrashid.tryouts.performance;

import org.matsim.core.mobsim.jdeqsim.util.Timer;

public class Test2 {

	/**
	 * @param args
	 */
	public volatile int v = 0;
	public int nv = 0;

	public static void main(String[] args) {
		int endTime = 10000000;

		Test2 t2 = new Test2();

		// synchronized (score: 10.7sec)
		Thread t0 = new Thread(new ARunnable(t2, 0, endTime));
		Thread t1 = new Thread(new ARunnable(t2, 1, endTime));

		/*
		 * //volatile (score: 1.9sec) Thread t0 = new Thread(new BRunnable(t2,
		 * 0, endTime)); Thread t1 = new Thread(new BRunnable(t2, 1, endTime));
		 */

		/*
		 * //volatile Lock used (score: 3.1sec) Thread t0 = new Thread(new
		 * CRunnable(t2, 0, endTime,0)); Thread t1 = new Thread(new
		 * CRunnable(t2, 1, endTime,1));
		 */

		/*
		 * // java.util.concurrent.locks.ReentrantLock is good: (score: 12.7sec)
		 * java.util.concurrent.locks.ReentrantLock lock=new
		 * java.util.concurrent.locks.ReentrantLock(); Thread t0 = new
		 * Thread(new DRunnable(t2, 0, endTime,lock)); Thread t1 = new
		 * Thread(new DRunnable(t2, 1, endTime,lock));
		 */

		t0.start();
		t1.start();

	}

	static class ARunnable implements Runnable {
		private Test2 t2;
		private int endTime;

		public void run() {
			Timer timer = new Timer();
			timer.startTimer();
			int counter = 0;
			while (t2.nv < endTime) {
				synchronized (t2) {
					// satawal needs for this code much longer, than for the code above (loses factor 10)
					if (t2.nv < endTime) {
						counter++;
						t2.nv += 1;
					}

					// if (t2.nv % 2 == parameter) {
					// t2.nv += 1;
					// }
					
					
				}
			}
			timer.endTimer();
			timer.printMeasuredTime("time for synchronied: ");
			System.out.println(Thread.currentThread().getId() + ":" + counter);
		}

		public ARunnable(Test2 t2, int parameter, int endTime) {
			super();
			this.t2 = t2;
			this.endTime = endTime;
		}
	}

	static class BRunnable implements Runnable {
		private Test2 t2;
		private int parameter;
		private int endTime;

		public void run() {
			Timer timer = new Timer();
			timer.startTimer();
			while (t2.v < endTime) {
				if (t2.v % 2 == parameter) {
					t2.v += 1;
				}
			}
			timer.endTimer();
			timer.printMeasuredTime("time for volatile: ");
		}

		public BRunnable(Test2 t2, int parameter, int endTime) {
			super();
			this.t2 = t2;
			this.parameter = parameter;
			this.endTime = endTime;
		}
	}

	static class CRunnable implements Runnable {
		private Test2 t2;
		private int parameter;
		private int endTime;
		private int id;
		private static Lock lock = new Lock();

		public void run() {
			Timer timer = new Timer();
			timer.startTimer();
			while (t2.nv < endTime) {
				lock.getLock(id);
				if (t2.nv % 2 == parameter) {
					t2.nv += 1;
				}
				lock.unLock(id);
			}
			timer.endTimer();
			timer.printMeasuredTime("time for volatile lock: ");
		}

		public CRunnable(Test2 t2, int parameter, int endTime, int id) {
			super();
			this.t2 = t2;
			this.parameter = parameter;
			this.endTime = endTime;
			this.id = id;
		}
	}

	static class Lock {
		// private volatile int wantingToAquireLock[]=new int[2];
		private volatile int hasLock = -1;
		private volatile int hasZeroLock = -1;

		public void getLock(int id) {
			// wantingToAquireLock[id]=1;
			// try to aquire lock
			tryAquireLock(id);

			while (hasLock != id) {
				tryAquireLock(id);
			}
		}

		private void tryAquireLock(int id) {
			// only do this the first time
			if (hasZeroLock == -1) {
				hasZeroLock = id;
				// if someone else overwrites he may enter the lock first
				if (hasZeroLock == id) {
					// not sure, if this check needed
					if (hasLock == -1) {
						hasLock = id;
					}
				}
			}
		}

		// unlock, if has right to unlock and give lock to next waiting
		public void unLock(int id) {
			// if (hasLock==id){
			// wantingToAquireLock[id]=0;
			// }
			// reset lock, need to give back in inverse order
			if (hasLock == id) {
				hasLock = -1;
				hasZeroLock = -1;
			}
		}
	}

	static class DRunnable implements Runnable {
		private Test2 t2;
		private int parameter;
		private int endTime;
		private java.util.concurrent.locks.ReentrantLock lock;

		public void run() {
			Timer timer = new Timer();
			timer.startTimer();
			while (t2.v < endTime) {
				lock.lock();
				if (t2.v % 2 == parameter) {
					t2.v += 1;
				}
				lock.unlock();
			}
			timer.endTimer();
			timer.printMeasuredTime("time for volatile: ");
		}

		public DRunnable(Test2 t2, int parameter, int endTime,
				java.util.concurrent.locks.ReentrantLock lock) {
			super();
			this.t2 = t2;
			this.parameter = parameter;
			this.endTime = endTime;
			this.lock = lock;
		}
	}

}
