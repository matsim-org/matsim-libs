package org.matsim.facilities;

import java.io.ByteArrayInputStream;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author mrieser / Senozon AG
 */
public class MatsimFacilitiesReaderTest {

	@Test
	public void testReadLinkId() {
		String str = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE facilities SYSTEM \"http://www.matsim.org/files/dtd/facilities_v1.dtd\">\n" +
"<facilities name=\"test facilities for triangle network\">\n" +
"\n" +
"	<facility id=\"1\" x=\"60.0\" y=\"110.0\" linkId=\"Aa\">\n" +
"		<activity type=\"home\">\n" +
"			<capacity value=\"201.0\" />\n" +
"			<opentime start_time=\"00:00:00\" end_time=\"24:00:00\" />\n" +
"		</activity>\n" +
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
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimFacilitiesReader reader = new MatsimFacilitiesReader(scenario);
		reader.parse(new ByteArrayInputStream(str.getBytes()));
		
		ActivityFacilities facilities = scenario.getActivityFacilities();
		Assert.assertEquals(3, facilities.getFacilities().size());
		
		ActivityFacility fac1 = facilities.getFacilities().get(Id.create(1, ActivityFacility.class));
		Assert.assertEquals(Id.create("Aa", Link.class), fac1.getLinkId());
		
		ActivityFacility fac10 = facilities.getFacilities().get(Id.create(10, ActivityFacility.class));
		Assert.assertEquals(Id.create("Bb", Link.class), fac10.getLinkId());

		ActivityFacility fac20 = facilities.getFacilities().get(Id.create(20, ActivityFacility.class));
		Assert.assertNull(fac20.getLinkId());
	}
}
