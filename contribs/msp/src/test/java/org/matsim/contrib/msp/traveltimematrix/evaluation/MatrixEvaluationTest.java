package org.matsim.contrib.msp.traveltimematrix.evaluation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.contrib.msp.traveltimematrix.TestNetworks;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the comparison itself: that a variant's error against the reference comes out as the error it
 * was built to have, and that the two tables say so.
 * <p>
 * The variants here are not matrices but one-line lambdas over the reference - "always 10% slower",
 * "always right" - because what is under test is the subtraction and the grouping, not the matrix. The
 * matrix is tested where it lives.
 */
class MatrixEvaluationTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	private static final double CORRIDOR_END_TO_END = 223;

	/**
	 * Half a second on the travel time of the corridor, as a share: the accuracy a relative error can
	 * have at all when both sides are whole seconds.
	 */
	private static final double ROUNDING = 0.5 / CORRIDOR_END_TO_END;

	private Network network;
	private RoutedTravelTimes reference;

	@BeforeEach
	void computeAReference() {
		this.network = TestNetworks.corridor();
		// both ends of the corridor, at two times of day, on an empty network: every reference travel
		// time is then 223 s, so an expected error is a number and not a sum
		ODSample sample = ODSample.of(Map.of(node("n1"), List.of(node("n4"))),
			new double[]{3 * 3600.0, 8 * 3600.0});
		this.reference = RoutedTravelTimes.compute(this.network, sample, new QSimFreeSpeedTravelTime(1), 1, 1);
	}

	@Test
	void theReferenceComparedAgainstItselfIsOffByTheRoundingAndNothingElse() {

		MatrixEvaluation evaluation = MatrixEvaluation.evaluate(this.reference,
			Map.of(MatrixEvaluation.REFERENCE, this.reference.asTimeDependentMatrix()),
			ODSample.DEFAULT_DISTANCE_BANDS, null);

		TravelTimeErrorStats stats = evaluation.getOverall().get(MatrixEvaluation.REFERENCE);
		assertEquals(2, stats.getCount(), "one pair at two departure times");
		assertTrue(Math.abs(stats.getBias()) <= 0.5, "a travel time is handed out as whole seconds");
	}

	@Test
	void aVariantThatIsTenPercentTooSlowIsReportedAsTenPercentTooSlow() {

		Map<String, TravelTimeMatrix> variants = new LinkedHashMap<>();
		variants.put("TOO_SLOW", scaled(1.1));
		variants.put("TOO_FAST", scaled(0.9));

		MatrixEvaluation evaluation = MatrixEvaluation.evaluate(this.reference, variants,
			ODSample.DEFAULT_DISTANCE_BANDS, null);

		// to within the rounding: a matrix answers whole seconds, so 10% of 223 s is 245 s and not 245.3
		TravelTimeErrorStats tooSlow = evaluation.getOverall().get("TOO_SLOW");
		assertEquals(0.1, tooSlow.getRelativeBias(), ROUNDING);
		assertEquals(0.1, tooSlow.getMape(), ROUNDING);
		assertEquals(0.1 * CORRIDOR_END_TO_END, tooSlow.getBias(), 1);

		TravelTimeErrorStats tooFast = evaluation.getOverall().get("TOO_FAST");
		assertEquals(-0.1, tooFast.getRelativeBias(), ROUNDING, "a bias is signed");
		assertEquals(0.1, tooFast.getMape(), ROUNDING, "a mean absolute error is not");
	}

	@Test
	void theErrorIsGroupedByTimeOfDayAndByDistance() {

		MatrixEvaluation evaluation = MatrixEvaluation.evaluate(this.reference,
			Map.of("TOO_SLOW", scaled(1.1)), ODSample.DEFAULT_DISTANCE_BANDS, null);

		// one query per hour, and both of them in the 2 to 5 km band - the corridor is 3 km long
		assertEquals(1, evaluation.getStats("TOO_SLOW", "hour", "3").getCount());
		assertEquals(1, evaluation.getStats("TOO_SLOW", "hour", "8").getCount());
		assertEquals(2, evaluation.getStats("TOO_SLOW", "distance_band", "2000-5000").getCount());

		assertThrows(IllegalArgumentException.class, () -> evaluation.getStats("TOO_SLOW", "hour", "17"),
			"nothing was evaluated at 17:00");
		assertThrows(IllegalArgumentException.class, () -> evaluation.getStats("NO_SUCH_VARIANT", "hour", "8"));
	}

	@Test
	void theDistanceBandsLabelTheRowsOfTheTable() {
		double[] bands = {0, 2000, 5000};
		assertEquals("0-2000", MatrixEvaluation.bandOf(0, bands));
		assertEquals("0-2000", MatrixEvaluation.bandOf(1999, bands));
		assertEquals("2000-5000", MatrixEvaluation.bandOf(2000, bands));
		assertEquals("5000+", MatrixEvaluation.bandOf(123456, bands));
		assertEquals("<1000", MatrixEvaluation.bandOf(500, new double[]{1000, 2000}),
			"a distance below the first band is labelled as such rather than squeezed into it");
	}

	@Test
	void itWritesOneQueryRowPerVariantAndOneErrorRowPerGroup() throws IOException {

		Path queries = Path.of(utils.getOutputDirectory(), "queries.csv");
		Path errors = Path.of(utils.getOutputDirectory(), "errors.csv");

		Map<String, TravelTimeMatrix> variants = new LinkedHashMap<>();
		variants.put("TOO_SLOW", scaled(1.1));
		variants.put("TOO_FAST", scaled(0.9));

		MatrixEvaluation evaluation = MatrixEvaluation.evaluate(this.reference, variants,
			ODSample.DEFAULT_DISTANCE_BANDS, queries.toString());
		evaluation.writeErrors(errors.toString());

		List<String> queryLines = Files.readAllLines(queries);
		assertEquals(1 + 2 * 2, queryLines.size(), "a header plus one pair times two departure times times "
			+ "two variants");
		assertTrue(queryLines.get(0).startsWith("from_node,to_node,departure_time"));
		assertTrue(queryLines.stream().anyMatch(line -> line.contains("n1,n4") && line.contains("TOO_SLOW")));

		List<String> errorLines = Files.readAllLines(errors);
		// per variant: all, two hours, one distance band
		assertEquals(1 + 2 * 4, errorLines.size());
		assertTrue(errorLines.get(0).startsWith("variant,group,group_value,n,"));
		assertTrue(errorLines.stream().anyMatch(line -> line.startsWith("TOO_SLOW,all,all,2,")));
	}

	/**
	 * A variant that answers a fixed multiple of the reference, which is the error the test then expects
	 * to be reported.
	 */
	private TravelTimeMatrix scaled(double factor) {
		TravelTimeMatrix exact = this.reference.asTimeDependentMatrix();
		return (fromNode, toNode, departureTime) ->
			(int) Math.round(factor * exact.getTravelTime(fromNode, toNode, departureTime));
	}

	private Node node(String id) {
		return this.network.getNodes().get(Id.createNodeId(id));
	}
}
