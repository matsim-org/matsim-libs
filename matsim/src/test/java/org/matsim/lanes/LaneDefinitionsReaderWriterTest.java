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
package org.matsim.lanes;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.MatsimLaneDefinitionsReader;
import org.matsim.lanes.data.MatsimLaneDefinitionsWriter;
import org.matsim.lanes.data.v11.Lane;
import org.matsim.lanes.data.v11.LaneDefinitions;
import org.matsim.lanes.data.v20.LaneData20;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;
import org.matsim.testcases.MatsimTestCase;

/**
 * Tests the reader and writer for the different lane formats
 * @author dgrether
 *
 */
public class LaneDefinitionsReaderWriterTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(LaneDefinitionsReaderWriterTest.class);

	private static final String TESTXMLV11 = "testLaneDefinitions_v1.1.xml";

	private static final String TESTXMLV20 = "testLaneDefinitions_v2.0.xml";

	private Id id1 = new IdImpl("1");

	private Id id3 = new IdImpl("3");

	private Id id5 = new IdImpl("5");

	private Id id23 = new IdImpl("23");

	private Id id42 = new IdImpl("42");

	private static final class Fixture{
		ScenarioImpl scenario;

		Fixture(){
			Config config = ConfigUtils.createConfig();
			config.scenario().setUseLanes(true);
			this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		}
	}
	
	public void testReader11() {
		Fixture f = new Fixture();
		MatsimLaneDefinitionsReader reader = new MatsimLaneDefinitionsReader(f.scenario);
		reader.readFile(this.getClassInputDirectory() + TESTXMLV11);
		checkContentV11(f.scenario.getLaneDefinitions11());
	}

	public void testReader20() {
		Fixture f = new Fixture();
		MatsimLaneDefinitionsReader reader = new MatsimLaneDefinitionsReader(f.scenario);
		reader.readFile(this.getClassInputDirectory() + TESTXMLV20);
		checkContent(f.scenario.getScenarioElement(LaneDefinitions20.class));
	}
	
	public void testWriter20() {
		Fixture f = new Fixture();
		String testoutput = this.getOutputDirectory() + "testLaneDefinitions2.0out.xml.gz";
		log.debug("reading file...");
		// read the test file
		MatsimLaneDefinitionsReader reader = new MatsimLaneDefinitionsReader(
				f.scenario);
		reader.readFile(this.getClassInputDirectory() + TESTXMLV20);

		// write the test file
		log.debug("write the test file...");
		MatsimLaneDefinitionsWriter writer = new MatsimLaneDefinitionsWriter();
		writer.writeFile20(testoutput, f.scenario.getScenarioElement(LaneDefinitions20.class));

		f = new Fixture();
		log.debug("and read it again");
		reader = new MatsimLaneDefinitionsReader(
				f.scenario);
		reader.readFile(testoutput);
		checkContent(f.scenario.getScenarioElement(LaneDefinitions20.class));
	}
	

	
	public void testWriter11() {
		Fixture f = new Fixture();
		String testoutput = this.getOutputDirectory() + "testLssOutput.xml.gz";
		log.debug("reading file...");
		// read the test file
		MatsimLaneDefinitionsReader reader = new MatsimLaneDefinitionsReader(
				f.scenario);
		reader.readFile(this.getClassInputDirectory() + TESTXMLV11);

		// write the test file
		log.debug("write the test file...");
		MatsimLaneDefinitionsWriter writer = new MatsimLaneDefinitionsWriter();
		writer.writeFile11(testoutput, f.scenario.getLaneDefinitions11());

		log.debug("and read it again");
		reader = new MatsimLaneDefinitionsReader(
				f.scenario);
		reader.readFile(testoutput);
		checkContentV11(f.scenario.getLaneDefinitions11());
	}

	private void checkContent(LaneDefinitions20 lanedefs) {
		assertEquals(2, lanedefs.getLanesToLinkAssignments().size());
		LanesToLinkAssignment20 l2la;
		List<LanesToLinkAssignment20> assignments = new ArrayList<LanesToLinkAssignment20>();
		assignments.addAll(lanedefs.getLanesToLinkAssignments().values());
		l2la = assignments.get(0);
		assertNotNull(l2la);
		assertEquals(id23, l2la.getLinkId());
		List<LaneData20> lanes = new ArrayList<LaneData20>();
		lanes.addAll(l2la.getLanes().values());
		LaneData20 lane = lanes.get(0);
		assertEquals(id3, lane.getId());
		assertEquals(id1, lane.getToLinkIds().get(0));
		assertEquals(45.0, lane.getStartsAtMeterFromLinkEnd(), EPSILON);
		assertEquals(1.0, lane.getNumberOfRepresentedLanes());
		assertEquals(0.725, lane.getCapacityVehiclesPerHour(), EPSILON);
		lane = lanes.get(1);
		assertEquals(id5, lane.getId());
		assertEquals(60.0, lane.getStartsAtMeterFromLinkEnd(), EPSILON);
		assertEquals(2.5, lane.getNumberOfRepresentedLanes());
		assertEquals(2, lane.getCapacityVehiclesPerHour(), EPSILON);
		//check a lanes2linkassignment using default values
		l2la = assignments.get(1);
		assertNotNull(l2la);
		assertEquals(id42, l2la.getLinkId());
		lanes.clear();
		lanes.addAll(l2la.getLanes().values());
		lane = lanes.get(0);
		assertEquals(id1, lane.getId());
		assertEquals(id1, lane.getToLinkIds().get(0));
		assertEquals(45.0, lane.getStartsAtMeterFromLinkEnd(), EPSILON);
		assertEquals(1900.0, lane.getCapacityVehiclesPerHour(), EPSILON);
		assertEquals(1.0, lane.getNumberOfRepresentedLanes());
	}
	
	private void checkContentV11(LaneDefinitions lanedefs) {
		assertEquals(2, lanedefs.getLanesToLinkAssignments().size());
		org.matsim.lanes.data.v11.LanesToLinkAssignment l2la;
		List<org.matsim.lanes.data.v11.LanesToLinkAssignment> assignments = new ArrayList<org.matsim.lanes.data.v11.LanesToLinkAssignment>();
		assignments.addAll(lanedefs.getLanesToLinkAssignments().values());
		l2la = assignments.get(0);
		assertNotNull(l2la);
		assertEquals(id23, l2la.getLinkId());
		List<Lane> lanes = new ArrayList<Lane>();
		lanes.addAll(l2la.getLanes().values());
		Lane lane = lanes.get(0);
		assertEquals(id3, lane.getId());
		assertEquals(id1, lane.getToLinkIds().get(0));
		assertEquals(45.0, lane.getStartsAtMeterFromLinkEnd(), EPSILON);
		assertEquals(1.0, lane.getNumberOfRepresentedLanes());
		lane = lanes.get(1);
		assertEquals(id5, lane.getId());
		assertEquals(60.0, lane.getStartsAtMeterFromLinkEnd(), EPSILON);
		assertEquals(2.5, lane.getNumberOfRepresentedLanes());
		//check a lanes2linkassignment using default values
		l2la = assignments.get(1);
		assertNotNull(l2la);
		assertEquals(id42, l2la.getLinkId());
		lanes.clear();
		lanes.addAll(l2la.getLanes().values());
		lane = lanes.get(0);
		assertEquals(id1, lane.getId());
		assertEquals(id1, lane.getToLinkIds().get(0));
		assertEquals(45.0, lane.getStartsAtMeterFromLinkEnd(), EPSILON);
		assertEquals(1.0, lane.getNumberOfRepresentedLanes());
	}

}
