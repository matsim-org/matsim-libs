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

	public void testReader11() {
		LaneDefinitions laneDefs = new LaneDefinitionsImpl();
		MatsimLaneDefinitionsReader reader = new MatsimLaneDefinitionsReader(laneDefs);
		reader.readFile(this.getClassInputDirectory() + TESTXMLV11);
		checkContent(laneDefs);
	}

	public void testReader20() {
		LaneDefinitions laneDefs = new LaneDefinitionsImpl();
		MatsimLaneDefinitionsReader reader = new MatsimLaneDefinitionsReader(laneDefs);
		reader.readFile(this.getClassInputDirectory() + TESTXMLV20);
		checkContent(laneDefs);
	}
	
	public void testWriter20() {
		String testoutput = this.getOutputDirectory() + "testLaneDefinitions2.0out.xml.gz";
		log.debug("reading file...");
		// read the test file
		LaneDefinitions laneDefs = new LaneDefinitionsImpl();
		MatsimLaneDefinitionsReader reader = new MatsimLaneDefinitionsReader(
				laneDefs);
		reader.readFile(this.getClassInputDirectory() + TESTXMLV20);

		// write the test file
		log.debug("write the test file...");
		MatsimLaneDefinitionsWriter writer = new MatsimLaneDefinitionsWriter(laneDefs);
		writer.writeFile(testoutput);

		log.debug("and read it again");
		laneDefs = new LaneDefinitionsImpl();
		reader = new MatsimLaneDefinitionsReader(
				laneDefs);
		reader.readFile(testoutput);
		checkContent(laneDefs);
	}
	

	
	public void testWriter11() {
		String testoutput = this.getOutputDirectory() + "testLssOutput.xml.gz";
		log.debug("reading file...");
		// read the test file
		LaneDefinitions laneDefs = new LaneDefinitionsImpl();
		MatsimLaneDefinitionsReader reader = new MatsimLaneDefinitionsReader(
				laneDefs);
		reader.readFile(this.getClassInputDirectory() + TESTXMLV11);

		// write the test file
		log.debug("write the test file...");
		MatsimLaneDefinitionsWriter writer = new MatsimLaneDefinitionsWriter(laneDefs);
		writer.writeFile(testoutput);

		log.debug("and read it again");
		laneDefs = new LaneDefinitionsImpl();
		reader = new MatsimLaneDefinitionsReader(
				laneDefs);
		reader.readFile(testoutput);
		checkContent(laneDefs);
	}

	private void checkContent(LaneDefinitions lanedefs) {
		assertEquals(2, lanedefs.getLanesToLinkAssignments().size());
		LanesToLinkAssignment l2la;
		List<LanesToLinkAssignment> assignments = new ArrayList<LanesToLinkAssignment>();
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
