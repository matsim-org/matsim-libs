/* *********************************************************************** *
 * project: org.matsim.*
 * EquilTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.examples;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

@RunWith(Parameterized.class)
public class EquilTest extends MatsimTestCase {
	
	@Rule public MatsimTestUtils helper = new MatsimTestUtils();

	private final boolean isUsingFastCapacityUpdate;

	public EquilTest(boolean isUsingFastCapacityUpdate) {
		this.isUsingFastCapacityUpdate = isUsingFastCapacityUpdate;
	}

	@Parameters(name = "{index}: isUsingfastCapacityUpdate == {0}")
	public static Collection<Object> parameterObjects () {
		Object [] capacityUpdates = new Object [] { false, true };
		return Arrays.asList(capacityUpdates);
	}

	@Test
	public void testEquil() {
		Config c = loadConfig(null);
		
		c.qsim().setUsingFastCapacityUpdate(this.isUsingFastCapacityUpdate);
		
		String netFileName = "test/scenarios/equil/network.xml";
		String popFileName = "test/scenarios/equil/plans100.xml";

		String referenceFileName ;
		
		if(this.isUsingFastCapacityUpdate) {
			System.out.println(helper.getInputDirectory());
			referenceFileName = helper.getInputDirectory() + "events_fastCapacityUpdate.xml.gz";
		} else {
			System.out.println(helper.getInputDirectory());
			referenceFileName = helper.getInputDirectory() + "events.xml.gz";
		}
		
		String eventsFileName = getOutputDirectory() + "events.xml.gz";

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(c);

		new MatsimNetworkReader(scenario.getNetwork()).readFile(netFileName);

		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(popFileName);

		EventsManager events = EventsUtils.createEventsManager();
		EventWriterXML writer = new EventWriterXML(eventsFileName);
		events.addHandler(writer);

		//		SimulationTimer.setTime(0); // I don't think this is needed. kai, may'10
		Mobsim sim = QSimUtils.createDefaultQSim(scenario, events);
		sim.run();

		writer.closeFile();

		assertEquals("different event files.", EventsFileComparator.compare(referenceFileName, eventsFileName), 0);
	}
}
