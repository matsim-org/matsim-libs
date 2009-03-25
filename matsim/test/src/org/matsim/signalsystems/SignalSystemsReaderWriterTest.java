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

package org.matsim.signalsystems;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.basic.signalsystems.BasicSignalGroupDefinition;
import org.matsim.basic.signalsystems.BasicSignalSystemDefinition;
import org.matsim.basic.signalsystems.BasicSignalSystems;
import org.matsim.basic.signalsystems.BasicSignalSystemsImpl;
import org.matsim.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

/**
 * Test case for the readers and writers for the (light-)signalSystems_v1.*.xsd file
 * format.
 * @author dgrether
 */
public class SignalSystemsReaderWriterTest extends MatsimTestCase {
	
	private static final Logger log = Logger
			.getLogger(SignalSystemsReaderWriterTest.class);
	
  private static final String TESTXML  = "testSignalSystems_v1.1.xml";
  
  private Id id1 = new IdImpl("1");
  
  private Id id2 = new IdImpl("2");
  
  private Id id5 = new IdImpl("5");
  
  private Id id23 = new IdImpl("23");
  
  private Id id42 = new IdImpl("42");
  
  public void testParser() throws IOException {
  	BasicSignalSystems lss = new BasicSignalSystemsImpl();
  	MatsimSignalSystemsReader reader = new MatsimSignalSystemsReader(lss);
  	reader.readFile(this.getPackageInputDirectory() + TESTXML);
  	
  	checkContent(lss);
  }
  
  public void testWriter() {
  	String testoutput = this.getOutputDirectory()  + "testLssOutput.xml";
  	log.debug("reading file...");
  	//read the test file
  	BasicSignalSystems lss = new BasicSignalSystemsImpl();
  	MatsimSignalSystemsReader reader = new MatsimSignalSystemsReader(lss);
  	reader.readFile(this.getPackageInputDirectory() + TESTXML);
  	
  	
  	//write the test file
  	log.debug("write the test file...");
  	MatsimSignalSystemsWriter writer = new MatsimSignalSystemsWriter(lss);
  	writer.writeFile(testoutput);
  	
  	log.debug("and read it again");
  	lss = new BasicSignalSystemsImpl();
  	reader = new MatsimSignalSystemsReader(lss);
  	reader.readFile(testoutput);
  	checkContent(lss);
  }

  private void checkContent(BasicSignalSystems lss) {
  	BasicSignalSystemDefinition lssd;
  	lssd = lss.getSignalSystemDefinitions().get(0);
  	assertNotNull(lssd);
  	assertEquals(id23, lssd.getId());
  	assertEquals(60.0, lssd.getDefaultCirculationTime(), EPSILON);
  	assertEquals(5.0, lssd.getDefaultSyncronizationOffset(), EPSILON);
  	assertEquals(3.0, lssd.getDefaultInterimTime(), EPSILON);
  	
  	BasicSignalGroupDefinition lsgd;
  	lsgd = lss.getSignalGroupDefinitions().get(1);
  	assertNotNull(lsgd);
  	assertEquals(id42, lsgd.getLinkRefId());
  	assertEquals(id42, lsgd.getId());
  	assertEquals(id42, lsgd.getLightSignalSystemDefinitionId());
  	assertEquals(id5, lsgd.getLaneIds().get(0));
  	assertEquals(id1, lsgd.getToLinkIds().get(0));
  	assertEquals(id2, lsgd.getToLinkIds().get(1));
  }

}
