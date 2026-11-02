package org.matsim.contrib.msp.traveltimematrix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndex;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexProfile;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the config group: that it survives a round trip through a config file, and that the derived
 * objects it hands out - the DVRP matrix parameters, the road classification, the congestion index -
 * match what was configured.
 */
class TravelTimeMatrixConfigGroupTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void everyParameterSurvivesAConfigFile() {

		TravelTimeMatrixConfigGroup written = new TravelTimeMatrixConfigGroup();
		written.setVariant(TravelTimeMatrixConfigGroup.Variant.BY_ROAD_CLASS);
		written.setRoadClasses(TravelTimeMatrixConfigGroup.RoadClasses.OSM_HIGHWAY_TYPE);
		written.setEvaluateIndexAt(TravelTimeMatrixConfigGroup.EvaluateIndexAt.DEPARTURE);
		written.setTimeBinSize(600);
		written.setEndTime(108000);
		written.setCongestionIndexFile("congestion_index.csv");
		written.setCalibrationFactor(0.8);
		written.setCellSize(2000);
		written.setMaxNeighborDistance(7500);
		written.setMaxNeighborTravelTime(300);
		written.setMatrixCacheFile("matrix.bin");
		written.setNumberOfThreads(7);

		Config config = ConfigUtils.createConfig();
		config.addModule(written);
		Path file = Path.of(utils.getOutputDirectory(), "config.xml");
		new ConfigWriter(config).write(file.toString());

		TravelTimeMatrixConfigGroup read = ConfigUtils.addOrGetModule(
			ConfigUtils.loadConfig(file.toString(), new TravelTimeMatrixConfigGroup()),
			TravelTimeMatrixConfigGroup.class);

		assertEquals(TravelTimeMatrixConfigGroup.Variant.BY_ROAD_CLASS, read.getVariant());
		assertEquals(TravelTimeMatrixConfigGroup.RoadClasses.OSM_HIGHWAY_TYPE, read.getRoadClasses());
		assertEquals(TravelTimeMatrixConfigGroup.EvaluateIndexAt.DEPARTURE, read.getEvaluateIndexAt());
		assertEquals(600, read.getTimeBinSize());
		assertEquals(108000, read.getEndTime());
		assertEquals("congestion_index.csv", read.getCongestionIndexFile());
		assertEquals(0.8, read.getCalibrationFactor());
		assertEquals(2000, read.getCellSize());
		assertEquals(7500, read.getMaxNeighborDistance());
		assertEquals(300, read.getMaxNeighborTravelTime());
		assertEquals("matrix.bin", read.getMatrixCacheFile());
		assertEquals(7, read.getNumberOfThreads());
	}

	@Test
	void everyParameterIsDocumented() {
		TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();
		Map<String, String> comments = config.getComments();
		for (String parameter : config.getParams().keySet()) {
			assertTrue(comments.containsKey(parameter), "'" + parameter + "' has no comment, so a config file "
				+ "written from this group would not say what it means");
		}
	}

	@Test
	void theMatrixParametersAreTheOnesConfigured() {

		TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();
		config.setCellSize(1500);
		config.setMaxNeighborDistance(6000);
		config.setMaxNeighborTravelTime(120);

		DvrpTravelTimeMatrixParams params = config.getMatrixParams();

		assertEquals(6000, params.getMaxNeighborDistance());
		assertEquals(120, params.getMaxNeighborTravelTime());
		assertEquals(1500, ((SquareGridZoneSystemParams) params.getZoneSystemParams()).getCellSize());
	}

	@Test
	void onlyTheRoadClassVariantSplitsByRoadClass() {

		TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();

		config.setVariant(TravelTimeMatrixConfigGroup.Variant.GLOBAL_INDEX);
		assertTrue(config.getRoadClassification(TestNetworks.corridor()).isSingleClass(),
			"level 0 needs no road classes, so asking for them must not build any");

		config.setVariant(TravelTimeMatrixConfigGroup.Variant.BY_ROAD_CLASS);
		config.setRoadClasses(TravelTimeMatrixConfigGroup.RoadClasses.OSM_HIGHWAY_TYPE);
		assertEquals(List.of("motorway", "residential", RoadClassification.OTHER),
			config.getRoadClassification(TestNetworks.corridor()).getRoadClasses());
	}

	@Test
	void theFreeSpeedVariantNeedsNoCongestionIndexFile() {

		TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();
		config.setVariant(TravelTimeMatrixConfigGroup.Variant.FREE_SPEED);

		assertEquals(0.0, config.getCongestionIndex().get(8 * 3600));
	}

	@Test
	void aMissingCongestionIndexFileIsRefusedByName() {

		TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();
		config.setVariant(TravelTimeMatrixConfigGroup.Variant.GLOBAL_INDEX);

		IllegalStateException e = assertThrows(IllegalStateException.class, config::getCongestionIndex);
		assertTrue(e.getMessage().contains("congestionIndexFile"), e.getMessage());
	}

	@Test
	void theConfiguredIndexFileIsRead() {

		Path file = Path.of(utils.getOutputDirectory(), "congestion_index.csv");
		new CongestionIndexProfile(900, Map.of(RoadClassification.ALL, new double[]{0.1, 0.4}))
			.write(file.toString());

		TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();
		config.setVariant(TravelTimeMatrixConfigGroup.Variant.GLOBAL_INDEX);
		config.setCongestionIndexFile(file.toString());

		CongestionIndex index = config.getCongestionIndex();
		assertEquals(0.1, index.get(0), 1e-6);
		assertEquals(0.4, index.get(900), 1e-6);
	}
}
