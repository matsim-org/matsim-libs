
/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesWriterTest.java
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

	/**
 * @author mrieser
 */
public class FacilitiesWriterTest {

	 @Test
	 void testWriteLinkId() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        ActivityFacilities facilities = scenario.getActivityFacilities();
        ActivityFacilitiesFactory factory = facilities.getFactory();

        ActivityFacility fac1 = factory.createActivityFacility(Id.create("1", ActivityFacility.class), new Coord(10.0, 15.0));
        ((ActivityFacilityImpl) fac1).setLinkId(Id.create("Abc", Link.class));
        ActivityFacility fac2 = factory.createActivityFacility(Id.create("2", ActivityFacility.class), new Coord(20.0, 25.0));
        ((ActivityFacilityImpl) fac2).setLinkId(Id.create("Def", Link.class));
        ActivityFacility fac3 = factory.createActivityFacility(Id.create("3", ActivityFacility.class), new Coord(30.0, 35.0));

        fac1.getAttributes().putAttribute("population", 1000);
        fac2.getAttributes().putAttribute("population", 1200);
        fac3.getAttributes().putAttribute("owner", "pepsiCo");

        facilities.addActivityFacility(fac1);
        facilities.addActivityFacility(fac2);
        facilities.addActivityFacility(fac3);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1000);
        new FacilitiesWriter(facilities).write(outputStream);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        facilities = scenario.getActivityFacilities();
        MatsimFacilitiesReader reader = new MatsimFacilitiesReader(scenario);
        reader.parse(inputStream);

        Assertions.assertEquals(3, facilities.getFacilities().size());

        ActivityFacility fac1b = facilities.getFacilities().get(Id.create(1, ActivityFacility.class));
        Assertions.assertEquals(Id.create("Abc", Link.class), fac1b.getLinkId());
        Assertions.assertEquals(1000, fac1b.getAttributes().getAttribute("population"));

        ActivityFacility fac2b = facilities.getFacilities().get(Id.create(2, ActivityFacility.class));
        Assertions.assertEquals(Id.create("Def", Link.class), fac2b.getLinkId());
        Assertions.assertEquals(1200, fac2b.getAttributes().getAttribute("population"));

        ActivityFacility fac3b = facilities.getFacilities().get(Id.create(3, ActivityFacility.class));
        Assertions.assertNull(fac3b.getLinkId());
        Assertions.assertEquals("pepsiCo", fac3b.getAttributes().getAttribute("owner"));
    }

	 @Test
	 void testWrite3DCoord() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        ActivityFacilities facilities = scenario.getActivityFacilities();
        ActivityFacilitiesFactory factory = facilities.getFactory();

        ActivityFacility fac1 = factory.createActivityFacility(Id.create("1", ActivityFacility.class), new Coord(10.0, 15.0, 12.3));
        ActivityFacility fac2 = factory.createActivityFacility(Id.create("2", ActivityFacility.class), new Coord(20.0, 25.0, -4.2));
        ActivityFacility fac3 = factory.createActivityFacility(Id.create("3", ActivityFacility.class), new Coord(30.0, 35.0));

        facilities.addActivityFacility(fac1);
        facilities.addActivityFacility(fac2);
        facilities.addActivityFacility(fac3);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1000);
        new FacilitiesWriter(facilities).write(outputStream);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        facilities = scenario.getActivityFacilities();
        MatsimFacilitiesReader reader = new MatsimFacilitiesReader(scenario);
        reader.parse(inputStream);

        Assertions.assertEquals(3, facilities.getFacilities().size());

        ActivityFacility fac1b = facilities.getFacilities().get(Id.create(1, ActivityFacility.class));
        Assertions.assertTrue(fac1b.getCoord().hasZ());
        Assertions.assertEquals(12.3, fac1b.getCoord().getZ(), Double.MIN_NORMAL);

        ActivityFacility fac2b = facilities.getFacilities().get(Id.create(2, ActivityFacility.class));
        Assertions.assertTrue(fac2b.getCoord().hasZ());
        Assertions.assertEquals(-4.2, fac2b.getCoord().getZ(), Double.MIN_NORMAL);

        ActivityFacility fac3b = facilities.getFacilities().get(Id.create(3, ActivityFacility.class));
        Assertions.assertFalse(fac3b.getCoord().hasZ());
    }

	 // the better fix for https://github.com/matsim-org/matsim/pull/505
	 @Test
	 void testFacilityDescription() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        ActivityFacilities facilities = scenario.getActivityFacilities();
        ActivityFacilitiesFactory factory = facilities.getFactory();

        String desc = "Some special text & that could pose <problems> to \"html\' or { json }.";


        ActivityFacility fac1 = factory.createActivityFacility(Id.create("1", ActivityFacility.class), new Coord(10.0, 15.0));
        ((ActivityFacilityImpl) fac1).setDesc(desc);
        facilities.addActivityFacility(fac1);

        // write

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(500);
        new FacilitiesWriter(facilities).write(outputStream);

        // read

        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        facilities = scenario.getActivityFacilities();
        MatsimFacilitiesReader reader = new MatsimFacilitiesReader(scenario);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        reader.parse(inputStream);

        // check

        Assertions.assertEquals(1, facilities.getFacilities().size());
        ActivityFacility fac1b = facilities.getFacilities().get(Id.create(1, ActivityFacility.class));
        String desc2 = ((ActivityFacilityImpl) fac1b).getDesc();
        Assertions.assertEquals(desc, desc2);
    }

	 // inspired by https://github.com/matsim-org/matsim/pull/505
	 @Test
	 void testFacilitiesName() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        ActivityFacilities facilities = scenario.getActivityFacilities();
        ActivityFacilitiesFactory factory = facilities.getFactory();

        String desc = "Some special text & that could pose <problems> to \"html\' or { json }.";

        facilities.setName(desc);

        // write

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(500);
        new FacilitiesWriter(facilities).write(outputStream);

        // read

        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        facilities = scenario.getActivityFacilities();
        MatsimFacilitiesReader reader = new MatsimFacilitiesReader(scenario);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        reader.parse(inputStream);

        // check

        String desc2 = facilities.getName();
        Assertions.assertEquals(desc, desc2);
    }

}
