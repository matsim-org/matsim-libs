/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesParserWriterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.examples.TriangleScenario;
import org.matsim.testcases.MatsimTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * @author mrieser / Simunto GmbH
 */
public class FacilitiesParserWriterTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testParserWriter1() {
		Config config = ConfigUtils.createConfig();
		TriangleScenario.setUpScenarioConfig(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		ActivityFacilities facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());

		String outputFilename = this.utils.getOutputDirectory() + "output_facilities.xml";
		new FacilitiesWriterV1(new IdentityTransformation(), facilities).write(outputFilename);

		long checksum_ref = CRCChecksum.getCRCFromFile(config.facilities().getInputFile());
		long checksum_run = CRCChecksum.getCRCFromFile(outputFilename);
		Assertions.assertEquals(checksum_ref, checksum_run);
	}

	@Test
	void testWriteReadV2_withActivities() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		ActivityFacilities facilities = scenario.getActivityFacilities();

		ActivityFacilitiesFactory factory = facilities.getFactory();

		ActivityFacility fac1 = factory.createActivityFacility(Id.create("1", ActivityFacility.class), new Coord(10.0, 15.0));
		fac1.addActivityOption(new ActivityOptionImpl("home"));
		facilities.addActivityFacility(fac1);

		ActivityFacility fac2 = factory.createActivityFacility(Id.create("2", ActivityFacility.class), new Coord(20.0, 25.0));
		ActivityOptionImpl shopOption = new ActivityOptionImpl("shop");
		shopOption.addOpeningTime(new OpeningTimeImpl(8*3600, 20*3600));
		fac2.addActivityOption(shopOption);
		facilities.addActivityFacility(fac2);

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		new FacilitiesWriter(facilities).write(outStream);

		/* ------ */

		ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
		ActivityFacilities facilities2 = FacilitiesUtils.createActivityFacilities();
		new MatsimFacilitiesReader(null, null, facilities2).parse(inStream);

		Assertions.assertEquals(2, facilities2.getFacilities().size());

		ActivityFacility fac1b = facilities2.getFacilities().get(Id.create("1", ActivityFacility.class));
		Assertions.assertEquals(1, fac1b.getActivityOptions().size());
		Assertions.assertTrue(fac1b.getActivityOptions().get("home").getOpeningTimes().isEmpty());
		Assertions.assertEquals(0, fac1b.getAttributes().size());

		ActivityFacility fac2b = facilities2.getFacilities().get(Id.create("2", ActivityFacility.class));
		Assertions.assertEquals(1, fac2b.getActivityOptions().size());
		Assertions.assertNotNull(fac2b.getActivityOptions().get("shop").getOpeningTimes());
		Assertions.assertEquals(8*3600, fac2b.getActivityOptions().get("shop").getOpeningTimes().first().getStartTime(), 0.0);
		Assertions.assertEquals(20*3600, fac2b.getActivityOptions().get("shop").getOpeningTimes().first().getEndTime(), 0.0);
		Assertions.assertEquals(0, fac2b.getAttributes().size());
	}

	@Test
	void testWriteReadV2_withAttributes() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		ActivityFacilities facilities = scenario.getActivityFacilities();

		ActivityFacilitiesFactory factory = facilities.getFactory();

		ActivityFacility fac1 = factory.createActivityFacility(Id.create("1", ActivityFacility.class), new Coord(10.0, 15.0));
		fac1.getAttributes().putAttribute("size_m2", 100);
		facilities.addActivityFacility(fac1);

		ActivityFacility fac2 = factory.createActivityFacility(Id.create("2", ActivityFacility.class), new Coord(20.0, 25.0));
		fac2.getAttributes().putAttribute("size_m2", 500);
		facilities.addActivityFacility(fac2);

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		new FacilitiesWriter(facilities).write(outStream);

		/* ------ */

		ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
		ActivityFacilities facilities2 = FacilitiesUtils.createActivityFacilities();
		new MatsimFacilitiesReader(null, null, facilities2).parse(inStream);

		Assertions.assertEquals(2, facilities2.getFacilities().size());

		ActivityFacility fac1b = facilities2.getFacilities().get(Id.create("1", ActivityFacility.class));
		Assertions.assertEquals(0, fac1b.getActivityOptions().size());
		Assertions.assertEquals(1, fac1b.getAttributes().size());
		Assertions.assertEquals(100, fac1b.getAttributes().getAttribute("size_m2"));

		ActivityFacility fac2b = facilities2.getFacilities().get(Id.create("2", ActivityFacility.class));
		Assertions.assertEquals(0, fac2b.getActivityOptions().size());
		Assertions.assertEquals(1, fac2b.getAttributes().size());
		Assertions.assertEquals(500, fac2b.getAttributes().getAttribute("size_m2"));
	}

	@Test
	void testWriteReadV2_withActivitiesAndAttributes() { // MATSIM-859
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		ActivityFacilities facilities = scenario.getActivityFacilities();

		ActivityFacilitiesFactory factory = facilities.getFactory();

		ActivityFacility fac1 = factory.createActivityFacility(Id.create("1", ActivityFacility.class), new Coord(10.0, 15.0));
		fac1.addActivityOption(new ActivityOptionImpl("home"));
		fac1.getAttributes().putAttribute("size_m2", 100);
		facilities.addActivityFacility(fac1);


		ActivityFacility fac2 = factory.createActivityFacility(Id.create("2", ActivityFacility.class), new Coord(20.0, 25.0));
		ActivityOptionImpl shopOption = new ActivityOptionImpl("shop");
		shopOption.addOpeningTime(new OpeningTimeImpl(8*3600, 20*3600));
		fac2.addActivityOption(shopOption);
		fac2.getAttributes().putAttribute("size_m2", 500);
		facilities.addActivityFacility(fac2);

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		new FacilitiesWriter(facilities).write(outStream);

		/* ------ */

		ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
		ActivityFacilities facilities2 = FacilitiesUtils.createActivityFacilities();
		new MatsimFacilitiesReader(null, null, facilities2).parse(inStream);

		Assertions.assertEquals(2, facilities2.getFacilities().size());

		ActivityFacility fac1b = facilities2.getFacilities().get(Id.create("1", ActivityFacility.class));
		Assertions.assertEquals(1, fac1b.getActivityOptions().size());
		Assertions.assertTrue(fac1b.getActivityOptions().get("home").getOpeningTimes().isEmpty());
		Assertions.assertEquals(1, fac1b.getAttributes().size());
		Assertions.assertEquals(100, fac1b.getAttributes().getAttribute("size_m2"));

		ActivityFacility fac2b = facilities2.getFacilities().get(Id.create("2", ActivityFacility.class));
		Assertions.assertEquals(1, fac2b.getActivityOptions().size());
		Assertions.assertNotNull(fac2b.getActivityOptions().get("shop").getOpeningTimes());
		Assertions.assertEquals(8*3600, fac2b.getActivityOptions().get("shop").getOpeningTimes().first().getStartTime(), 0.0);
		Assertions.assertEquals(20*3600, fac2b.getActivityOptions().get("shop").getOpeningTimes().first().getEndTime(), 0.0);
		Assertions.assertEquals(1, fac2b.getAttributes().size());
		Assertions.assertEquals(500, fac2b.getAttributes().getAttribute("size_m2"));
	}

}
