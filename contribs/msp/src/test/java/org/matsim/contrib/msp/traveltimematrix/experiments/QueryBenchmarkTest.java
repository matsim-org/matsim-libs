package org.matsim.contrib.msp.traveltimematrix.experiments;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.msp.traveltimematrix.TestNetworks;
import org.matsim.contrib.msp.traveltimematrix.evaluation.ODSample;
import org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeQuery;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToIntBiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the benchmark, not the thing being benchmarked: that every query is asked, that the ladder is
 * reported at the counts it was given, and that the wall-clock budget ends a run which would otherwise
 * take longer than anyone will wait.
 * <p>
 * What is deliberately <b>not</b> asserted is any duration. A test that expects a lookup to take under a
 * microsecond fails on a loaded continuous-integration machine and tells nobody anything; what matters
 * is that the counts and the bookkeeping are right, and the durations are the measurement.
 */
class QueryBenchmarkTest {

	private final Network network = TestNetworks.grid(4);

	@Test
	void itAnswersExactlyTheNumberOfQueriesAskedFor() {

		AtomicLong asked = new AtomicLong();
		QueryBenchmark.Result result = QueryBenchmark.run(pool(), 5000,
			new long[]{1000, 2000}, 0, counting(asked));

		assertEquals(5000, result.queries(), "the target is met exactly, not rounded up to a whole pass of the pool");
		assertFalse(result.stoppedOnBudget());
		// plus the warm-up, which is a tenth of the target and whose timing is thrown away
		assertEquals(5000 + 500, asked.get(), "the warm-up is answered too, it is just not timed");
	}

	@Test
	void theLadderIsReportedAtTheCountsItWasGiven() {

		QueryBenchmark.Result result = QueryBenchmark.run(pool(), 10000,
			new long[]{100, 1000, 10000}, 0, (from, to) -> 1);

		assertEquals(List.of(100L, 1000L, 10000L),
			result.checkpoints().stream().map(QueryBenchmark.Checkpoint::queries).toList());
		result.checkpoints().forEach(checkpoint -> assertTrue(checkpoint.seconds() >= 0));
	}

	@Test
	void aRungAboveTheTargetDoesNotAppearButTheFinalCountAlwaysDoes() {

		// what happens to the 100 million rung of a configuration that only reaches 10 million: the row
		// is absent rather than present with a made-up number. The count it did reach is always the last
		// checkpoint, so every answerer has at least one row however slow it is.
		QueryBenchmark.Result result = QueryBenchmark.run(pool(), 2000,
			new long[]{1000, 1000000}, 0, (from, to) -> 1);

		assertEquals(List.of(1000L, 2000L), result.checkpoints().stream()
			.map(QueryBenchmark.Checkpoint::queries).toList(),
			"the 1000 rung, then where it actually stopped - and not the million it never reached");
		assertEquals(2000, result.queries());
	}

	@Test
	void somethingTooSlowToReachTheFirstRungStillGetsAMeasurement() {

		// the direct-routing baseline: 2.6 ms per query means it does not see 10,000 queries in a
		// minute, and a benchmark that produces no row at all is worse than useless in a cost table
		QueryBenchmark.Result result = QueryBenchmark.run(pool(), 1_000_000, new long[]{10_000}, 0.3,
			(from, to) -> {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return 1;
			});

		assertTrue(result.stoppedOnBudget());
		assertEquals(1, result.checkpoints().size(), "exactly one: where it got to");
		assertEquals(result.queries(), result.checkpoints().get(0).queries());
		assertTrue(result.checkpoints().get(0).microsecondsPerQuery() > 0);
	}

	@Test
	void theWallClockBudgetEndsARunThatWouldNotFinish() {

		// one millisecond per query against a target of a million: without a budget this test would run
		// for a quarter of an hour, which is exactly the situation the direct-routing baseline is in
		QueryBenchmark.Result result = QueryBenchmark.run(pool(), 1_000_000,
			new long[]{64, 1_000_000}, 0.3, (from, to) -> {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return 1;
			});

		assertTrue(result.stoppedOnBudget(), "the budget, not the target, is what ended it");
		assertTrue(result.queries() < 1_000_000);
		assertTrue(result.seconds() >= 0.3, "it ran until the budget was spent");
		assertTrue(result.microsecondsPerQuery() > 0);
		assertTrue(result.checkpoints().stream().noneMatch(checkpoint -> checkpoint.queries() == 1_000_000),
			"a rung it never reached is not in the result");
	}

	@Test
	void theWarmUpCannotSpendTheWholeBudget() {

		// the bug this guards against: 10,000 warm-up queries at a millisecond each is ten seconds, so a
		// benchmark of something slow used to burn its whole budget before the clock started and return
		// no checkpoint at all
		AtomicLong asked = new AtomicLong();
		long startedAt = System.nanoTime();

		QueryBenchmark.Result result = QueryBenchmark.run(pool(), 1_000_000, new long[]{64}, 0.5,
			(from, to) -> {
				asked.incrementAndGet();
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return 1;
			});

		double seconds = (System.nanoTime() - startedAt) / 1e9;
		assertTrue(seconds < 3, "the warm-up plus the budget stayed near the budget, not ten times it "
			+ "(took " + seconds + " s)");
		assertFalse(result.checkpoints().isEmpty(), "and there was budget left to reach the first rung");
	}

	@Test
	void aPoolSmallerThanTheTargetIsCycled() {

		List<TravelTimeQuery> pool = pool().subList(0, 3);
		AtomicLong asked = new AtomicLong();

		QueryBenchmark.Result result = QueryBenchmark.run(pool, 100, new long[]{100}, 0, counting(asked));

		assertEquals(100, result.queries(), "three pairs answer a hundred queries by going round");
	}

	@Test
	void anEmptyPoolIsRefused() {
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> QueryBenchmark.run(List.of(), 100, new long[]{100}, 0, (from, to) -> 1));
		assertTrue(thrown.getMessage().contains("at least one pair"));
	}

	private List<TravelTimeQuery> pool() {
		return ODSample.uniform(this.network, 4, 4, new double[]{0}, 1);
	}

	private static ToIntBiFunction<Node, Node> counting(AtomicLong asked) {
		return (from, to) -> {
			asked.incrementAndGet();
			return 1;
		};
	}
}
