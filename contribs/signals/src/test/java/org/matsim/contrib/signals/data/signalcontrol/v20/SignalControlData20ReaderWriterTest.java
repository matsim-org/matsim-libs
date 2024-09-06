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

import jakarta.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemControllerData;
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

	private static final Logger log = LogManager.getLogger(SignalControlData20ReaderWriterTest.class);

	private static final String TESTXML = "testSignalControl_v2.0.xml";

	private Id<SignalSystem> systemId42 = Id.create("42", SignalSystem.class);

	private Id<SignalPlan> signalPlanId8 = Id.create("8", SignalPlan.class);

	private Id<SignalGroup> groupId23 = Id.create("23", SignalGroup.class);

	private Id<SignalSystem> systemId43 = Id.create("43", SignalSystem.class);

//	private Id id24 = new IdImpl("24");

//	private Id id5 = new IdImpl("5");

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void testReader() throws JAXBException, SAXException, ParserConfigurationException, IOException{
		SignalControlData controlData = new SignalControlDataImpl();
		SignalControlReader20 reader = new SignalControlReader20(controlData);
		reader.readFile(this.testUtils.getPackageInputDirectory() + TESTXML);
		checkContent(controlData);
	}

	@Test
	void testWriter() throws JAXBException, SAXException, ParserConfigurationException, IOException {
  	String testoutput = this.testUtils.getOutputDirectory()  + "testSignalControlOutput.xml";
  	log.debug("reading file...");
  	//read the test file
		SignalControlData controlData = new SignalControlDataImpl();
		SignalControlReader20 reader = new SignalControlReader20(controlData);
		reader.readFile(this.testUtils.getPackageInputDirectory() + TESTXML);

  	//write the test file
  	log.debug("write the test file...");
  	SignalControlWriter20 writer = new SignalControlWriter20(controlData);
  	writer.write(testoutput);

  	log.debug("and read it again");
		controlData = new SignalControlDataImpl();
		reader = new SignalControlReader20(controlData);
		reader.readFile(testoutput);
  	checkContent(controlData);
  }




	private void checkContent(SignalControlData controlData) {
		Assertions.assertNotNull(controlData);
		Assertions.assertEquals(2, controlData.getSignalSystemControllerDataBySystemId().size());

		//first controller
		SignalSystemControllerData systemController = controlData.getSignalSystemControllerDataBySystemId().get(systemId42);
		Assertions.assertNotNull(systemController);
		Assertions.assertNotNull(systemController.getControllerIdentifier());
		Assertions.assertEquals("DefaultPlanbasedSignalSystemController", systemController.getControllerIdentifier());
		Assertions.assertNotNull(systemController.getSignalPlanData());
		SignalPlanData plan = systemController.getSignalPlanData().get(signalPlanId8);
		Assertions.assertNotNull(plan);
		double startTime = plan.getStartTime();
		Assertions.assertEquals(0.0, startTime, MatsimTestUtils.EPSILON);
		double stopTime = plan.getEndTime();
		Assertions.assertEquals(0.0, stopTime, MatsimTestUtils.EPSILON);
		Integer cycleTime = plan.getCycleTime();
		Assertions.assertNotNull(cycleTime);
		Assertions.assertEquals(Integer.valueOf(60), cycleTime);
		Assertions.assertEquals(3, plan.getOffset());

		Assertions.assertNotNull(plan.getSignalGroupSettingsDataByGroupId());
		SignalGroupSettingsData signalGroupSettings = plan.getSignalGroupSettingsDataByGroupId().get(groupId23);
		Assertions.assertNotNull(signalGroupSettings);
		Assertions.assertEquals(groupId23, signalGroupSettings.getSignalGroupId());
		Assertions.assertNotNull(signalGroupSettings.getOnset());
		Assertions.assertEquals(0, signalGroupSettings.getOnset());
		Assertions.assertNotNull(signalGroupSettings.getDropping());
		Assertions.assertEquals(45, signalGroupSettings.getDropping());

		//second controller
		systemController = controlData.getSignalSystemControllerDataBySystemId().get(systemId43);
		Assertions.assertNotNull(systemController);
		Assertions.assertNotNull(systemController.getControllerIdentifier());
		Assertions.assertEquals("logicbasedActuatedController", systemController.getControllerIdentifier());
		Assertions.assertNull(systemController.getSignalPlanData());
	}

}
