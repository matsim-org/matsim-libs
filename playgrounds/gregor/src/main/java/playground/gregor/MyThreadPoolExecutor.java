package playground.gregor;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.matsim.core.gbl.MatsimRandom;


public class MyThreadPoolExecutor {
	int poolSize = 4;

	int maxPoolSize = 4;

	long keepAliveTime = 10;

	ThreadPoolExecutor threadPool = null;

	final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(
			5);

	public MyThreadPoolExecutor()
	{
		this.threadPool = new ThreadPoolExecutor(this.poolSize, this.maxPoolSize,
				this.keepAliveTime, TimeUnit.SECONDS, this.queue);

	}

	public void runTask(Runnable task)
	{
		// System.out.println("Task count.."+threadPool.getTaskCount() );
		// System.out.println("Queue Size before assigning the
		// task.."+queue.size() );
		this.threadPool.execute(task);
		// System.out.println("Queue Size after assigning the
		// task.."+queue.size() );
		// System.out.println("Pool Size after assigning the
		// task.."+threadPool.getActiveCount() );
		// System.out.println("Task count.."+threadPool.getTaskCount() );
		System.out.println("Task count.." + this.queue.size());

	}

	public void shutDown()
	{
		this.threadPool.shutdown();
	}

	public static void main(String args[])
	{
		MyThreadPoolExecutor mtpe = new MyThreadPoolExecutor();
		// start first one
		mtpe.runTask(new Task("first"));
		mtpe.runTask(new Task("second"));
		mtpe.runTask(new Task("third"));
		mtpe.runTask(new Task("fourth"));
		mtpe.shutDown();
	}


	private static class Task implements Runnable {
		private final String nr;

		public Task(String string) {
			this.nr = string;
		}

		@Override
		public void run()
		{
			Random rand1 = MatsimRandom.getRandom();
			XORShiftRandom rand2 = new XORShiftRandom();

			long before1 = System.currentTimeMillis();
			for (long i = 0; i < 10000000; i++) {
				double dbl = rand1.nextDouble();
				dbl++; //just in case (fools the compiler)
			}
			long after1 = System.currentTimeMillis();
			System.out.println("run time java.util.Random:" + (after1-before1));

			long before2 = System.currentTimeMillis();
			for (long i = 0; i < 10000000; i++) {
				double dbl =rand2.nextDouble();
				dbl++; //just in case (fools the compiler)
			}
			long after2 = System.currentTimeMillis();
			System.out.println("run time XORShift:" + (after2-before2));
		}
	}

	private static class XORShiftRandom {

		private long x;

		public XORShiftRandom () {
			this.x = System.nanoTime();
		}

		public long randomLong() {
			this.x ^= (this.x << 21);
			this.x ^= (this.x >>> 35);
			this.x ^= (this.x << 4);
			return this.x;
		}

		public double nextDouble() {
			long l = randomLong();
			return (double)l/Long.MAX_VALUE;
		}
	}
}
