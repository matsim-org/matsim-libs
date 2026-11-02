package org.matsim.contrib.msp.traveltimematrix.congestion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.msp.traveltimematrix.RoadClassification;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the binned congestion index curve: which bin a time of day falls into, what happens outside the
 * day it was measured over, and that the csv is a complete description of the profile.
 */
class CongestionIndexProfileTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void aTimeIsLookedUpInTheBinItFallsInto() {

		CongestionIndexProfile profile = profile(900, new double[]{0.0, 0.1, 0.2, 0.3});

		assertEquals(0.0, profile.get(0));
		assertEquals(0.0, profile.get(899));
		assertEquals(0.1, profile.get(900));
		assertEquals(0.2, profile.get(2 * 900));
		assertEquals(0.3, profile.get(3 * 900 + 1));
	}

	@Test
	void aTimePastTheProfileGetsItsLastBin() {

		CongestionIndexProfile profile = profile(900, new double[]{0.0, 0.1, 0.2, 0.3});

		// MATSim counts on past midnight, and a late departure plus a long travel time lands past the end
		// of the measured day. The night is far closer to the late evening than to the morning, so the
		// last bin is the honest answer and wrapping around to bin 0 would not be.
		assertEquals(0.3, profile.get(4 * 900));
		assertEquals(0.3, profile.get(30 * 3600));
		assertEquals(0.0, profile.get(-1), "a time before the profile gets its first bin");
	}

	@Test
	void anUnknownRoadClassFallsBackToTheNetworkWideIndex() {

		Map<String, double[]> byClass = new LinkedHashMap<>();
		byClass.put(RoadClassification.ALL, new double[]{0.1, 0.2});
		byClass.put("motorway", new double[]{0.5, 0.6});
		CongestionIndexProfile profile = new CongestionIndexProfile(900, byClass);

		assertEquals(0.5, profile.get(0, "motorway"));
		// a class the profile does not hold is not an error: it means that class carried no traffic, and
		// the network-wide value is the least wrong stand-in
		assertEquals(0.1, profile.get(0, "residential"));
		assertEquals(0.2, profile.get(900, "residential"));
	}

	@Test
	void aProfileWithoutTheNetworkWideClassIsRefused() {

		Map<String, double[]> withoutAll = Map.of("motorway", new double[]{0.1, 0.2});

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> new CongestionIndexProfile(900, withoutAll));
		assertTrue(e.getMessage().contains(RoadClassification.ALL), e.getMessage());
	}

	@Test
	void classesHaveToCoverTheSameDay() {

		Map<String, double[]> ragged = new LinkedHashMap<>();
		ragged.put(RoadClassification.ALL, new double[]{0.1, 0.2});
		ragged.put("motorway", new double[]{0.5});

		assertThrows(IllegalArgumentException.class, () -> new CongestionIndexProfile(900, ragged));
	}

	@Test
	void theDailyMeanIsTheMeanOverTheBins() {
		assertEquals(0.15, profile(900, new double[]{0.0, 0.1, 0.2, 0.3}).getDailyMean(RoadClassification.ALL), 1e-12);
	}

	@Test
	void theCsvIsARoundTrip() {

		Map<String, double[]> byClass = new LinkedHashMap<>();
		byClass.put(RoadClassification.ALL, new double[]{0.0, 0.125, 0.25});
		byClass.put("motorway", new double[]{0.0, 0.5, 0.75});
		byClass.put("residential", new double[]{0.01, 0.02, 0.03});
		CongestionIndexProfile written = new CongestionIndexProfile(600, byClass);

		Path file = Path.of(utils.getOutputDirectory(), "congestion_index.csv");
		written.write(file.toString());
		CongestionIndexProfile read = CongestionIndexProfile.read(file.toString());

		// the bin size is read off the file, so a profile is a complete input on its own
		assertEquals(600, read.getTimeBinSize());
		assertEquals(3, read.getNumberOfBins());
		assertEquals(byClass.keySet(), read.getRoadClasses());
		for (String roadClass : byClass.keySet()) {
			assertArrayEqualsWithinWritingPrecision(byClass.get(roadClass), read.getValues(roadClass), roadClass);
		}
	}

	@Test
	void aProfileWithAGapInItIsRefused() {

		Path file = Path.of(utils.getOutputDirectory(), "gap.csv");
		writeLines(file, "road_class,bin_start,congestion_index", "all,0,0.1", "all,900,0.2", "all,2700,0.4");

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> CongestionIndexProfile.read(file.toString()));
		assertTrue(e.getMessage().contains("1800"), e.getMessage());
	}

	private static void writeLines(Path file, String... lines) {
		try {
			Files.write(file, List.of(lines));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static CongestionIndexProfile profile(int timeBinSize, double[] values) {
		return new CongestionIndexProfile(timeBinSize, Map.of(RoadClassification.ALL, values));
	}

	private static void assertArrayEqualsWithinWritingPrecision(double[] expected, double[] actual, String message) {
		assertEquals(expected.length, actual.length, message);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], actual[i], 1e-6, message + ", bin " + i);
		}
	}
}
