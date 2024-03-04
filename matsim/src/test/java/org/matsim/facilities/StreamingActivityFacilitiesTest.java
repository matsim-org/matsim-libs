package org.matsim.facilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

/**
 * @author mrieser / Simunto GmbH
 */
public class StreamingActivityFacilitiesTest {

	@Test
	void testFacilityIsComplete() {
		String str = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<!DOCTYPE facilities SYSTEM \"http://www.matsim.org/files/dtd/facilities_v1.dtd\">\n" +
				"<facilities name=\"test facilities for triangle network\">\n" +
				"\n" +
				"	<facility id=\"1\" x=\"60.0\" y=\"110.0\" linkId=\"Aa\">\n" +
				"		<activity type=\"home\">\n" +
				"			<capacity value=\"201.0\" />\n" +
				"			<opentime start_time=\"00:00:00\" end_time=\"24:00:00\" />\n" +
				"		</activity>\n" +
				"		<attributes>" +
				"			<attribute name=\"population\" class=\"java.lang.Integer\">1000</attribute>" +
				"		</attributes>" +
				"	</facility>\n" +
				"\n" +
				"	<facility id=\"10\" x=\"110.0\" y=\"270.0\" linkId=\"Bb\">\n" +
				"		<activity type=\"education\">\n" +
				"			<capacity value=\"201.0\" />\n" +
				"			<opentime start_time=\"08:00:00\" end_time=\"12:00:00\" />\n" +
				"		</activity>\n" +
				"	</facility>\n" +
				"\n" +
				"	<facility id=\"20\" x=\"120.0\" y=\"240.0\">\n" +
				"		<activity type=\"shop\">\n" +
				"			<capacity value=\"50.0\" />\n" +
				"			<opentime start_time=\"08:00:00\" end_time=\"20:00:00\" />\n" +
				"		</activity>\n" +
				"	</facility>\n" +
				"</facilities>";


		boolean[] foundFacilities = new boolean[3];
		StreamingActivityFacilities streamingFacilities = new StreamingActivityFacilities(f -> {
			if (f.getId().toString().equals("1")) {
				Assertions.assertEquals(60.0, f.getCoord().getX(), 1e-7);
				Assertions.assertTrue(f.getActivityOptions().containsKey("home"));
				Assertions.assertEquals(1000, ((Integer) f.getAttributes().getAttribute("population")).intValue());
				foundFacilities[0] = true;
			}
			if (f.getId().toString().equals("10")) {
				Assertions.assertEquals(110.0, f.getCoord().getX(), 1e-7);
				Assertions.assertTrue(f.getActivityOptions().containsKey("education"));
				Assertions.assertTrue(f.getAttributes().isEmpty());
				foundFacilities[1] = true;
			}
			if (f.getId().toString().equals("20")) {
				Assertions.assertEquals(120.0, f.getCoord().getX(), 1e-7);
				Assertions.assertTrue(f.getActivityOptions().containsKey("shop"));
				Assertions.assertTrue(f.getAttributes().isEmpty());
				foundFacilities[2] = true;
			}
		});
		MatsimFacilitiesReader reader = new MatsimFacilitiesReader(null, null, streamingFacilities);
		reader.parse(new ByteArrayInputStream(str.getBytes()));

		Assertions.assertTrue(foundFacilities[0]);
		Assertions.assertTrue(foundFacilities[1]);
		Assertions.assertTrue(foundFacilities[2]);
	}

}
