package org.matsim.contrib.msp.traveltimematrix.experiments;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;
import java.util.function.ToIntBiFunction;

/**
 * How long a batch of origin-destination queries takes on <b>several threads</b>, repeated so that the
 * spread is reported rather than hidden.
 * <p>
 * {@link QueryBenchmark} answers a different question and both are needed. That one drives one thread to
 * a hundred million queries to find the <em>mean cost of a query</em>, which is the number a break-even
 * calculation needs. This one asks what a caller actually waits for: a batch of a given size, answered by
 * the number of threads the caller has, including the cost of handing the work out. The two differ by more
 * than the thread count - a batch of ten thousand lookups is a few hundred microseconds of work, so the
 * thread pool, the memory hierarchy and the operating system's scheduler are all visible in it, and a rate
 * measured over a hundred million queries does not predict it.
 * <p>
 * <b>Three things it does on purpose.</b>
 * <ul>
 *     <li><b>Each thread gets its own stretch of the pool</b>, starting at its own offset. Eight threads
 *     cycling the same pairs in the same order would read the same cache lines eight times over and
 *     measure a warm cache rather than a matrix.</li>
 *     <li><b>One answerer per thread</b>, from an {@link IntFunction}. A matrix lookup is read-only and
 *     could share one, but a {@link org.matsim.core.router.speedy.LeastCostPathTree} cannot be shared at
 *     all, and a benchmark that silently shared one would measure corruption. The function is handed the
 *     thread's index so the two cases are written the same way.</li>
 *     <li><b>The dispatch is inside the measurement.</b> The clock starts before the tasks are submitted
 *     and stops when the last one is done, because a caller who wants a batch answered pays for that too.
 *     What it costs is not left to be argued about: {@link #dispatchFloor()} is an answerer that does no
 *     work, so the same harness over it measures the floor, and the overhead is a row of the table
 *     instead of a caveat in the text.</li>
 * </ul>
 * <b>What a measurement from here does not transfer between machines.</b> A lookup in a multi-gigabyte
 * matrix is a random memory access, so the rate is set by memory latency and by how many misses the
 * hardware keeps in flight - not by the clock speed. Threads help mostly because they add outstanding
 * misses, which is why the speedup can exceed the core count in the small sizes and fall well short of it
 * once the memory system saturates. Both of those depend on the machine, so a speedup measured on one
 * machine is an estimate and not a conversion factor on another. The way to avoid needing one is to
 * measure the batch and the build in the same job on the same node, which is what
 * {@link org.matsim.contrib.msp.traveltimematrix.run.RunQueryBenchmark} is for.
 *
 * @see QueryBenchmark the single-threaded rate, over a ladder of query counts
 * @see org.matsim.contrib.msp.traveltimematrix.run.RunQueryBenchmark the command that runs this over a
 * matrix and over the routers it has to beat
 */
public final class ParallelQueryBenchmark {

	private static final Logger log = LogManager.getLogger(ParallelQueryBenchmark.class);

	/**
	 * How many repetitions are always thrown away before the clock counts. Two, because the first touches
	 * pages and cache lines that the second finds mapped and warm.
	 */
	private static final int DISCARDED_REPETITIONS = 2;

	/**
	 * How many queries the discarded phase keeps going for, beyond {@value #DISCARDED_REPETITIONS}
	 * repetitions, before the clock starts.
	 * <p>
	 * <b>Why a count and not just the repetitions.</b> Two repetitions of a ten-thousand-query batch are
	 * twenty thousand iterations, which is not enough for the just-in-time compiler to reach its top tier -
	 * and the result was unmistakable: the ten-thousand-query row of the {@link #dispatchFloor()} came out
	 * at half a millisecond while the hundred-thousand-query row of the same floor came out at a seventh of
	 * that, i.e. the small batch was reporting the compiler rather than the work. Warming to a fixed query
	 * count instead makes every batch size measure the same compiled code, which is the only way the rows
	 * of one answerer can be read against each other.
	 */
	private static final long WARM_UP_QUERIES = 200_000;

	/**
	 * How long the extra warm-up may take. It exists for answerers whose queries cost microseconds; for one
	 * that takes milliseconds per query, {@value #DISCARDED_REPETITIONS} repetitions have already run for
	 * far longer than this and the compiler has long since had its iterations - so this stops a router from
	 * spending minutes here to reach a query count that means nothing to it.
	 */
	private static final double WARM_UP_SECONDS = 1.0;

	/**
	 * What one batch size on one thread count cost, over all the repetitions that were run.
	 *
	 * @param threads how many threads answered
	 * @param queries how many queries one repetition answered in total, over all threads
	 * @param seconds the wall-clock time of each repetition, in the order they ran
	 */
	public record Result(int threads, long queries, List<Double> seconds) {

		public Result {
			if (seconds.isEmpty()) {
				throw new IllegalArgumentException("A measurement with no repetition is not one.");
			}
			seconds = List.copyOf(seconds);
		}

		public int repetitions() {
			return this.seconds.size();
		}

		public double meanSeconds() {
			return this.seconds.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
		}

		/**
		 * The middle repetition, which is the one to quote. A mean over a handful of repetitions of a
		 * sub-millisecond batch is pulled around by any one of them that was interrupted.
		 */
		public double medianSeconds() {
			List<Double> sorted = this.seconds.stream().sorted().toList();
			int middle = sorted.size() / 2;
			return sorted.size() % 2 == 1
				? sorted.get(middle)
				: (sorted.get(middle - 1) + sorted.get(middle)) / 2;
		}

		public double minSeconds() {
			return this.seconds.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
		}

		public double maxSeconds() {
			return this.seconds.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
		}

		/**
		 * @return the cost of one query at the median repetition [us], i.e. wall-clock time divided by the
		 * queries answered in it - so on eight threads this is an eighth of what one query occupies a
		 * thread for, which is the point of measuring it
		 */
		public double microsecondsPerQuery() {
			return medianSeconds() * 1e6 / this.queries;
		}

		public double queriesPerSecond() {
			return this.queries / medianSeconds();
		}
	}

	private ParallelQueryBenchmark() {
	}

	/**
	 * Answers {@code queries} queries on {@code threads} threads, {@code repetitions} times.
	 *
	 * @param pool the pairs to ask about, cycled when there are fewer than the batch needs. Never empty.
	 * @param threads how many threads to use. One still goes through the executor, so that the one-thread
	 * row carries the same dispatch cost as the others and the speedup column is a ratio of like for like.
	 * @param queries how many queries one repetition answers in total. Split as evenly as the thread count
	 * divides it; the remainder goes to the first threads, so the total is exactly what was asked for.
	 * @param repetitions how many times to repeat, after {@value #DISCARDED_REPETITIONS} discarded ones
	 * @param answerer makes the answerer of one thread, given its index in {@code [0, threads)}. Called
	 * once per thread <em>per repetition</em>, always before the clock starts - so building a router's tree
	 * is not part of the measurement, and a stateful answerer cannot carry a warm tree from one repetition
	 * into the next. Keep whatever is expensive and shareable outside the function: see
	 * {@link RoutingBaseline#graph(org.matsim.api.core.v01.network.Network)}.
	 */
	public static Result run(List<TravelTimeQuery> pool, int threads, long queries, int repetitions,
		IntFunction<ToIntBiFunction<Node, Node>> answerer) {

		if (pool.isEmpty()) {
			throw new IllegalArgumentException("A benchmark needs at least one pair to ask about.");
		}
		if (threads < 1) {
			throw new IllegalArgumentException("A benchmark needs at least one thread, not " + threads + ".");
		}
		if (queries < threads) {
			throw new IllegalArgumentException("A batch of " + queries + " queries cannot be split over "
				+ threads + " threads. Drop the thread count or raise the batch.");
		}
		if (repetitions < 1) {
			throw new IllegalArgumentException("A measurement needs at least one repetition, not "
				+ repetitions + ".");
		}

		ExecutorService executor = Executors.newFixedThreadPool(threads);
		try {
			List<Double> measured = new ArrayList<>(repetitions);
			long checksum = 0;

			long discarded = 0;
			long warmUpStartedAt = System.nanoTime();

			for (int repetition = 0; measured.size() < repetitions; repetition++) {

				// new answerers for every repetition, and built BEFORE the clock starts. An answerer may
				// be stateful - RoutingBaseline.byOrigin remembers the origin of the last query - and
				// reusing one would let the second repetition find the first repetition's tree already
				// calculated, which measures lookups and reports them as searches.
				List<Callable<Long>> tasks = tasks(pool, threads, queries, answerer);

				long startedAt = System.nanoTime();
				checksum += invoke(executor, tasks);
				double seconds = (System.nanoTime() - startedAt) / 1e9;

				if (repetition < DISCARDED_REPETITIONS || stillWarmingUp(discarded, warmUpStartedAt)) {
					discarded += queries;
				} else {
					measured.add(seconds);
				}
			}

			log.debug("Discarded {} queries of warm-up in {} s before the clock started.", discarded,
				milliseconds((System.nanoTime() - warmUpStartedAt) / 1e9));

			Result result = new Result(threads, queries, measured);
			log.info("{} queries on {} thread(s): {} ms median, {} to {} over {} repetitions, {} us per query "
					+ "(checksum {}, which is only here to keep the compiler from optimising the benchmark "
					+ "away).", queries, threads, milliseconds(result.medianSeconds()),
				milliseconds(result.minSeconds()), milliseconds(result.maxSeconds()), result.repetitions(),
				String.format(Locale.ROOT, "%.4f", result.microsecondsPerQuery()), checksum);
			return result;

		} finally {
			executor.shutdown();
		}
	}

	/**
	 * Whether the discarded phase should run another repetition: when it has neither answered
	 * {@value #WARM_UP_QUERIES} queries nor spent {@value #WARM_UP_SECONDS} second on trying.
	 */
	private static boolean stillWarmingUp(long discarded, long startedAt) {
		return discarded < WARM_UP_QUERIES && (System.nanoTime() - startedAt) / 1e9 < WARM_UP_SECONDS;
	}

	/**
	 * An answerer that answers nothing, for measuring what the harness itself costs.
	 * <p>
	 * The row it produces is the floor of every other row at the same batch size and thread count: the
	 * loop, the thread pool and the executor's hand-off, with no memory touched. At ten thousand queries
	 * that is a real share of the total, and a reader who cannot see it has to take the claim on trust.
	 */
	public static IntFunction<ToIntBiFunction<Node, Node>> dispatchFloor() {
		return thread -> (from, to) -> 1;
	}

	/**
	 * One task per thread, each walking its own stretch of the pool.
	 * <p>
	 * Built fresh for every repetition, outside the timed section, so that a repetition measures the queries
	 * and not the construction of the closures - and every repetition asks exactly the same questions in
	 * the same order from the same starting state, which is what makes the spread between them a property
	 * of the machine rather than of the sample or of what the previous repetition left behind.
	 */
	private static List<Callable<Long>> tasks(List<TravelTimeQuery> pool, int threads, long queries,
		IntFunction<ToIntBiFunction<Node, Node>> answerer) {

		List<Callable<Long>> tasks = new ArrayList<>(threads);
		long each = queries / threads;
		long remainder = queries % threads;

		for (int thread = 0; thread < threads; thread++) {

			ToIntBiFunction<Node, Node> answer = answerer.apply(thread);
			long count = each + (thread < remainder ? 1 : 0);

			// its own offset into the pool, so that the threads are not reading the same entries in
			// lock-step. The offset is deterministic, so two runs ask the same questions.
			int offset = (int) ((long) thread * pool.size() / threads);

			tasks.add(() -> {
				long checksum = 0;
				int index = offset;
				for (long answered = 0; answered < count; answered++) {
					TravelTimeQuery query = pool.get(index);
					checksum += answer.applyAsInt(query.fromNode(), query.toNode());
					index = index + 1 == pool.size() ? 0 : index + 1;
				}
				return checksum;
			});
		}
		return tasks;
	}

	private static long invoke(ExecutorService executor, List<Callable<Long>> tasks) {
		try {
			long checksum = 0;
			for (Future<Long> future : executor.invokeAll(tasks)) {
				checksum += future.get();
			}
			return checksum;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("The benchmark was interrupted.", e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("A benchmark thread failed, so the measurement is void.", e);
		}
	}

	private static String milliseconds(double seconds) {
		return String.format(Locale.ROOT, "%.3f", seconds * 1000);
	}
}
