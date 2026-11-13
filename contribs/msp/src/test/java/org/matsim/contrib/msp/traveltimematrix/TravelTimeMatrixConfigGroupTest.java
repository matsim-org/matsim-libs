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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
		written.setVariant(TravelTimeMatrixConfigGroup.Variant.GLOBAL_INDEX);
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

		assertEquals(TravelTimeMatrixConfigGroup.Variant.GLOBAL_INDEX, read.getVariant());
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

	/**
	 * The road classification is a diagnostic on the <b>index</b> and not a variant of the matrix - the
	 * matrix reads the network-wide curve whatever is set here - so it is built whenever it is asked for.
	 * It used to depend on the variant, which is the one thing that changed when level 1 was removed.
	 */
	@Test
	void theRoadClassesAreBuiltWhicheverVariantIsSet() {

		TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();

		config.setRoadClasses(TravelTimeMatrixConfigGroup.RoadClasses.OSM_HIGHWAY_TYPE);
		assertEquals(List.of("motorway", "residential", RoadClassification.OTHER),
			config.getRoadClassification(TestNetworks.corridor()).getRoadClasses());

		config.setVariant(TravelTimeMatrixConfigGroup.Variant.FREE_SPEED);
		config.setRoadClasses(TravelTimeMatrixConfigGroup.RoadClasses.DEFAULT_GROUPS);
		assertEquals(List.of("motorway", "primary", "residential", "secondary", "tertiary",
				RoadClassification.OTHER),
			config.getRoadClassification(TestNetworks.corridor()).getRoadClasses(),
			"the default groups, whichever of them the network actually has links of");
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

	@Test
	void anInfiniteNeighborhoodIsRecognisedAndNotCachedByDirectory() {

		TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();
		config.setMatrixCacheDirectory(utils.getOutputDirectory());
		config.setMaxNeighborDistance(5000);

		assertFalse(config.isUnboundedNeighborhood());
		assertNotNull(config.getMatrixCacheFile(), "a finite radius caches under the name the thresholds imply");

		config.setMaxNeighborDistance(Double.POSITIVE_INFINITY);

		assertTrue(config.isUnboundedNeighborhood());
		// the cache format of contribs/dvrp starts with the zone system this configuration has none of,
		// and (long) of an infinity would make a nonsense of the file name besides
		assertNull(config.getMatrixCacheFile(), "an unbounded radius is not cached from a directory");
	}

	@Test
	void anExplicitCacheFileIsStillHonouredWhenUnbounded() {

		TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();
		config.setMaxNeighborDistance(Double.POSITIVE_INFINITY);
		config.setMatrixCacheFile("/tmp/mine.bin");

		assertEquals("/tmp/mine.bin", config.getMatrixCacheFile(),
			"a caller who names a file has said they know what they are doing");
	}

	@Test
	void aNeighborhoodRadiusIsReadFromTheCommandLineSpellingOfInfinity() {

		assertEquals(Double.POSITIVE_INFINITY, MatrixOptions.parseNeighborDistance("inf"));
		assertEquals(Double.POSITIVE_INFINITY, MatrixOptions.parseNeighborDistance(" INF "));
		assertEquals(Double.POSITIVE_INFINITY, MatrixOptions.parseNeighborDistance("Infinity"));
		assertEquals(5000.0, MatrixOptions.parseNeighborDistance("5000"));
		assertThrows(IllegalArgumentException.class, () -> MatrixOptions.parseNeighborDistance("far"));
	}
}
