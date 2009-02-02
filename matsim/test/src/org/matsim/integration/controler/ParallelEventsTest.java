/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelEventsTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.integration.controler;

import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.events.Events;
import org.matsim.events.parallelEventsHandler.ParallelEvents;
import org.matsim.testcases.MatsimTestCase;

/**
 * Tests the integration of {@link ParallelEvents} into the {@link Controler}
 *
 * @author mrieser
 */
public class ParallelEventsTest extends MatsimTestCase {

	public void testDefaultWithoutConfig() {
		final Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(-1); // disable running any iteration
		final Controler controler = new Controler(config);
		controler.run();
		Events events = controler.getEvents();
		assertEquals(org.matsim.events.Events.class, events.getClass());
	}

	public void testLoadParallelWithConfig() {
		final Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(-1); // disable running any iteration
		config.setParam("parallelEventHandling", "numberOfThreads", "3");
		final Controler controler = new Controler(config);
		controler.run();
		Events events = controler.getEvents();
		assertEquals(org.matsim.events.parallelEventsHandler.ParallelEvents.class, events.getClass());
	}

}
