/* *********************************************************************** *
 * project: org.matsim.*
 * SignalControlData20ReaderWriterTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.data.signalcontrol.v20;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.MatsimSignalSystemsReader;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 * 
 */
public class SignalControlData20ReaderWriterTest {

	private static final Logger log = Logger.getLogger(SignalControlData20ReaderWriterTest.class);

	private static final String TESTXML = "testSignalControl_v2.0.xml";

	private Id<SignalSystem> systemId42 = Id.create("42", SignalSystem.class);
	
	private Id<SignalPlan> signalPlanId8 = Id.create("8", SignalPlan.class);
	
	private Id<SignalGroup> groupId23 = Id.create("23", SignalGroup.class);
	
	private Id<SignalSystem> systemId43 = Id.create("43", SignalSystem.class);
	
//	private Id id24 = new IdImpl("24");

//	private Id id5 = new IdImpl("5");

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public void testReader() throws JAXBException, SAXException, ParserConfigurationException, IOException{
		SignalControlData controlData = new SignalControlDataImpl();
		SignalControlReader20 reader = new SignalControlReader20(controlData, MatsimSignalSystemsReader.SIGNALCONTROL20);
		reader.readFile(this.testUtils.getPackageInputDirectory() + TESTXML);
		checkContent(controlData);
	}
	
  @Test
  public void testWriter() throws JAXBException, SAXException, ParserConfigurationException, IOException {
  	String testoutput = this.testUtils.getOutputDirectory()  + "testSignalControlOutput.xml";
  	log.debug("reading file...");
  	//read the test file
		SignalControlData controlData = new SignalControlDataImpl();
		SignalControlReader20 reader = new SignalControlReader20(controlData, MatsimSignalSystemsReader.SIGNALCONTROL20);
		reader.readFile(this.testUtils.getPackageInputDirectory() + TESTXML);
  	
  	//write the test file
  	log.debug("write the test file...");
  	SignalControlWriter20 writer = new SignalControlWriter20(controlData);
  	writer.write(testoutput);
  	
  	log.debug("and read it again");
		controlData = new SignalControlDataImpl();
		reader = new SignalControlReader20(controlData, MatsimSignalSystemsReader.SIGNALCONTROL20);
		reader.readFile(testoutput);
  	checkContent(controlData);
  }

	
	
	
	private void checkContent(SignalControlData controlData) {
		Assert.assertNotNull(controlData);
		Assert.assertEquals(2, controlData.getSignalSystemControllerDataBySystemId().size());
		
		//first controller
		SignalSystemControllerData systemController = controlData.getSignalSystemControllerDataBySystemId().get(systemId42);
		Assert.assertNotNull(systemController);
		Assert.assertNotNull(systemController.getControllerIdentifier());
		Assert.assertEquals("DefaultPlanbasedSignalSystemController", systemController.getControllerIdentifier());
		Assert.assertNotNull(systemController.getSignalPlanData());
		SignalPlanData plan = systemController.getSignalPlanData().get(signalPlanId8);
		Assert.assertNotNull(plan);
		Double startTime = plan.getStartTime();
		Assert.assertNotNull(startTime);
		Assert.assertEquals(Double.valueOf(0.0), startTime);
		Double stopTime = plan.getEndTime();
		Assert.assertNotNull(stopTime);
		Assert.assertEquals(Double.valueOf(0.0), stopTime);
		Integer cycleTime = plan.getCycleTime();
		Assert.assertNotNull(cycleTime);
		Assert.assertEquals(Integer.valueOf(60), cycleTime);
		Assert.assertEquals(Integer.valueOf(3), plan.getOffset());
		
		Assert.assertNotNull(plan.getSignalGroupSettingsDataByGroupId());
		SignalGroupSettingsData signalGroupSettings = plan.getSignalGroupSettingsDataByGroupId().get(groupId23);
		Assert.assertNotNull(signalGroupSettings);
		Assert.assertEquals(groupId23, signalGroupSettings.getSignalGroupId());
		Assert.assertNotNull(signalGroupSettings.getOnset());
		Assert.assertEquals(0, signalGroupSettings.getOnset());
		Assert.assertNotNull(signalGroupSettings.getDropping());
		Assert.assertEquals(45, signalGroupSettings.getDropping());
		
		//second controller
		systemController = controlData.getSignalSystemControllerDataBySystemId().get(systemId43);
		Assert.assertNotNull(systemController);
		Assert.assertNotNull(systemController.getControllerIdentifier());
		Assert.assertEquals("logicbasedActuatedController", systemController.getControllerIdentifier());
		Assert.assertNull(systemController.getSignalPlanData());
	}

}
