/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayQSimTest.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.AgentFactory;
import org.matsim.ptproject.qsim.agents.DefaultAgentFactory;
import org.matsim.testcases.MatsimTestCase;

public class WithinDayQSimTest extends MatsimTestCase {
	
	static private final Logger log = Logger.getLogger(WithinDayQSimTest.class);
			
	/**
	 * @author cdobler
	 */
	public void testSetAgentFactory() {
		
		Scenario scenario = new ScenarioImpl();
		EventsManager eventsManager = new EventsManagerImpl();

		QSimConfigGroup qSimConfig = new QSimConfigGroup();
		scenario.getConfig().addQSimConfigGroup(qSimConfig);
		
		QSim sim = new WithinDayQSim(scenario, eventsManager);
	
		// using a DefaultAgentFactory should cause a RuntimeException
		try {
			AgentFactory factory = new DefaultAgentFactory(sim);
			sim.setAgentFactory(factory);
			fail("expected RuntimeException");
		} catch (RuntimeException e) {
			log.debug("catched expected exception", e);
		}

		// using a WithinDayAgentFactory should be fine
		sim.setAgentFactory(new WithinDayAgentFactory(sim));
	}
}