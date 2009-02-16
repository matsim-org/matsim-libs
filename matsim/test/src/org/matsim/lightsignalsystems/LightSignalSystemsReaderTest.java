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

package org.matsim.lightsignalsystems;

import org.matsim.basic.signalsystems.BasicLane;
import org.matsim.basic.signalsystems.BasicLanesToLinkAssignment;
import org.matsim.basic.signalsystems.BasicLightSignalGroupDefinition;
import org.matsim.basic.signalsystems.BasicLightSignalSystemDefinition;
import org.matsim.basic.signalsystems.BasicLightSignalSystems;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.signalsystems.MatsimLightSignalSystemsReader;
import org.matsim.signalsystems.MatsimLightSignalSystemsWriter;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 */
public class LightSignalSystemsReaderTest extends MatsimTestCase {

  private static final String TESTXML  = "testLightSignalSystems.xml";
  
  private Id id1 = new IdImpl("1");
  
  private Id id2 = new IdImpl("2");
  
  private Id id3 = new IdImpl("3");

  private Id id5 = new IdImpl("5");
  
  private Id id23 = new IdImpl("23");
  
  private Id id42 = new IdImpl("42");
  
  public void testParser() {
  	BasicLightSignalSystems lss = new BasicLightSignalSystems();
  	MatsimLightSignalSystemsReader reader = new MatsimLightSignalSystemsReader(lss);
  	reader.readFile(this.getPackageInputDirectory() + TESTXML);
  	
  	checkContent(lss);
  }
  
  public void testWriter() {
  	String testoutput = this.getOutputDirectory()  + "testLssOutput.xml";
  	//read the test file
  	BasicLightSignalSystems lss = new BasicLightSignalSystems();
  	MatsimLightSignalSystemsReader reader = new MatsimLightSignalSystemsReader(lss);
  	reader.readFile(this.getPackageInputDirectory() + TESTXML);

  	//write the test file
  	MatsimLightSignalSystemsWriter writer = new MatsimLightSignalSystemsWriter(lss);
  	writer.writeFile(testoutput);
  	
  	lss = new BasicLightSignalSystems();
  	reader = new MatsimLightSignalSystemsReader(lss);
  	reader.readFile(testoutput);
  	checkContent(lss);
  }

  private void checkContent(BasicLightSignalSystems lss) {
  	assertEquals(1, lss.getLanesToLinkAssignments().size());
  	assertEquals(2, lss.getLightSignalSystemDefinitions().size());
  	assertEquals(2, lss.getLightSignalGroupDefinitions().size());
  	BasicLanesToLinkAssignment l2la;
  	l2la = lss.getLanesToLinkAssignments().get(0);
  	assertNotNull(l2la);
  	assertEquals(id23, l2la.getLinkId());
  	BasicLane lane = l2la.getLanes().get(0);
  	assertEquals(id3, lane.getId());
  	assertEquals(id1, lane.getToLinkIds().get(0));
  	assertEquals(45.0, lane.getLength(), EPSILON);
  	assertEquals(1, lane.getNumberOfRepresentedLanes());
  	lane = l2la.getLanes().get(1);
  	assertEquals(id5, lane.getId());
  	assertEquals(60.0, lane.getLength(), EPSILON);
  	assertEquals(2, lane.getNumberOfRepresentedLanes());	
  	
  	BasicLightSignalSystemDefinition lssd;
  	lssd = lss.getLightSignalSystemDefinitions().get(0);
  	assertNotNull(lssd);
  	assertEquals(id23, lssd.getId());
  	assertEquals(60.0, lssd.getDefaultCirculationTime(), EPSILON);
  	assertEquals(5.0, lssd.getDefaultSyncronizationOffset(), EPSILON);
  	assertEquals(3.0, lssd.getDefaultInterimTime(), EPSILON);
  	
  	BasicLightSignalGroupDefinition lsgd;
  	lsgd = lss.getLightSignalGroupDefinitions().get(1);
  	assertNotNull(lsgd);
  	assertEquals(id42, lsgd.getLinkRefId());
  	assertEquals(id42, lsgd.getId());
  	assertEquals(id42, lsgd.getLightSignalSystemDefinitionId());
  	assertEquals(id5, lsgd.getLaneIds().get(0));
  	assertEquals(id1, lsgd.getToLinkIds().get(0));
  	assertEquals(id2, lsgd.getToLinkIds().get(1));
  }

}
