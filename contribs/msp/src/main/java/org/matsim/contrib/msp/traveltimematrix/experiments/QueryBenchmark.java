package org.matsim.contrib.msp.traveltimematrix.experiments;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntBiFunction;

/**
 * Times how fast something answers origin-destination queries, at a ladder of query counts.
 * <p>
 * The ladder is the point. A single number is not trustworthy for something this fast: the first
 * thousand queries of a matrix measure the JIT compiler, the first million measure a cold cache, and the
 * rate only settles after that. Reporting the rate at 10k, 100k, 1M and 10M makes the settling visible
 * instead of hiding it in an average, and it is also the only honest way to put a 0.5 microsecond lookup
 * and a 16 millisecond shortest-path search in the same table - the fast one reaches the top of the
 * ladder and the slow one does not.
 * <p>
 * Which is what the <b>budget</b> is for: a run stops at the query target or the wall-clock budget,
 * whichever comes first, and says which. Timing routing to 10 million queries would take weeks, and a
 * benchmark that has to be killed produces no row at all.
 * <p>
 * Two things it does on purpose:
 * <ul>
 *     <li><b>A warm-up pass</b> before the clock starts, so the first checkpoint measures the same
 *     compiled code as the last.</li>
 *     <li><b>A checksum</b> of every answer, returned and logged. Nothing uses the value, which is
 *     exactly the problem: without it a just-in-time compiler is free to notice that the loop has no
 *     effect and delete it, and the benchmark then measures an empty loop.</li>
 * </ul>
 * The queries are cycled from a fixed pool in the order they were sampled, so a benchmark asks about
 * pairs all over the study area rather than walking one row of the matrix - a benchmark over one origin
 * would measure a warm cache line and not what an analysis does.
 */
public final class QueryBenchmark {

	private static final Logger log = LogManager.getLogger(QueryBenchmark.class);

	/**
	 * How often the wall clock is consulted. Reading the clock costs tens of nanoseconds against a
	 * lookup of a few hundred, so checking every query would be measuring the clock; checking every 64
	 * overshoots a budget by at most 64 queries, which matters for nothing in this ladder.
	 */
	private static final int BUDGET_CHECK_INTERVAL = 64;

	/**
	 * How many queries the warm-up answers before the clock starts.
	 */
	private static final int WARM_UP_QUERIES = 10_000;

	/**
	 * What share of the wall-clock budget the warm-up may spend. Without this cap a warm-up of 10,000
	 * queries at 16 ms each - which is one free-speed shortest-path search on a city network - would
	 * spend 160 seconds before the clock starts, and a benchmark given a 60 second budget would return
	 * no measurement at all.
	 */
	private static final double WARM_UP_BUDGET_SHARE = 0.1;

	/**
	 * The rate at one rung of the ladder.
	 *
	 * @param queries how many queries had been answered
	 * @param seconds how long they took
	 * @param microsecondsPerQuery the mean cost of one, which is what the table reports
	 */
	public record Checkpoint(long queries, double seconds, double microsecondsPerQuery) {
	}

	/**
	 * What a benchmark found.
	 *
	 * @param checkpoints one entry per rung that was reached, in order
	 * @param queries how many were answered in total
	 * @param seconds how long that took
	 * @param stoppedOnBudget whether the wall-clock budget ended it before the target was reached. The
	 * last checkpoint is then the count it actually got to rather than a rung of the ladder, so that
	 * something too slow to reach the first rung still has a measurement
	 */
	public record Result(List<Checkpoint> checkpoints, long queries, double seconds, boolean stoppedOnBudget) {

		/**
		 * @return the mean cost of a query over the whole run [us]
		 */
		public double microsecondsPerQuery() {
			return this.queries == 0 ? Double.NaN : this.seconds * 1e6 / this.queries;
		}
	}

	private QueryBenchmark() {
	}

	/**
	 * Answers queries from the pool until the target or the budget is reached.
	 *
	 * @param pool the pairs to ask about, cycled if there are fewer than the target. Never empty.
	 * @param target how many queries to answer at most
	 * @param checkpoints the rungs to report at, ascending. Rungs above {@code target} or beyond the
	 * budget simply do not appear in the result.
	 * @param budgetSeconds wall-clock budget, or a non-positive value for none
	 * @param answer what is under test: a travel time [s] for a pair. A matrix lookup, or a whole
	 * shortest-path search - this class cannot tell and that is the point of it taking a function.
	 */
	public static Result run(List<TravelTimeQuery> pool, long target, long[] checkpoints, double budgetSeconds,
		ToIntBiFunction<Node, Node> answer) {

		if (pool.isEmpty()) {
			throw new IllegalArgumentException("A benchmark needs at least one pair to ask about.");
		}

		warmUp(pool, target, budgetSeconds, answer);

		List<Checkpoint> reached = new ArrayList<>();
		long budgetNanos = budgetSeconds > 0 ? (long) (budgetSeconds * 1e9) : Long.MAX_VALUE;
		int nextCheckpoint = 0;

		long answered = 0;
		long checksum = 0;
		boolean stoppedOnBudget = false;

		long startedAt = System.nanoTime();
		long elapsed = 0;

		outer:
		while (answered < target) {
			for (TravelTimeQuery query : pool) {

				checksum += answer.applyAsInt(query.fromNode(), query.toNode());
				answered++;

				while (nextCheckpoint < checkpoints.length && answered == checkpoints[nextCheckpoint]) {
					elapsed = System.nanoTime() - startedAt;
					reached.add(checkpoint(answered, elapsed));
					nextCheckpoint++;
				}

				if (answered % BUDGET_CHECK_INTERVAL == 0 || answered >= target) {
					elapsed = System.nanoTime() - startedAt;
					if (elapsed >= budgetNanos) {
						stoppedOnBudget = answered < target;
						break outer;
					}
					if (answered >= target) {
						break outer;
					}
				}
			}
		}

		elapsed = System.nanoTime() - startedAt;
		double seconds = elapsed / 1e9;

		// a run that stopped early has no rung of its own - the direct-routing baseline does not reach
		// 10,000 queries in a minute - so the count it did reach becomes its last checkpoint. Without
		// this the slowest thing in the table is the one with no row in it.
		if (reached.isEmpty() || reached.get(reached.size() - 1).queries() != answered) {
			reached.add(checkpoint(answered, elapsed));
		}

		log.info("{} queries in {} s, {} us each{} (mean travel time {} s, which is only here to keep the compiler "
				+ "from optimising the benchmark away).", answered, String.format(Locale.ROOT, "%.2f", seconds),
			String.format(Locale.ROOT, "%.3f", seconds * 1e6 / Math.max(1, answered)),
			stoppedOnBudget ? " - stopped on the wall-clock budget before reaching " + target : "",
			checksum / Math.max(1, answered));

		return new Result(List.copyOf(reached), answered, seconds, stoppedOnBudget);
	}

	private static Checkpoint checkpoint(long queries, long nanos) {
		return new Checkpoint(queries, nanos / 1e9, nanos / 1000.0 / queries);
	}

	/**
	 * Answers a few queries whose timing is thrown away, so that the first rung of the ladder measures
	 * compiled code rather than the compiler.
	 * <p>
	 * Capped three ways, and all three are needed: by a fixed count, because that is all a just-in-time
	 * compiler needs; by a tenth of the target, so that a short benchmark is not mostly warm-up; and by
	 * {@value #WARM_UP_BUDGET_SHARE} of the wall-clock budget, because for something that takes
	 * milliseconds per query the fixed count alone would spend the entire budget here and leave nothing
	 * to measure.
	 */
	private static void warmUp(List<TravelTimeQuery> pool, long target, double budgetSeconds,
		ToIntBiFunction<Node, Node> answer) {

		long warmUp = Math.min(WARM_UP_QUERIES, Math.max(1, target / 10));
		long warmUpNanos = budgetSeconds > 0
			? (long) (budgetSeconds * WARM_UP_BUDGET_SHARE * 1e9)
			: Long.MAX_VALUE;

		long done = 0;
		long checksum = 0;
		long startedAt = System.nanoTime();

		outer:
		while (done < warmUp) {
			for (TravelTimeQuery query : pool) {
				checksum += answer.applyAsInt(query.fromNode(), query.toNode());
				done++;
				if (done >= warmUp || (done % BUDGET_CHECK_INTERVAL == 0
					&& System.nanoTime() - startedAt >= warmUpNanos)) {
					break outer;
				}
			}
		}

		log.debug("Warmed up on {} of {} queries in {} s (checksum {}).", done, warmUp,
			String.format(Locale.ROOT, "%.2f", (System.nanoTime() - startedAt) / 1e9), checksum);
	}
}
