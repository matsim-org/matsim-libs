package playground.muelleki.smalltasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

public class ExecutorPerfTest {
	private static final int RUNS = 3;
	private static final int TOTAL_TASKS = 20000000;
	private final int nTasks;
	private final int nSubTasks;

	static final ThreadLocal<Random> R = new ThreadLocal<Random>() {
		@Override
		protected synchronized Random initialValue() {
			return new Random();
		}
	};

	public class SeqTest implements Initializable {
		@Override
		public void init() {
			return;
		}

		@Override
		public void run() {
			Random r = R.get();
			for (int i = 0; i < nTasks * nSubTasks; i++)
				r.nextDouble();
		}

		@Override
		public void shutdown() {
			return;
		}
	}

	public class ExecutorTest implements Initializable {
		private final class RandomGenerating implements RunnableCallable {
			@Override
			public void run() {
				Random r = R.get();
				for (int j = 0; j < nSubTasks; j++)
					r.nextDouble();
			}

			@Override
			public Object call() throws Exception {
				run();
				return null;
			}
		}

		private final SmallTaskExecutorService es;
		private Collection<Collection<RunnableCallable>> taskLists = new ArrayList<Collection<RunnableCallable>>(nTasks);
		private final int nThreads;

		public ExecutorTest(int nThreads, String name) {
			this.nThreads = nThreads;
			if (name.equalsIgnoreCase("simple"))
				es = new SmallTaskExecutorServiceSimple(nThreads);
			else if (name.equalsIgnoreCase("prefintarray"))
				es = new SmallTaskExecutorServicePrefWithAtomicIntegerArray(nThreads);
			else if (name.equalsIgnoreCase("vanilla"))
				es = new SmallTaskExecutorServiceVanilla(nThreads);
			else if (name.equalsIgnoreCase("prefint"))
				es = new SmallTaskExecutorServicePrefWithAtomicInteger(nThreads);
			else if (name.equalsIgnoreCase("prefintdc"))
				es = new SmallTaskExecutorServicePrefWithAtomicIntegerDC(nThreads);
			else if (name.equalsIgnoreCase("prescheduled"))
				es = new SmallTaskExecutorServicePrescheduled(nThreads);
			else
				throw new IllegalArgumentException("Unknown small task executor: " + name);
		}

		@Override
		public void init() {
			for (int k = 0; k < this.nThreads; k++) {
				ArrayList<RunnableCallable> taskList = new ArrayList<RunnableCallable>();
				for (int i = nTasks * k / this.nThreads; i < nTasks * (k+1) / this.nThreads; i++)
					taskList.add(new RandomGenerating());
				taskLists.add(taskList);
			}
		}

		@Override
		public void run() {
			try {
				es.invokeAll(taskLists);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void shutdown() {
			es.shutdown();
		}
	}

	public ExecutorPerfTest(int nSubTasks) {
		this.nTasks = TOTAL_TASKS / nSubTasks;
		this.nSubTasks = nSubTasks;
	}

	public static void main(String[] args) {
		ExecutorPerfTest executorPerfTest = new ExecutorPerfTest(Integer.parseInt(args[0]));
		executorPerfTest.start(args);
	}

	private void start(String[] args) {
		final Initializable r;
		
		System.out.printf("Size(%d, %d): ", nTasks, this.nSubTasks);
		
		if (args.length == 1) {
			System.out.printf("SeqTest(1, plain): ");
			r = new SeqTest();
		}
		else {
			final int nThreads = Integer.parseInt(args[1]);
			String name = args[2];
			System.out.printf("ExecutorTest(%d, %s): ", nThreads, name);
			r = new ExecutorTest(nThreads, name);
		}
		ArrayList<Long> TimeStamps = new ArrayList<Long>(20);
		TimeStamps.add(System.nanoTime());
		r.init();
		TimeStamps.add(System.nanoTime());
		for (int i = 0; i < RUNS; i++) {
			r.run();
			TimeStamps.add(System.nanoTime());
		}
		r.shutdown();
		TimeStamps.add(System.nanoTime());
		System.gc();
		TimeStamps.add(System.nanoTime());
		Iterator<Long> it = TimeStamps.iterator();
		long t = it.next();
		while (it.hasNext()) {
			long tn = it.next();
			System.out.printf("%d, ", tn - t);
			t = tn;
		}
		System.out.printf("\n", TimeStamps.toString());
	}
}
