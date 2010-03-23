/* *********************************************************************** *
 * project: org.matsim.*
 * OTFVisTest.java
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

/**
 * 
 */
package org.matsim.run;

import java.io.File;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.executables.OTFEvent2MVI;

/**
 * Simple test case to ensure the converting from eventsfile to .mvi-file
 * Needs somehow a bunch of memory - please use "-Xmx630m"!
 * 
 * @author yu
 * 
 */
public class OTFVisTest {
  
  @Rule public MatsimTestUtils utils = new MatsimTestUtils();
  
  @Test
  public void testConvert() {
		String networkFilename = "examples/equil/network.xml", // 
		eventsFilename = utils.getInputDirectory() + "events.txt.gz", //
		mviFilename = utils.getOutputDirectory() + "events.mvi";

		Scenario scenario = new ScenarioImpl();
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		new OTFEvent2MVI(new QSim(scenario, new EventsManagerImpl())
				.getQNetwork(), eventsFilename, mviFilename, 300/* snapshotPeriod */)
				.convert();
		File f = new File(mviFilename);
		Assert.assertTrue("No mvi file written!", f.exists());
	}
}
