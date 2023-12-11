package org.matsim.contrib.analysis.vsp.traveltimedistance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestUtils;

import java.io.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GoogleMapRouteValidatorTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testReadJson() throws IOException {
		GoogleMapRouteValidator googleMapRouteValidator = getDummyValidator();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(utils.getClassInputDirectory() + "route.json")));
		Optional<Tuple<Double, Double>> result = googleMapRouteValidator.readFromJson(reader);
		assertTrue(result.isPresent());
		assertEquals(413, result.get().getFirst(), MatsimTestUtils.EPSILON);
		assertEquals(2464, result.get().getSecond(), MatsimTestUtils.EPSILON);
	}

	//All values with null filled are not necessary for this test
	private GoogleMapRouteValidator getDummyValidator() {
		return new GoogleMapRouteValidator(utils.getOutputDirectory(), null, null, null, null);
	}
}
