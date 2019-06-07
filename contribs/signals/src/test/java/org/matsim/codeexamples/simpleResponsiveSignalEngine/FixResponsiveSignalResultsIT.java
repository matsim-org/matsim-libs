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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.testcases.MatsimTestUtils;


/**
 * @author tthunig
 *
 */
public class FixResponsiveSignalResultsIT {

	private static final Logger LOG = Logger.getLogger(FixResponsiveSignalResultsIT.class);
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public void testOneCrossingExample() {
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
		Assert.assertEquals(group1Setting.getOnset(), 0);
		Assert.assertEquals(group1Setting.getDropping(), 25);
		Assert.assertEquals(group2Setting.getOnset(), 30);
		Assert.assertEquals(group2Setting.getDropping(), 55);
	}
	
}
