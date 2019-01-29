/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerEventsTest.java
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

package org.matsim.core.controler;

import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.testcases.MatsimTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dgrether
 */
public class ControlerEventsTest extends MatsimTestCase {

	private List<Integer> calledStartupListener = null;

	void addCalledStartupListenerNumber(int i) {
		this.calledStartupListener.add(i);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.calledStartupListener = new ArrayList<>(3);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.calledStartupListener = null;
	}

	public void testCoreListenerExecutionOrder() {
		Config config = loadConfig(getClassInputDirectory() + "config.xml");

		TestController controler = new TestController(config);
		ControlerEventsTestListener firstListener = new ControlerEventsTestListener(1, this);
		ControlerEventsTestListener secondListener = new ControlerEventsTestListener(2, this);
		ControlerEventsTestListener thirdListener = new ControlerEventsTestListener(3, this);

		controler.addCoreControlerListener(firstListener);
		controler.addCoreControlerListener(secondListener);
		controler.addCoreControlerListener(thirdListener);
		controler.run(config);
		assertEquals(3, this.calledStartupListener.get(0).intValue());
		assertEquals(2, this.calledStartupListener.get(1).intValue());
		assertEquals(1, this.calledStartupListener.get(2).intValue());
	}

	public void testEvents() {
		Config config = loadConfig(getClassInputDirectory() + "config.xml");

		TestController controler = new TestController(config);
		ControlerEventsTestListener listener = new ControlerEventsTestListener(1, this);
		controler.addControlerListener(listener);
		controler.run(config);
		//test for startup events
		StartupEvent startup = listener.getStartupEvent();
		assertNotNull("No ControlerStartupEvent fired!", startup);
		//test for shutdown
		ShutdownEvent shutdown = listener.getShutdownEvent();
		assertNotNull("No ControlerShutdownEvent fired!", shutdown);
		//test for iterations
		//setup
		List<IterationStartsEvent> setupIt = listener.getIterationStartsEvents();
		assertEquals(1, setupIt.size());
		assertEquals(0, setupIt.get(0).getIteration());
		//shutdown
		List<IterationEndsEvent> finishIt = listener.getIterationEndsEvents();
		assertEquals(1, finishIt.size());
		assertEquals(0, finishIt.get(0).getIteration());
	}

	private static class TestController extends AbstractController {

		private final Config config;

		public TestController(Config config) {
			this.config = config;
			super.setupOutputDirectory(new OutputDirectoryHierarchy(config.controler()));
		}

		@Override
		protected void loadCoreListeners() {

		}

		@Override
		protected void runMobSim() {

		}

		@Override
		protected void prepareForSim() {

		}
		
		@Override
		protected void prepareForMobsim() {
		}
		
		@Override
		protected boolean continueIterations(int iteration) {
			return iteration <= config.controler().getLastIteration();
		}
	}

}
