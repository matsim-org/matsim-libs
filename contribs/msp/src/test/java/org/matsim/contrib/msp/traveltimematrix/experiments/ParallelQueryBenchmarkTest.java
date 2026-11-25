package org.matsim.contrib.msp.traveltimematrix.experiments;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.msp.traveltimematrix.TestNetworks;
import org.matsim.contrib.msp.traveltimematrix.evaluation.ODSample;
import org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeQuery;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToIntBiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the harness, not the machine it runs on: that the batch is split over the threads without a query
 * going missing or being answered twice as often as it should, that every thread gets an answerer of its
 * own, and that the repetitions are all reported.
 * <p>
 * No duration is asserted anywhere, for the reason {@link QueryBenchmarkTest} gives: a test that expects
 * eight threads to be faster than one fails on a loaded continuous-integration machine with a single core
 * to spare, and the durations are the measurement rather than the contract.
 */
class ParallelQueryBenchmarkTest {

	private final Network network = TestNetworks.grid(4);

	@Test
	void theBatchIsSplitOverTheThreadsAndEveryQueryIsAnswered() {

		AtomicLong answered = new AtomicLong();
		ParallelQueryBenchmark.Result result = ParallelQueryBenchmark.run(pool(), 4, 1000, 3,
			thread -> counting(answered));

		assertEquals(1000, result.queries(), "the row says what was asked for");
		assertEquals(4, result.threads());
		assertEquals(3, result.repetitions());

		// every repetition answers the whole batch, the discarded ones included, so the total is a whole
		// number of batches and at least the three that were timed
		assertEquals(0, answered.get() % 1000, "no repetition answered a partial batch");
		assertTrue(answered.get() >= 4 * 1000, "the timed repetitions plus a warm-up (answered "
			+ answered.get() + ")");
	}

	@Test
	void theWarmUpIsAQueryCountAndNotARepetitionCount() {

		// the bug this guards against: two repetitions of a small batch are not enough iterations for the
		// just-in-time compiler, and the small-batch row then measured the compiler instead of the work -
		// it came out slower than the row of a batch ten times its size
		AtomicLong small = new AtomicLong();
		ParallelQueryBenchmark.run(pool(), 1, 100, 1, thread -> counting(small));

		assertTrue(small.get() > 100 * 100, "a batch of 100 is warmed up far past two repetitions of it "
			+ "(answered " + small.get() + ")");
	}

	@Test
	void aBatchTheThreadCountDoesNotDivideStillAddsUpExactly() {

		// 1001 over 4 threads is 251 + 250 + 250 + 250: the remainder goes to the first threads rather
		// than being rounded away, so a table's query count is the number that was answered
		AtomicLong answered = new AtomicLong();
		ParallelQueryBenchmark.Result result = ParallelQueryBenchmark.run(pool(), 4, 1001, 1,
			thread -> counting(answered));

		assertEquals(1001, result.queries());
		assertEquals(0, answered.get() % 1001, "every repetition answered the batch exactly, remainder "
			+ "included, so nothing was rounded away");
	}

	@Test
	void everyThreadGetsItsOwnAnswerer() {

		// the reason the factory takes an index at all: a LeastCostPathTree cannot be shared between
		// threads, so a benchmark that handed one answerer to all of them would measure corruption
		Set<Integer> indices = ConcurrentHashMap.newKeySet();
		ParallelQueryBenchmark.run(pool(), 4, 400, 1, thread -> {
			indices.add(thread);
			return (from, to) -> 1;
		});

		assertEquals(Set.of(0, 1, 2, 3), indices, "one answerer per thread, each told which thread it is");
	}

	@Test
	void theThreadsStartAtDifferentPlacesInThePool() {

		// what keeps eight threads from reading the same cache lines in lock-step and reporting a warm
		// cache as a matrix: on a pool of 16 pairs, four threads asking 4 queries each between them cover
		// the whole pool rather than its first quarter four times over
		Set<Node> origins = ConcurrentHashMap.newKeySet();
		List<TravelTimeQuery> pool = pool();

		ParallelQueryBenchmark.run(pool, 4, 4, 1, thread -> (from, to) -> {
			origins.add(from);
			return 1;
		});

		assertTrue(origins.size() > 1, "four threads asking one query each did not all ask the same one "
			+ "(pool of " + pool.size() + " pairs, " + origins.size() + " distinct origins seen)");
	}

	@Test
	void everyRepetitionIsReportedAndTheMedianLiesBetweenTheExtremes() {

		ParallelQueryBenchmark.Result result = ParallelQueryBenchmark.run(pool(), 2, 200, 7,
			thread -> (from, to) -> 1);

		assertEquals(7, result.seconds().size(), "the spread is reported, not averaged away");
		assertTrue(result.minSeconds() <= result.medianSeconds());
		assertTrue(result.medianSeconds() <= result.maxSeconds());
		assertTrue(result.microsecondsPerQuery() > 0);
		assertTrue(result.queriesPerSecond() > 0);
	}

	@Test
	void theDispatchFloorAnswersTheSameBatchWithoutDoingAnyWork() {

		// the row that says how much of a small batch is the harness rather than the matrix
		ParallelQueryBenchmark.Result result = ParallelQueryBenchmark.run(pool(), 4, 10_000, 3,
			ParallelQueryBenchmark.dispatchFloor());

		assertEquals(10_000, result.queries());
		assertTrue(result.medianSeconds() >= 0);
	}

	@Test
	void aBatchSmallerThanTheThreadCountIsRefused() {

		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> ParallelQueryBenchmark.run(pool(), 8, 4, 1, thread -> (from, to) -> 1));
		assertTrue(thrown.getMessage().contains("cannot be split"));
	}

	@Test
	void anEmptyPoolIsRefused() {
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> ParallelQueryBenchmark.run(List.of(), 2, 100, 1, thread -> (from, to) -> 1));
		assertTrue(thrown.getMessage().contains("at least one pair"));
	}

	@Test
	void aMeasurementWithNoRepetitionIsRefused() {
		assertThrows(IllegalArgumentException.class,
			() -> ParallelQueryBenchmark.run(pool(), 2, 100, 0, thread -> (from, to) -> 1));
	}

	private List<TravelTimeQuery> pool() {
		return ODSample.uniform(this.network, 4, 4, new double[]{0}, 1);
	}

	private static ToIntBiFunction<Node, Node> counting(AtomicLong answered) {
		return (from, to) -> {
			answered.incrementAndGet();
			return 1;
		};
	}
}
