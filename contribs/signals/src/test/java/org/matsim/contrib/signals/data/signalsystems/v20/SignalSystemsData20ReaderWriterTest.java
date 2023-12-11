/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.contrib.signals.data.signalsystems.v20;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.lanes.Lane;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;

/**
 * Test case for the readers and writers for the signalSystems_v2.0.xsd file
 * format.
 * @author dgrether
 */
public class SignalSystemsData20ReaderWriterTest {

	private static final Logger log = LogManager.getLogger(SignalSystemsData20ReaderWriterTest.class);

  private static final String TESTXML  = "testSignalSystems_v2.0.xml";

  @RegisterExtension public MatsimTestUtils testUtils = new MatsimTestUtils();

  private Id<SignalSystem> systemId1 = Id.create("1", SignalSystem.class);
  private Id<Signal> signalId1 = Id.create("1", Signal.class);
  private Id<Lane> laneId1 = Id.create("1", Lane.class);
  private Id<Link> linkId1 = Id.create("1", Link.class);

  private Id<SignalSystem> systemId2 = Id.create("2", SignalSystem.class);
  private Id<Signal> signalId2 = Id.create("2", Signal.class);
  private Id<Link> linkId2 = Id.create("2", Link.class);
  private Id<Lane> laneId2 = Id.create("2", Lane.class);

  private Id<Signal> signalId3 = Id.create("3", Signal.class);
  private Id<Link> linkId3 = Id.create("3", Link.class);

  private Id<Link> linkId4 = Id.create("4", Link.class);

	@Test
	void testParser() throws IOException, JAXBException, SAXException, ParserConfigurationException {
  	SignalSystemsData lss = new SignalSystemsDataImpl();
  	SignalSystemsReader20 reader = new SignalSystemsReader20(lss);
  	reader.readFile(this.testUtils.getPackageInputDirectory() + TESTXML);

  	checkContent(lss);
  }

	@Test
	void testWriter() throws JAXBException, SAXException, ParserConfigurationException, IOException {
  	String testoutput = this.testUtils.getOutputDirectory()  + "testLssOutput.xml";
  	log.debug("reading file...");
  	//read the test file
  	SignalSystemsData lss = new SignalSystemsDataImpl();
  	SignalSystemsReader20 reader = new SignalSystemsReader20(lss);
  	reader.readFile(this.testUtils.getPackageInputDirectory() + TESTXML);

  	//write the test file
  	log.debug("write the test file...");
  	SignalSystemsWriter20 writer = new SignalSystemsWriter20(lss);
  	writer.write(testoutput);

  	log.debug("and read it again");
  	lss = new SignalSystemsDataImpl();
  	reader = new SignalSystemsReader20(lss);
  	reader.readFile(testoutput);
  	checkContent(lss);
  }

  private void checkContent(SignalSystemsData ss) {
  	//system id 1
  	SignalSystemData ssdata = ss.getSignalSystemData().get(systemId1);
  	Assertions.assertNotNull(ssdata);
  	Assertions.assertEquals(2, ssdata.getSignalData().size());

  	SignalData signaldata = ssdata.getSignalData().get(signalId1);
  	Assertions.assertNotNull(signaldata);
  	Assertions.assertEquals(signalId1, signaldata.getId());
  	Assertions.assertEquals(linkId1, signaldata.getLinkId());
  	Assertions.assertNull(signaldata.getLaneIds());
  	Assertions.assertNull(signaldata.getTurningMoveRestrictions());

  	signaldata = ssdata.getSignalData().get(signalId2);
  	Assertions.assertNotNull(signaldata);
  	Assertions.assertEquals(signalId2, signaldata.getId());
  	Assertions.assertEquals(linkId2, signaldata.getLinkId());
  	Assertions.assertNotNull(signaldata.getTurningMoveRestrictions());
  	Assertions.assertEquals(1, signaldata.getTurningMoveRestrictions().size());
  	Assertions.assertEquals(linkId3, signaldata.getTurningMoveRestrictions().iterator().next());
  	Assertions.assertNull(signaldata.getLaneIds());

  	//system id 2
  	ssdata = ss.getSignalSystemData().get(systemId2);
  	Assertions.assertNotNull(ssdata);

  	signaldata = ssdata.getSignalData().get(signalId1);
  	Assertions.assertNotNull(signaldata);
  	Assertions.assertEquals(signalId1, signaldata.getId());
  	Assertions.assertEquals(linkId3, signaldata.getLinkId());
  	Assertions.assertNotNull(signaldata.getLaneIds());
  	Assertions.assertEquals(laneId1, signaldata.getLaneIds().iterator().next());
  	Assertions.assertNull(signaldata.getTurningMoveRestrictions());

  	signaldata = ssdata.getSignalData().get(signalId2);
  	Assertions.assertNotNull(signaldata);
  	Assertions.assertEquals(signalId2, signaldata.getId());
  	Assertions.assertEquals(linkId4, signaldata.getLinkId());
  	Assertions.assertNotNull(signaldata.getLaneIds());
  	Assertions.assertEquals(2, signaldata.getLaneIds().size());
  	Assertions.assertTrue(signaldata.getLaneIds().contains(laneId1));
  	Assertions.assertTrue(signaldata.getLaneIds().contains(laneId2));
  	Assertions.assertNotNull(signaldata.getTurningMoveRestrictions());
  	Assertions.assertEquals(1, signaldata.getTurningMoveRestrictions().size());
  	Assertions.assertTrue(signaldata.getTurningMoveRestrictions().contains(linkId3));

  }

}
