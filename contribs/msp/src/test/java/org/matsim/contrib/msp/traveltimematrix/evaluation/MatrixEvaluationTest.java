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
import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the comparison itself: that a variant's error against a reference comes out as the error it was
 * built to have, that one run reports every variant against every reference, and that the two tables say
 * so.
 * <p>
 * The variants here are not matrices but one-line lambdas over a known travel time - "always 10%
 * slower", "always right" - because what is under test is the subtraction and the grouping, not the
 * matrix. The matrix is tested where it lives.
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

	private static final String FREE_SPEED_ROUTING = "FREE_SPEED_ROUTING";

	private Network network;
	private RoutedTravelTimes routed;
	private List<TravelTimeQuery> queries;

	@BeforeEach
	void routeAFewQueries() {
		this.network = TestNetworks.corridor();
		// both ends of the corridor, at two times of day, on an empty network: every travel time is then
		// 223 s, so an expected error is a number and not a sum
		this.routed = RoutedTravelTimes.compute(this.network, List.of(
				TravelTimeQuery.of(node("n1"), node("n4"), 3 * 3600.0),
				TravelTimeQuery.of(node("n1"), node("n4"), 8 * 3600.0)),
			new QSimFreeSpeedTravelTime(1), 1, 1);
		this.queries = this.routed.getQueries();
	}

	@Test
	void theRouterComparedAgainstItselfIsOffByTheRoundingAndNothingElse() {

		// the self-check of the grid: the same computation on both sides of the subtraction, so anything
		// but a rounding error means the comparison is not wired up the way it reads
		MatrixEvaluation evaluation = MatrixEvaluation.evaluate(this.queries,
			congestedRouting(), Map.of(MatrixEvaluation.CONGESTED_ROUTING, this.routed.asTimeDependentMatrix()),
			ODSample.DEFAULT_DISTANCE_BANDS, null);

		TravelTimeErrorStats stats = evaluation.getOverall(MatrixEvaluation.CONGESTED_ROUTING)
			.get(MatrixEvaluation.CONGESTED_ROUTING);
		assertEquals(2, stats.getCount(), "one pair at two departure times");
		assertTrue(Math.abs(stats.getBias()) <= 0.5, "a travel time is handed out as whole seconds");
	}

	@Test
	void aVariantThatIsTenPercentTooSlowIsReportedAsTenPercentTooSlow() {

		Map<String, TravelTimeMatrix> variants = new LinkedHashMap<>();
		variants.put("TOO_SLOW", scaled(1.1));
		variants.put("TOO_FAST", scaled(0.9));

		MatrixEvaluation evaluation = MatrixEvaluation.evaluate(this.queries, congestedRouting(), variants,
			ODSample.DEFAULT_DISTANCE_BANDS, null);

		// to within the rounding: a matrix answers whole seconds, so 10% of 223 s is 245 s and not 245.3
		TravelTimeErrorStats tooSlow = evaluation.getOverall(MatrixEvaluation.CONGESTED_ROUTING).get("TOO_SLOW");
		assertEquals(0.1, tooSlow.getRelativeBias(), ROUNDING);
		assertEquals(0.1, tooSlow.getMape(), ROUNDING);
		assertEquals(0.1 * CORRIDOR_END_TO_END, tooSlow.getBias(), 1);

		TravelTimeErrorStats tooFast = evaluation.getOverall(MatrixEvaluation.CONGESTED_ROUTING).get("TOO_FAST");
		assertEquals(-0.1, tooFast.getRelativeBias(), ROUNDING, "a bias is signed");
		assertEquals(0.1, tooFast.getMape(), ROUNDING, "a mean absolute error is not");
	}

	@Test
	void everyVariantIsReportedAgainstEveryReference() {

		// what the restructured evaluation is for: the same variant judged by two different notions of
		// "right" in one run. The network is empty, so here the two references happen to agree - what is
		// under test is that both of them are reported, over the same queries, with the same columns.
		Map<String, ToDoubleFunction<TravelTimeQuery>> references = new LinkedHashMap<>();
		references.put(FREE_SPEED_ROUTING, TravelTimeQuery::freeSpeedTravelTime);
		references.put(MatrixEvaluation.CONGESTED_ROUTING, timeDependent());

		MatrixEvaluation evaluation = MatrixEvaluation.evaluate(this.queries, references,
			Map.of("TOO_SLOW", scaled(1.1)), ODSample.DEFAULT_DISTANCE_BANDS, null);

		assertEquals(List.of(FREE_SPEED_ROUTING, MatrixEvaluation.CONGESTED_ROUTING),
			List.copyOf(evaluation.getReferences()), "in the order they were given");
		assertEquals(0.1, evaluation.getOverall(FREE_SPEED_ROUTING).get("TOO_SLOW").getRelativeBias(), ROUNDING);
		assertEquals(0.1, evaluation.getOverall(MatrixEvaluation.CONGESTED_ROUTING).get("TOO_SLOW")
			.getRelativeBias(), ROUNDING);

		assertThrows(IllegalArgumentException.class, () -> evaluation.getOverall("NO_SUCH_REFERENCE"));
	}

	@Test
	void theErrorIsGroupedByTimeOfDayAndByDistance() {

		MatrixEvaluation evaluation = MatrixEvaluation.evaluate(this.queries, congestedRouting(),
			Map.of("TOO_SLOW", scaled(1.1)), ODSample.DEFAULT_DISTANCE_BANDS, null);

		// one query per hour, and both of them in the 2 to 5 km band - the corridor is 3 km long
		String reference = MatrixEvaluation.CONGESTED_ROUTING;
		assertEquals(1, evaluation.getStats(reference, "TOO_SLOW", "hour", "3").getCount());
		assertEquals(1, evaluation.getStats(reference, "TOO_SLOW", "hour", "8").getCount());
		assertEquals(2, evaluation.getStats(reference, "TOO_SLOW", "distance_band", "2000-5000").getCount());

		assertThrows(IllegalArgumentException.class,
			() -> evaluation.getStats(reference, "TOO_SLOW", "hour", "17"), "nothing was evaluated at 17:00");
		assertThrows(IllegalArgumentException.class,
			() -> evaluation.getStats(reference, "NO_SUCH_VARIANT", "hour", "8"));
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
	void itWritesOneQueryRowPerVariantAndOneErrorRowPerReferenceVariantAndGroup() throws IOException {

		Path queryFile = Path.of(utils.getOutputDirectory(), "queries.csv");
		Path errors = Path.of(utils.getOutputDirectory(), "errors.csv");

		Map<String, ToDoubleFunction<TravelTimeQuery>> references = new LinkedHashMap<>();
		references.put(FREE_SPEED_ROUTING, TravelTimeQuery::freeSpeedTravelTime);
		references.put(MatrixEvaluation.CONGESTED_ROUTING, timeDependent());

		Map<String, TravelTimeMatrix> variants = new LinkedHashMap<>();
		variants.put("TOO_SLOW", scaled(1.1));
		variants.put("TOO_FAST", scaled(0.9));

		MatrixEvaluation evaluation = MatrixEvaluation.evaluate(this.queries, references, variants,
			ODSample.DEFAULT_DISTANCE_BANDS, queryFile.toString());
		evaluation.writeErrors(errors.toString());

		List<String> queryLines = Files.readAllLines(queryFile);
		assertEquals(1 + 2 * 2, queryLines.size(), "a header plus one pair times two departure times times "
			+ "two variants - a reference is a column here and not a row");
		assertEquals("from_node,to_node,departure_time,distance,tt_ref_free_speed_routing,"
			+ "tt_ref_congested_routing,variant,tt_estimate", queryLines.get(0));
		assertTrue(queryLines.stream().anyMatch(line -> line.contains("n1,n4") && line.contains("TOO_SLOW")));

		List<String> errorLines = Files.readAllLines(errors);
		// per reference and variant: all, two hours, one distance band
		assertEquals(1 + 2 * 2 * 4, errorLines.size());
		assertTrue(errorLines.get(0).startsWith("reference,variant,group,group_value,n,"));
		assertTrue(errorLines.stream().anyMatch(line ->
			line.startsWith(MatrixEvaluation.CONGESTED_ROUTING + ",TOO_SLOW,all,all,2,")));
		assertTrue(errorLines.stream().anyMatch(line -> line.startsWith(FREE_SPEED_ROUTING + ",TOO_FAST,all,all,2,")));
	}

	@Test
	void aReferenceThatIsUnknownForAQueryIsRefusedRatherThanSkipped() {

		// the state a generated pair is in before anything determined what it should have taken: a
		// silently skipped query would shrink one row of the grid and not the others, which makes two
		// cells quietly incomparable
		MatrixEvaluation.evaluate(this.queries, congestedRouting(), Map.of("TOO_SLOW", scaled(1.1)),
			ODSample.DEFAULT_DISTANCE_BANDS, null);

		Map<String, ToDoubleFunction<TravelTimeQuery>> experienced =
			Map.of("EXPERIENCED", TravelTimeQuery::observedTravelTime);
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> MatrixEvaluation.evaluate(this.queries, experienced, Map.of("TOO_SLOW", scaled(1.1)),
				ODSample.DEFAULT_DISTANCE_BANDS, null));
		assertTrue(thrown.getMessage().contains("reference 'EXPERIENCED'"), thrown.getMessage());

		assertThrows(IllegalArgumentException.class, () -> MatrixEvaluation.evaluate(this.queries, Map.of(),
			Map.of("TOO_SLOW", scaled(1.1)), ODSample.DEFAULT_DISTANCE_BANDS, null),
			"and no reference at all is not an evaluation");
	}

	@Test
	void aQueryWhosePathLengthIsUnknownGetsAGroupOfItsOwn() {

		// an experienced or API travel time can arrive without a path length. It must neither disappear
		// from the table nor be guessed into a band it may not belong to.
		List<TravelTimeQuery> withoutDistance = List.of(
			new TravelTimeQuery(node("n1"), node("n4"), 8 * 3600.0, 300, Double.NaN, Double.NaN));

		MatrixEvaluation evaluation = MatrixEvaluation.evaluate(withoutDistance,
			Map.of("EXPERIENCED", TravelTimeQuery::observedTravelTime), Map.of("TOO_SLOW", scaled(1.1)),
			ODSample.DEFAULT_DISTANCE_BANDS, null);

		assertEquals(1, evaluation.getStats("EXPERIENCED", "TOO_SLOW", "distance_band", "unknown").getCount());
	}

	/**
	 * The time-dependent optimum as the reference, under the name the command reports it with.
	 */
	private Map<String, ToDoubleFunction<TravelTimeQuery>> congestedRouting() {
		return Map.of(MatrixEvaluation.CONGESTED_ROUTING, timeDependent());
	}

	private ToDoubleFunction<TravelTimeQuery> timeDependent() {
		return query -> this.routed.getTimeDependentTravelTime(query.fromNode(), query.toNode(),
			query.departureTime());
	}

	/**
	 * A variant that answers a fixed multiple of the routed travel time, which is the error the test then
	 * expects to be reported.
	 */
	private TravelTimeMatrix scaled(double factor) {
		TravelTimeMatrix exact = this.routed.asTimeDependentMatrix();
		return (fromNode, toNode, departureTime) ->
			(int) Math.round(factor * exact.getTravelTime(fromNode, toNode, departureTime));
	}

	private Node node(String id) {
		return this.network.getNodes().get(Id.createNodeId(id));
	}
}
