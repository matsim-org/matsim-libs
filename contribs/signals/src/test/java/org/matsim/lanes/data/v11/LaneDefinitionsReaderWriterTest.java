/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * LaneDefinitionsReaderWriterTest.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package org.matsim.lanes.data.v11;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.v20.*;
import org.matsim.testcases.MatsimTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the reader and writer for the different lane formats
 * @author dgrether
 *
 */
public class LaneDefinitionsReaderWriterTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(LaneDefinitionsReaderWriterTest.class);

	private static final String TESTXMLV11 = "testLaneDefinitions_v1.1.xml";

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
	
	public void testReader11() {
		Fixture f = new Fixture();
		LaneDefinitions11Impl lanedefs11 = new LaneDefinitions11Impl();
		LaneDefinitionsReader11 reader = new LaneDefinitionsReader11(lanedefs11);
		reader.readFile(this.getClassInputDirectory() + TESTXMLV11);
		checkContentV11(lanedefs11);
	}

	public void testWriter11() {
		String testoutput = this.getOutputDirectory() + "testLssOutput.xml.gz";
		log.debug("reading file...");
		// read the test file
		LaneDefinitions11Impl lanedefs11 = new LaneDefinitions11Impl();
		LaneDefinitionsReader11 reader = new LaneDefinitionsReader11(lanedefs11);
		reader.readFile(this.getClassInputDirectory() + TESTXMLV11);

		// write the test file
		log.debug("write the test file...");
		LaneDefinitionsWriter11 writerDelegate = new LaneDefinitionsWriter11(lanedefs11);
		writerDelegate.write(testoutput);

		log.debug("and read it again");
		lanedefs11 = new LaneDefinitions11Impl();
		reader = new LaneDefinitionsReader11(lanedefs11);
		reader.readFile(testoutput);
		checkContentV11(lanedefs11);
	}

	private void checkContentV11(LaneDefinitions11 lanedefs) {
		assertEquals(2, lanedefs.getLanesToLinkAssignments().size());
		org.matsim.lanes.data.v11.LanesToLinkAssignment11 l2la;
		List<LanesToLinkAssignment11> assignments = new ArrayList<LanesToLinkAssignment11>();
		assignments.addAll(lanedefs.getLanesToLinkAssignments().values());
		l2la = assignments.get(0);
		assertNotNull(l2la);
		assertEquals(linkId23, l2la.getLinkId());
		List<LaneData11> lanes = new ArrayList<LaneData11>();
		lanes.addAll(l2la.getLanes().values());
		LaneData11 lane = lanes.get(0);
		assertEquals(laneId3, lane.getId());
		assertEquals(linkId1, lane.getToLinkIds().get(0));
		assertEquals(45.0, lane.getStartsAtMeterFromLinkEnd(), EPSILON);
		assertEquals(1.0, lane.getNumberOfRepresentedLanes());
		lane = lanes.get(1);
		assertEquals(laneId5, lane.getId());
		assertEquals(60.0, lane.getStartsAtMeterFromLinkEnd(), EPSILON);
		assertEquals(2.5, lane.getNumberOfRepresentedLanes());
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
		assertEquals(1.0, lane.getNumberOfRepresentedLanes());
	}

}
