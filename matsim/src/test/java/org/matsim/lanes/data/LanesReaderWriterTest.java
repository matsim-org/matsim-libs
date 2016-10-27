/* *********************************************************************** *
 * project: org.matsim.*
 * LaneDefinitionsReaderWriterTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.lanes.data;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesReader;
import org.matsim.lanes.data.LanesToLinkAssignment;
import org.matsim.lanes.data.LanesWriter;
import org.matsim.testcases.MatsimTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the reader and writer for lanes
 * @author dgrether
 *
 */
public class LanesReaderWriterTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(LanesReaderWriterTest.class);

	private static final String FILENAME = "testLanes.xml";

	private Id<Lane> laneId1 = Id.create("1", Lane.class);
	private Id<Link> linkId1 = Id.create("1", Link.class);

	private Id<Lane> laneId3 = Id.create("3", Lane.class);

	private Id<Lane> laneId5 = Id.create("5", Lane.class);

	private Id<Link> linkId23 = Id.create("23", Link.class);

	private Id<Link> linkId42 = Id.create("42", Link.class);

	private static final class Fixture{
		Scenario scenario;

		Fixture(){
			Config config = ConfigUtils.createConfig();
			config.qsim().setUseLanes(true);
			// need to LOAD the scenario in order for the lanes container to be created.
			this.scenario = ScenarioUtils.loadScenario(config);
		}
	}

	public void testReader20() {
		Fixture f = new Fixture();
		LanesReader reader = new LanesReader(f.scenario);
		reader.readFile(this.getClassInputDirectory() + FILENAME);
		checkContent(f.scenario.getLanes());
	}
	
	public void testWriter20() {
		Fixture f = new Fixture();
		String testoutput = this.getOutputDirectory() + "testLaneDefinitions2.0out.xml.gz";
		log.debug("reading file...");
		// read the test file
		LanesReader reader = new LanesReader(
				f.scenario);
		reader.readFile(this.getClassInputDirectory() + FILENAME);

		// write the test file
		log.debug("write the test file...");
		LanesWriter writerDelegate = new LanesWriter(f.scenario.getLanes());
		writerDelegate.write(testoutput);

		f = new Fixture();
		log.debug("and read it again");
		reader = new LanesReader(
				f.scenario);
		reader.readFile(testoutput);
		checkContent(f.scenario.getLanes());
	}

	private void checkContent(Lanes lanedefs) {
		assertEquals(2, lanedefs.getLanesToLinkAssignments().size());
		LanesToLinkAssignment l2la;
		List<LanesToLinkAssignment> assignments = new ArrayList<>();
		assignments.addAll(lanedefs.getLanesToLinkAssignments().values());
		l2la = assignments.get(0);
		assertNotNull(l2la);
		assertEquals(linkId23, l2la.getLinkId());
		List<Lane> lanes = new ArrayList<>();
		lanes.addAll(l2la.getLanes().values());
		Lane lane = lanes.get(0);
		assertEquals(laneId3, lane.getId());
		assertEquals(linkId1, lane.getToLinkIds().get(0));
		assertEquals(45.0, lane.getStartsAtMeterFromLinkEnd(), EPSILON);
		assertEquals(1.0, lane.getNumberOfRepresentedLanes());
		assertEquals(0.725, lane.getCapacityVehiclesPerHour(), EPSILON);
		lane = lanes.get(1);
		assertEquals(laneId5, lane.getId());
		assertEquals(60.0, lane.getStartsAtMeterFromLinkEnd(), EPSILON);
		assertEquals(2.5, lane.getNumberOfRepresentedLanes());
		assertEquals(2, lane.getCapacityVehiclesPerHour(), EPSILON);
		//check a lanes2linkassignment using default values
		l2la = assignments.get(1);
		assertNotNull(l2la);
		assertEquals(linkId42, l2la.getLinkId());
		lanes.clear();
		lanes.addAll(l2la.getLanes().values());
		lane = lanes.get(0);
		assertEquals(laneId1, lane.getId());
		assertEquals(linkId1, lane.getToLinkIds().get(0));
		assertEquals(45.0, lane.getStartsAtMeterFromLinkEnd(), EPSILON);
		assertEquals(1900.0, lane.getCapacityVehiclesPerHour(), EPSILON);
		assertEquals(1.0, lane.getNumberOfRepresentedLanes());
	}

}
