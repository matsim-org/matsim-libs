package playground.muelleki.smalltasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class ExecutorPerfTest {
	private static final int RUNS = 3;
	private static final int ITERATIONS = 10;
	private static final int TOTAL_TASKS = ITERATIONS * 1000000;
	private final int nTasks;
	private final int nSubTasks;
	private final String taskType;

	private TaskFactory getTaskFactory() {
		if (taskType == "random") {
			return new TaskFactory() {
				@Override
				public RunnableCallable create() {
					return new RandomGenerating(nSubTasks);
				}
			};
		}

		throw new IllegalArgumentException("Unknown task type: " + taskType);
	}

	public class SeqTest implements Initializable {
		private RunnableCallable task;

		@Override
		public void init() {
			task = getTaskFactory().create();
		}

		@Override
		public void run() {
			for (int i = 0; i < ITERATIONS * nTasks; i++)
				task.run();
		}

		@Override
		public void shutdown() {
			return;
		}
	}

	public class ExecutorTest implements Initializable {
		private final SmallTaskExecutorService es;
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
			TaskFactory taskFactory = getTaskFactory();
			Collection<Collection<RunnableCallable>> taskLists = new ArrayList<Collection<RunnableCallable>>(nTasks);
			for (int k = 0; k < this.nThreads; k++) {
				ArrayList<RunnableCallable> taskList = new ArrayList<RunnableCallable>();
				for (int i = nTasks * k / this.nThreads; i < nTasks * (k+1) / this.nThreads; i++) {
					taskList.add(taskFactory.create());
				}
				taskLists.add(taskList);
			}
			es.init(taskLists);
		}

		@Override
		public void run() {
			for (int i = 0; i < ITERATIONS; i++)
				es.invokeAll();
		}

		@Override
		public void shutdown() {
			es.shutdown();
		}
	}

	public ExecutorPerfTest(String taskType, int nSubTasks) {
		this.taskType = taskType;
		this.nTasks = TOTAL_TASKS / ITERATIONS / nSubTasks;
		this.nSubTasks = nSubTasks;
	}

	private static class Args {
		static int c = 0;
		@SuppressWarnings("unused")
		public static final int d = 0
		, TASKTYPE = c++
		, SUBTASKS = c++
		, THREADS = c++
		, NAME = c++
		;
	}

	public static void main(String[] args) {
		ExecutorPerfTest executorPerfTest = new ExecutorPerfTest(args[Args.TASKTYPE], Integer.parseInt(args[Args.SUBTASKS]));
		executorPerfTest.start(args);
	}

	private void start(String[] args) {
		final Initializable r;

		System.out.printf("Size(%s, %d, %d, %d): ", taskType, ITERATIONS, nTasks, this.nSubTasks);

		if (args.length == 1) {
			System.out.printf("SeqTest(1, plain): ");
			r = new SeqTest();
		}
		else {
			final int nThreads = Integer.parseInt(args[Args.THREADS]);
			String name = args[Args.NAME];
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
