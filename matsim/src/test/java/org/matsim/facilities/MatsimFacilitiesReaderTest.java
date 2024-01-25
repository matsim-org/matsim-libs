
/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimFacilitiesReaderTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

 package org.matsim.facilities;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
	 void testReadLinkId() {
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
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimFacilitiesReader reader = new MatsimFacilitiesReader(scenario);
		reader.parse(new ByteArrayInputStream(str.getBytes()));
		
		ActivityFacilities facilities = scenario.getActivityFacilities();
		Assertions.assertEquals(3, facilities.getFacilities().size());
		
		ActivityFacility fac1 = facilities.getFacilities().get(Id.create(1, ActivityFacility.class));
		Assertions.assertEquals(Id.create("Aa", Link.class), fac1.getLinkId());
		
		ActivityFacility fac10 = facilities.getFacilities().get(Id.create(10, ActivityFacility.class));
		Assertions.assertEquals(Id.create("Bb", Link.class), fac10.getLinkId());

		ActivityFacility fac20 = facilities.getFacilities().get(Id.create(20, ActivityFacility.class));
		Assertions.assertNull(fac20.getLinkId());
	}

	 @Test
	 void testRead3DCoord() {
		String str = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
	"<!DOCTYPE facilities SYSTEM \"http://www.matsim.org/files/dtd/facilities_v2.dtd\">\n" +
	"<facilities name=\"test facilities for triangle network\">\n" +
	"\n" +
	"	<facility id=\"1\" x=\"60.0\" y=\"110.0\" z=\"12.3\" linkId=\"Aa\">\n" +
	"		<activity type=\"home\">\n" +
	"			<capacity value=\"201.0\" />\n" +
	"			<opentime start_time=\"00:00:00\" end_time=\"24:00:00\" />\n" +
	"		</activity>\n" +
	"		<attributes>" +
	"			<attribute name=\"population\" class=\"java.lang.Integer\">1000</attribute>" +
	"		</attributes>" +
	"	</facility>\n" +
	"\n" +
	"	<facility id=\"10\" x=\"110.0\" y=\"270.0\" z=\"-4.2\" linkId=\"Bb\">\n" +
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
		Assertions.assertEquals(3, facilities.getFacilities().size());

		ActivityFacility fac1 = facilities.getFacilities().get(Id.create(1, ActivityFacility.class));
		Assertions.assertTrue(fac1.getCoord().hasZ());
		Assertions.assertEquals(12.3, fac1.getCoord().getZ(), Double.MIN_NORMAL);

		ActivityFacility fac10 = facilities.getFacilities().get(Id.create(10, ActivityFacility.class));
		Assertions.assertTrue(fac10.getCoord().hasZ());
		Assertions.assertEquals(-4.2, fac10.getCoord().getZ(), Double.MIN_NORMAL);

		ActivityFacility fac20 = facilities.getFacilities().get(Id.create(20, ActivityFacility.class));
		Assertions.assertFalse(fac20.getCoord().hasZ());
	}
}
