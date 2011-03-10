/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayQSimFactoryTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.withinday.mobsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.DefaultAgentFactory;
import org.matsim.ptproject.qsim.qnetsimengine.QSimEngineImpl;
import org.matsim.testcases.MatsimTestCase;

public class WithinDayQSimFactoryTest extends MatsimTestCase {
				
	/**
	 * @author cdobler
	 */
	public void testCreateMobsim() {
		
				Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		EventsManager eventsManager = new EventsManagerImpl();

		QSimConfigGroup qSimConfig = new QSimConfigGroup();
		scenario.getConfig().addQSimConfigGroup(qSimConfig);
				
		QSim sim = null;
		
		// number of threads is 1, therefore we expect a non-parallel WithinDayQSim
		qSimConfig.setNumberOfThreads(1);
		sim = new WithinDayQSimFactory().createMobsim(scenario, eventsManager);
		assertTrue(sim.getNetsimEngine().getClass().equals(QSimEngineImpl.class));
		assertTrue(sim.getEventsManager().getClass().equals(EventsManagerImpl.class));
		
		// number of threads is 1, therefore we expect a parallel WithinDayQSim
		qSimConfig.setNumberOfThreads(2);
		sim = new WithinDayQSimFactory().createMobsim(scenario, eventsManager);
		assertFalse(sim.getNetsimEngine().getClass().equals(QSimEngineImpl.class));
		assertTrue(sim.getEventsManager().getClass().equals(SynchronizedEventsManagerImpl.class));
	}
}