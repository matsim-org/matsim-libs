package org.matsim.contrib.msp.traveltimematrix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexProfile;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link TimeVaryingTravelTimeMatrix#create} end to end: the real free-speed matrix of
 * {@code contribs/dvrp} underneath, a congestion index read from a file, and - for level 1 - road class
 * shares computed over the same network.
 * <p>
 * The other tests stub the base matrix out to isolate the decorator; this one is the counterpart that
 * checks the wiring, including that the free-speed travel times of the real matrix are the ones the
 * corridor was built to have.
 */
class TimeVaryingTravelTimeMatrixCreateTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * n1 to n4 on {@link TestNetworks#corridor()}: two residential links of 101 s and one motorway link
	 * of 21 s.
	 */
	private static final int FREE_SPEED_END_TO_END = 223;

	@Test
	void theFreeSpeedVariantIsTheFreeSpeedMatrix() {

		Network network = TestNetworks.corridor();
		TravelTimeMatrixConfigGroup config = config(TravelTimeMatrixConfigGroup.Variant.FREE_SPEED);

		TimeVaryingTravelTimeMatrix matrix = TimeVaryingTravelTimeMatrix.create(network, config, 1);

		assertEquals(FREE_SPEED_END_TO_END, matrix.getTravelTime(node(network, "n1"), node(network, "n4"), 8 * 3600));
		assertEquals(0, matrix.getTravelTime(node(network, "n1"), node(network, "n1"), 8 * 3600));
	}

	@Test
	void levelZeroScalesEveryPairByTheSameFactor() {

		Network network = TestNetworks.corridor();
		TravelTimeMatrixConfigGroup config = config(TravelTimeMatrixConfigGroup.Variant.GLOBAL_INDEX);
		config.setCongestionIndexFile(writeIndex("global.csv",
			Map.of(RoadClassification.ALL, new double[]{0.0, 0.5})));

		TimeVaryingTravelTimeMatrix matrix = TimeVaryingTravelTimeMatrix.create(network, config, 1);

		// the profile has two bins of 43200 s, so the morning is free flowing and the afternoon is not
		assertEquals(FREE_SPEED_END_TO_END, matrix.getTravelTime(node(network, "n1"), node(network, "n4"), 3600));
		assertEquals(Math.round(FREE_SPEED_END_TO_END * 1.5),
			matrix.getTravelTime(node(network, "n1"), node(network, "n4"), 16 * 3600));
	}

	@Test
	void levelOneScalesAMotorwayPairLessThanAResidentialOne() {

		Network network = TestNetworks.corridor();
		TravelTimeMatrixConfigGroup config = config(TravelTimeMatrixConfigGroup.Variant.BY_ROAD_CLASS);
		config.setRoadClasses(TravelTimeMatrixConfigGroup.RoadClasses.OSM_HIGHWAY_TYPE);

		Map<String, double[]> byClass = new LinkedHashMap<>();
		byClass.put(RoadClassification.ALL, new double[]{0.5, 0.5});
		byClass.put("motorway", new double[]{0.0, 0.0});
		byClass.put("residential", new double[]{1.0, 1.0});
		byClass.put(RoadClassification.OTHER, new double[]{0.0, 0.0});
		config.setCongestionIndexFile(writeIndex("by-road-class.csv", byClass));

		TimeVaryingTravelTimeMatrix matrix = TimeVaryingTravelTimeMatrix.create(network, config, 1);

		Node n1 = node(network, "n1");
		Node n2 = node(network, "n2");
		Node n3 = node(network, "n3");
		Node n4 = node(network, "n4");

		// the motorway is free flowing, the residential streets are twice as slow, and the end to end
		// trip sits between the two in proportion to how much of its time is spent on each
		assertEquals(21, matrix.getTravelTime(n2, n3, 8 * 3600));
		assertEquals(2 * 101, matrix.getTravelTime(n1, n2, 8 * 3600));
		int endToEnd = matrix.getTravelTime(n1, n4, 8 * 3600);
		assertEquals(Math.round(FREE_SPEED_END_TO_END * (1 + 202.0 / 223)), endToEnd, 2);
		assertTrue(endToEnd > FREE_SPEED_END_TO_END && endToEnd < 2 * FREE_SPEED_END_TO_END);

		assertEquals(3, matrix.getRoadClassShares().getRoadClasses().size());
	}

	@Test
	void theMatrixCacheIsWrittenAndReadBack() {

		Network network = TestNetworks.corridor();
		Path cache = Path.of(utils.getOutputDirectory(), "matrix.bin");

		TravelTimeMatrixConfigGroup config = config(TravelTimeMatrixConfigGroup.Variant.FREE_SPEED);
		config.setMatrixCacheFile(cache.toString());

		int first = TimeVaryingTravelTimeMatrix.create(network, config, 1)
			.getTravelTime(node(network, "n1"), node(network, "n4"), 0);
		assertTrue(Files.exists(cache), "the cache is written when it does not exist yet");

		int second = TimeVaryingTravelTimeMatrix.create(network, config, 1)
			.getTravelTime(node(network, "n1"), node(network, "n4"), 0);
		assertEquals(first, second, "a matrix read from the cache answers like the one that wrote it");
	}

	private TravelTimeMatrixConfigGroup config(TravelTimeMatrixConfigGroup.Variant variant) {
		TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();
		config.setVariant(variant);
		// one zone per node, and no sparse part: this test is about the zonal matrix and the shares,
		// which are stored per zone pair
		config.setCellSize(TestNetworks.SPACING / 2.5);
		config.setMaxNeighborDistance(0);
		config.setMaxNeighborTravelTime(0);
		config.setNumberOfThreads(1);
		config.setEvaluateIndexAt(TravelTimeMatrixConfigGroup.EvaluateIndexAt.DEPARTURE);
		return config;
	}

	private String writeIndex(String name, Map<String, double[]> byRoadClass) {
		Path file = Path.of(utils.getOutputDirectory(), name);
		new CongestionIndexProfile(43200, byRoadClass).write(file.toString());
		return file.toString();
	}

	private static Node node(Network network, String id) {
		return network.getNodes().get(Id.createNodeId(id));
	}
}
