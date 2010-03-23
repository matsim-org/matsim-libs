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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vis.otfvis.executables.OTFEvent2MVI;

/**
 * Simple test case to ensure the converting from eventsfile to .mvi-file
 * 
 * @author yu
 * 
 */
public class OTFVisTest extends MatsimTestCase {
	public void testConvert() {
		String networkFilename = "examples/equil/network.xml", // 
		eventsFilename = getInputDirectory() + "events.txt.gz", //
		mviFilename = getOutputDirectory() + "events.mvi";

		Scenario scenario = new ScenarioImpl();
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		new OTFEvent2MVI(new QSim(scenario, new EventsManagerImpl())
				.getQNetwork(), eventsFilename, mviFilename, 300/* snapshotPeriod */)
				.convert();
		assertNotNull(0);
	}
}
