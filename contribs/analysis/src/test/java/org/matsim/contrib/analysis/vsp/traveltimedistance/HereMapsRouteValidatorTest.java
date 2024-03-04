package org.matsim.contrib.analysis.vsp.traveltimedistance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestUtils;

import java.io.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class HereMapsRouteValidatorTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testReadJson() throws IOException {
		HereMapsRouteValidator hereMapsRouteValidator = getDummyValidator(false);
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(utils.getClassInputDirectory() + "route.json")));
		Optional<Tuple<Double, Double>> result = hereMapsRouteValidator.readFromJson(reader, null);
		assertTrue(result.isPresent());
		assertEquals(394, result.get().getFirst(), MatsimTestUtils.EPSILON);
		assertEquals(2745, result.get().getSecond(), MatsimTestUtils.EPSILON);
	}

	@Test
	void testWriteFile() throws IOException {
		HereMapsRouteValidator hereMapsRouteValidator = getDummyValidator(true);
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(utils.getClassInputDirectory() + "route.json")));
		hereMapsRouteValidator.readFromJson(reader, "tripId");
		assertTrue(new File(utils.getOutputDirectory() + "tripId.json.gz").isFile());
	}

	//All values with null filled are not necessary for this test
	private HereMapsRouteValidator getDummyValidator(boolean writeDetailedFiles) {
		return new HereMapsRouteValidator(utils.getOutputDirectory(), null, null, null, null, writeDetailedFiles);
	}
}
