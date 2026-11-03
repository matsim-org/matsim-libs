package org.matsim.contrib.msp.traveltimematrix.evaluation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the error metrics on numbers whose answer can be worked out in one line, including the two cases
 * that are easy to get wrong: an error that cancels out in the bias but not in the MAPE, and a reference
 * travel time of zero, which has no percentage error at all.
 */
class TravelTimeErrorStatsTest {

	@Test
	void aSystematicOverestimateShowsUpInBothTheBiasAndTheMape() {

		TravelTimeErrorStats stats = new TravelTimeErrorStats();
		stats.add(100, 110);
		stats.add(200, 220);

		assertEquals(2, stats.getCount());
		assertEquals(150, stats.getMeanReference(), 1e-9);
		assertEquals(165, stats.getMeanEstimate(), 1e-9);
		assertEquals(15, stats.getBias(), 1e-9, "10 s and 20 s too slow");
		assertEquals(0.1, stats.getRelativeBias(), 1e-9);
		assertEquals(0.1, stats.getMape(), 1e-9);
		assertEquals(0.1, stats.getMaxAbsolutePercentageError(), 1e-9);
	}

	@Test
	void anErrorThatCancelsOutLeavesABiasOfZeroAndAMapeThatIsNot() {

		TravelTimeErrorStats stats = new TravelTimeErrorStats();
		stats.add(100, 120);
		stats.add(100, 80);

		assertEquals(0, stats.getBias(), 1e-9);
		assertEquals(0, stats.getRelativeBias(), 1e-9);
		assertEquals(0.2, stats.getMape(), 1e-9, "a scatter of 20% that averages to nothing");
		assertEquals(20, stats.getRmse(), 1e-9);
	}

	@Test
	void theRmseWeighsTheLargeErrors() {

		TravelTimeErrorStats scattered = new TravelTimeErrorStats();
		scattered.add(100, 100);
		scattered.add(100, 140);

		TravelTimeErrorStats even = new TravelTimeErrorStats();
		even.add(100, 120);
		even.add(100, 120);

		assertEquals(even.getBias(), scattered.getBias(), 1e-9, "the same mean error");
		assertTrue(scattered.getRmse() > even.getRmse(), "but one large error is worse than two small ones");
	}

	@Test
	void aReferenceOfZeroCountsInTheSecondsAndNotInThePercentages() {

		TravelTimeErrorStats stats = new TravelTimeErrorStats();
		stats.add(100, 110);
		stats.add(0, 0);

		assertEquals(2, stats.getCount());
		assertEquals(1, stats.getCountWithoutPercentage());
		assertEquals(5, stats.getBias(), 1e-9, "10 s over two queries");
		// not 5%: counting the pair of a node with itself as a perfect 0% would halve the reported error
		assertEquals(0.1, stats.getMape(), 1e-9);
		assertEquals(0.1, stats.getRelativeBias(), 1e-9);
	}

	@Test
	void anEmptyStatisticHasNoMeanRatherThanZero() {
		TravelTimeErrorStats stats = new TravelTimeErrorStats();
		assertEquals(0, stats.getCount());
		assertTrue(Double.isNaN(stats.getMape()), "a mean of nothing is not 0");
		assertTrue(Double.isNaN(stats.getBias()));
	}

	@Test
	void aRecordHasAValueForEveryColumnOfTheHeader() {
		TravelTimeErrorStats stats = new TravelTimeErrorStats();
		stats.add(100, 110);
		assertEquals(TravelTimeErrorStats.HEADER.size(), stats.toRecord().size());
	}
}
