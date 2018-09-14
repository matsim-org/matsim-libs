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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.examples.TriangleScenario;
import org.matsim.testcases.MatsimTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * @author mrieser / Simunto GmbH
 */
public class FacilitiesParserWriterTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testParserWriter1() {
		Config config = ConfigUtils.createConfig();
		TriangleScenario.setUpScenarioConfig(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		ActivityFacilities facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());

		String outputFilename = this.utils.getOutputDirectory() + "output_facilities.xml";
		TriangleScenario.writeFacilities(facilities, outputFilename);

		long checksum_ref = CRCChecksum.getCRCFromFile(config.facilities().getInputFile());
		long checksum_run = CRCChecksum.getCRCFromFile(outputFilename);
		Assert.assertEquals(checksum_ref, checksum_run);
	}

	@Test
	public void testWriteReadV1_withActivities() {
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

		Assert.assertEquals(2, facilities2.getFacilities().size());

		ActivityFacility fac1b = facilities2.getFacilities().get(Id.create("1", ActivityFacility.class));
		Assert.assertEquals(1, fac1b.getActivityOptions().size());
		Assert.assertTrue(fac1b.getActivityOptions().get("home").getOpeningTimes().isEmpty());
		Assert.assertEquals(0, fac1b.getAttributes().size());

		ActivityFacility fac2b = facilities2.getFacilities().get(Id.create("2", ActivityFacility.class));
		Assert.assertEquals(1, fac2b.getActivityOptions().size());
		Assert.assertNotNull(fac2b.getActivityOptions().get("shop").getOpeningTimes());
		Assert.assertEquals(8*3600, fac2b.getActivityOptions().get("shop").getOpeningTimes().first().getStartTime(), 0.0);
		Assert.assertEquals(20*3600, fac2b.getActivityOptions().get("shop").getOpeningTimes().first().getEndTime(), 0.0);
		Assert.assertEquals(0, fac2b.getAttributes().size());
	}

	@Test
	public void testWriteReadV1_withAttributes() {
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

		Assert.assertEquals(2, facilities2.getFacilities().size());

		ActivityFacility fac1b = facilities2.getFacilities().get(Id.create("1", ActivityFacility.class));
		Assert.assertEquals(0, fac1b.getActivityOptions().size());
		Assert.assertEquals(1, fac1b.getAttributes().size());
		Assert.assertEquals(100, fac1b.getAttributes().getAttribute("size_m2"));

		ActivityFacility fac2b = facilities2.getFacilities().get(Id.create("2", ActivityFacility.class));
		Assert.assertEquals(0, fac2b.getActivityOptions().size());
		Assert.assertEquals(1, fac2b.getAttributes().size());
		Assert.assertEquals(500, fac2b.getAttributes().getAttribute("size_m2"));
	}

	@Test
	public void testWriteReadV1_withActivitiesAndAttributes() { // MATSIM-859
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

		Assert.assertEquals(2, facilities2.getFacilities().size());

		ActivityFacility fac1b = facilities2.getFacilities().get(Id.create("1", ActivityFacility.class));
		Assert.assertEquals(1, fac1b.getActivityOptions().size());
		Assert.assertTrue(fac1b.getActivityOptions().get("home").getOpeningTimes().isEmpty());
		Assert.assertEquals(1, fac1b.getAttributes().size());
		Assert.assertEquals(100, fac1b.getAttributes().getAttribute("size_m2"));

		ActivityFacility fac2b = facilities2.getFacilities().get(Id.create("2", ActivityFacility.class));
		Assert.assertEquals(1, fac2b.getActivityOptions().size());
		Assert.assertNotNull(fac2b.getActivityOptions().get("shop").getOpeningTimes());
		Assert.assertEquals(8*3600, fac2b.getActivityOptions().get("shop").getOpeningTimes().first().getStartTime(), 0.0);
		Assert.assertEquals(20*3600, fac2b.getActivityOptions().get("shop").getOpeningTimes().first().getEndTime(), 0.0);
		Assert.assertEquals(1, fac2b.getAttributes().size());
		Assert.assertEquals(500, fac2b.getAttributes().getAttribute("size_m2"));
	}

}
