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

package org.matsim.signalsystems.data.signalsystems.v20;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.signalsystems.MatsimSignalSystemsReader;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsDataImpl;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsReader20;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsWriter20;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;

/**
 * Test case for the readers and writers for the (light-)signalSystems_v1.*.xsd file
 * format.
 * @author dgrether
 */
public class SignalSystemsData20ReaderWriterTest {
	
	private static final Logger log = Logger
			.getLogger(SignalSystemsData20ReaderWriterTest.class);
	
  private static final String TESTXML  = "testSignalSystems_v2.0.xml";
  
  @Rule  public MatsimTestUtils testUtils = new MatsimTestUtils();
  
  private Id id1 = new IdImpl("1");
  
  private Id id2 = new IdImpl("2");
  
  private Id id3 = new IdImpl("3");
  
  private Id id4 = new IdImpl("4");
  
  @Test
  public void testParser() throws IOException, JAXBException, SAXException, ParserConfigurationException {
  	SignalSystemsData lss = new SignalSystemsDataImpl();
  	SignalSystemsReader20 reader = new SignalSystemsReader20(lss, MatsimSignalSystemsReader.SIGNALSYSTEMS20);
  	reader.readFile(this.testUtils.getPackageInputDirectory() + TESTXML);
  	
  	checkContent(lss);
  }
  @Test
  public void testWriter() throws JAXBException, SAXException, ParserConfigurationException, IOException {
  	String testoutput = this.testUtils.getOutputDirectory()  + "testLssOutput.xml";
  	log.debug("reading file...");
  	//read the test file
  	SignalSystemsData lss = new SignalSystemsDataImpl();
  	SignalSystemsReader20 reader = new SignalSystemsReader20(lss, MatsimSignalSystemsReader.SIGNALSYSTEMS20);
  	reader.readFile(this.testUtils.getPackageInputDirectory() + TESTXML);
  	
  	//write the test file
  	log.debug("write the test file...");
  	SignalSystemsWriter20 writer = new SignalSystemsWriter20(lss);
  	writer.write(testoutput);
  	
  	log.debug("and read it again");
  	lss = new SignalSystemsDataImpl();
  	reader = new SignalSystemsReader20(lss, MatsimSignalSystemsReader.SIGNALSYSTEMS20);
  	reader.readFile(testoutput);
  	checkContent(lss);
  }

  private void checkContent(SignalSystemsData ss) {
  	//system id 1
  	SignalSystemData ssdata = ss.getSignalSystemData().get(id1);
  	Assert.assertNotNull(ssdata);
  	Assert.assertEquals(2, ssdata.getSignalData().size());
  	
  	SignalData signaldata = ssdata.getSignalData().get(id1);
  	Assert.assertNotNull(signaldata);
  	Assert.assertEquals(id1, signaldata.getId());
  	Assert.assertEquals(id1, signaldata.getLinkId());
  	Assert.assertNull(signaldata.getLaneIds());
  	Assert.assertNull(signaldata.getTurningMoveRestrictions());
  	
  	signaldata = ssdata.getSignalData().get(id2);
  	Assert.assertNotNull(signaldata);
  	Assert.assertEquals(id2, signaldata.getId());
  	Assert.assertEquals(id2, signaldata.getLinkId());
  	Assert.assertNotNull(signaldata.getTurningMoveRestrictions());
  	Assert.assertEquals(1, signaldata.getTurningMoveRestrictions().size());
  	Assert.assertEquals(id3, signaldata.getTurningMoveRestrictions().iterator().next());
  	Assert.assertNull(signaldata.getLaneIds());
  	
  	//system id 2 
  	ssdata = ss.getSignalSystemData().get(id2);
  	Assert.assertNotNull(ssdata);

  	signaldata = ssdata.getSignalData().get(id1);
  	Assert.assertNotNull(signaldata);
  	Assert.assertEquals(id1, signaldata.getId());
  	Assert.assertEquals(id3, signaldata.getLinkId());
  	Assert.assertNotNull(signaldata.getLaneIds());
  	Assert.assertEquals(id1, signaldata.getLaneIds().iterator().next());
  	Assert.assertNull(signaldata.getTurningMoveRestrictions());

  	signaldata = ssdata.getSignalData().get(id2);
  	Assert.assertNotNull(signaldata);
  	Assert.assertEquals(id2, signaldata.getId());
  	Assert.assertEquals(id4, signaldata.getLinkId());
  	Assert.assertNotNull(signaldata.getLaneIds());
  	Assert.assertEquals(2, signaldata.getLaneIds().size());
  	Assert.assertTrue(signaldata.getLaneIds().contains(id1));
  	Assert.assertTrue(signaldata.getLaneIds().contains(id2));
  	Assert.assertNotNull(signaldata.getTurningMoveRestrictions());
  	Assert.assertEquals(1, signaldata.getTurningMoveRestrictions().size());
  	Assert.assertTrue(signaldata.getTurningMoveRestrictions().contains(id3));
  	
  }

}
