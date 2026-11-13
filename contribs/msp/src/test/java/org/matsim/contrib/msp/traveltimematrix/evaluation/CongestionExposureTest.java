package org.matsim.contrib.msp.traveltimematrix.evaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.msp.traveltimematrix.TestNetworks;
import org.matsim.contrib.msp.traveltimematrix.TimeVaryingTravelTimeMatrix;
import org.matsim.contrib.msp.traveltimematrix.TravelTimeMatrixConfigGroup;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndex;
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
 * Tests the diagnostic that explains the error table: the congestion the queries carry against the
 * congestion the matrix applies to them.
 * <p>
 * Every number here is written down by hand, which is the point of the fixture: one pair of a known
 * free-speed travel time, a constant index of 20%, and references that are a fixed multiple of free
 * speed. So "the matrix applies twice the congestion these pairs carry" is an assertion on an exact
 * {@code 0.20} against an exact {@code 0.10}, and not on whatever a scenario happens to produce.
 * <p>
 * The case the class exists for is the last one: a reference with <em>no</em> excess travel time at all,
 * which is what a pair nobody drove looks like when the congested travel times come from a run. The
 * matrix still applies its index there, and that - not a miscalibrated index - is why a random sample
 * scores worse than using no congestion model.
 */
class CongestionExposureTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * The free-speed travel time the fake base matrix answers every pair with [s]. A round number so
	 * that the excess of a reference is a round number too.
	 */
	private static final double FREE_SPEED = 200;

	/**
	 * The index the matrix applies, at every time of day - so the applied index is this, exactly, and
	 * does not depend on where in the trip it is read.
	 */
	private static final double APPLIED = 0.20;

	private static final String CONGESTED_ROUTING = "CONGESTED_ROUTING";
	private static final String FREE_SPEED_ROUTING = "FREE_SPEED_ROUTING";

	private final Network network = TestNetworks.corridor();

	@Test
	void theIndexTheQueriesDeserveAgainstTheIndexTheyGot() {

		// the references carry 10% excess travel time; the matrix applies 20%. So the model puts twice as
		// much congestion into these pairs as there is in them, and the gap is ten percentage points of
		// their free-speed travel time
		CongestionExposure exposure = CongestionExposure.measure(queries(), reference(1.10), matrix(),
			ODSample.DEFAULT_DISTANCE_BANDS);

		assertEquals(0.10, exposure.getRealizedIndex(CONGESTED_ROUTING), 1e-9);
		assertEquals(APPLIED, exposure.getAppliedIndex(CONGESTED_ROUTING), 1e-9);
		assertEquals(0.0, exposure.getShareWithoutExcess(CONGESTED_ROUTING), 1e-9,
			"every one of these pairs was slowed down");
	}

	@Test
	void aSampleWhoseReferenceWasNeverSlowedDownIsTheWholeDiagnosis() {

		// what a generated pair looks like against travel times measured on a run: its links carried no
		// traffic, so the reference is free speed to the second and there is no excess for any index to
		// reproduce. The matrix applies 20% anyway, which is the error, and the share says it is not an
		// accident of a few pairs but the whole sample.
		CongestionExposure exposure = CongestionExposure.measure(queries(), reference(1.0), matrix(),
			ODSample.DEFAULT_DISTANCE_BANDS);

		assertEquals(0.0, exposure.getRealizedIndex(CONGESTED_ROUTING), 1e-9);
		assertEquals(APPLIED, exposure.getAppliedIndex(CONGESTED_ROUTING), 1e-9);
		assertEquals(1.0, exposure.getShareWithoutExcess(CONGESTED_ROUTING), 1e-9);
	}

	@Test
	void theFreeSpeedTravelTimeOfTheQueryWinsOverTheMatrixOwn() {

		// a routed query carries the exact free-speed travel time, and that is the denominator both
		// indices are shares of - so a matrix whose own free-speed answer is 10% off its discretisation
		// does not move either of them
		TravelTimeMatrix tooSlow = (from, to, time) -> (int) (1.1 * FREE_SPEED);
		TimeVaryingTravelTimeMatrix matrix = TimeVaryingTravelTimeMatrix.global(tooSlow,
			CongestionIndex.constant(APPLIED), config());

		CongestionExposure exposure = CongestionExposure.measure(routedQueries(), reference(1.10), matrix,
			ODSample.DEFAULT_DISTANCE_BANDS);

		assertEquals(0.10, exposure.getRealizedIndex(CONGESTED_ROUTING), 1e-9);
		assertEquals(APPLIED, exposure.getAppliedIndex(CONGESTED_ROUTING), 1e-9);
	}

	@Test
	void aPairWithNoFreeSpeedTravelTimeIsLeftOutRatherThanDividedBy() {

		// a node with itself, or a matrix that answers 0 s: there is nothing for an index to be a share
		// of, and both numbers would be 0/0. With every query in that state there is nothing to report,
		// and saying so beats a table of empty fields that reads like a measurement.
		TravelTimeMatrix zero = (from, to, time) -> 0;
		TimeVaryingTravelTimeMatrix matrix = TimeVaryingTravelTimeMatrix.global(zero,
			CongestionIndex.constant(APPLIED), config());

		assertThrows(IllegalArgumentException.class,
			() -> CongestionExposure.measure(queries(), reference(1.10), matrix, ODSample.DEFAULT_DISTANCE_BANDS),
			"not one query was left, so there is nothing to report rather than a NaN to misread");
	}

	@Test
	void theTableJoinsOntoTheErrorTableOfTheSameQueries() throws IOException {

		Map<String, ToDoubleFunction<TravelTimeQuery>> references = new LinkedHashMap<>();
		references.put(CONGESTED_ROUTING, query -> 1.10 * FREE_SPEED);
		references.put(FREE_SPEED_ROUTING, query -> FREE_SPEED);

		Path output = Path.of(utils.getOutputDirectory(), "congestion_exposure.csv");
		CongestionExposure.measure(routedQueries(), references, matrix(), ODSample.DEFAULT_DISTANCE_BANDS)
			.write(output.toString());

		List<String> rows = Files.readAllLines(output);
		assertEquals("reference,group,group_value,n,share_without_excess,mean_free_speed_travel_time,"
			+ "mean_reference_travel_time,realized_index,applied_index,index_bias,index_ratio", rows.get(0));

		// the group labels of the error table, so that a plot can put the error of a group next to the
		// reason for it
		assertTrue(rows.stream().anyMatch(row -> row.startsWith(CONGESTED_ROUTING + ",all,all,2,")));
		assertTrue(rows.stream().anyMatch(row -> row.startsWith(CONGESTED_ROUTING + ",hour,8,")));
		assertTrue(rows.stream().anyMatch(row -> row.startsWith(CONGESTED_ROUTING + ",hour,3,")));
		assertTrue(rows.stream().anyMatch(row -> row.contains(",distance_band,2000-5000,")));

		// the sanity row: against a free-speed reference the realized index is 0 by construction, so this
		// is where a bug in the denominator would show up as something other than the applied index
		String[] freeSpeedRow = rows.stream()
			.filter(row -> row.startsWith(FREE_SPEED_ROUTING + ",all,all,"))
			.findFirst().orElseThrow()
			.split(",", -1);
		assertEquals(0.0, Double.parseDouble(freeSpeedRow[7]), 1e-9, "realized index against free speed");
		assertEquals(APPLIED, Double.parseDouble(freeSpeedRow[8]), 1e-9);
		assertEquals("", freeSpeedRow[10], "a ratio to an index of zero is not a number a csv should carry");
	}

	/**
	 * One pair of the corridor at two times of day, with no free-speed travel time of their own, so the
	 * matrix's is used.
	 */
	private List<TravelTimeQuery> queries() {
		return List.of(
			TravelTimeQuery.of(node("n1"), node("n4"), 3 * 3600.0),
			TravelTimeQuery.of(node("n1"), node("n4"), 8 * 3600.0));
	}

	/**
	 * The same queries with a routed path on them: 3 km long - which is the {@code 2000-5000} distance
	 * band - and the exact free-speed travel time the fake matrix also answers, so that the two paths
	 * through {@link CongestionExposure} can be compared.
	 */
	private List<TravelTimeQuery> routedQueries() {
		return queries().stream()
			.map(query -> query.withRoutedPath(3 * TestNetworks.SPACING, FREE_SPEED))
			.toList();
	}

	/**
	 * A reference travel time that is a fixed multiple of the free-speed one, so that the realized index
	 * is the multiple minus one.
	 */
	private static Map<String, ToDoubleFunction<TravelTimeQuery>> reference(double factor) {
		return Map.of(CONGESTED_ROUTING, query -> factor * FREE_SPEED);
	}

	/**
	 * The matrix under test: {@value #FREE_SPEED} s for every pair, scaled by a constant index of
	 * {@value #APPLIED}.
	 */
	private static TimeVaryingTravelTimeMatrix matrix() {
		TravelTimeMatrix freeSpeed = (from, to, time) -> (int) FREE_SPEED;
		return TimeVaryingTravelTimeMatrix.global(freeSpeed, CongestionIndex.constant(APPLIED), config());
	}

	private static TravelTimeMatrixConfigGroup config() {
		TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();
		config.setCalibrationFactor(1.0);
		return config;
	}

	private Node node(String id) {
		return this.network.getNodes().get(Id.createNodeId(id));
	}
}
