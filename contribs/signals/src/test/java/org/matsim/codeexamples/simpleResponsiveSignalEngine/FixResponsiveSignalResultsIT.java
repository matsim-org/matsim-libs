/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
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
package org.matsim.codeexamples.simpleResponsiveSignalEngine;

import java.util.SortedMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.testcases.MatsimTestUtils;


/**
 * @author tthunig
 *
 */
public class FixResponsiveSignalResultsIT {

	private static final Logger LOG = LogManager.getLogger(FixResponsiveSignalResultsIT.class);

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void testOneCrossingExample() {
		LOG.info("Fix the results from the simple one-crossing-example in RunSimpleResponsiveSignalExample.");
		RunSimpleResponsiveSignalExample responsiveSignal = new RunSimpleResponsiveSignalExample();
		responsiveSignal.run();

		SignalsData signalsData = (SignalsData) responsiveSignal.getControler().getScenario().getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemControllerData signalControlSystem1 = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId()
				.get(Id.create("SignalSystem1", SignalSystem.class));
		SignalPlanData signalPlan = signalControlSystem1.getSignalPlanData().get(Id.create("SignalPlan1", SignalPlan.class));
		SortedMap<Id<SignalGroup>, SignalGroupSettingsData> signalGroupSettings = signalPlan.getSignalGroupSettingsDataByGroupId();
		SignalGroupSettingsData group1Setting = signalGroupSettings.get(Id.create("SignalGroup1", SignalGroup.class));
		SignalGroupSettingsData group2Setting = signalGroupSettings.get(Id.create("SignalGroup2", SignalGroup.class));

		LOG.info("SignalGroup1: onset " + group1Setting.getOnset() + ", dropping " + group1Setting.getDropping());
		LOG.info("SignalGroup2: onset " + group2Setting.getOnset() + ", dropping " + group2Setting.getDropping());
		Assertions.assertEquals(0, group1Setting.getOnset());
		Assertions.assertEquals(25, group1Setting.getDropping());
		Assertions.assertEquals(30, group2Setting.getOnset());
		Assertions.assertEquals(55, group2Setting.getDropping());
	}

}
