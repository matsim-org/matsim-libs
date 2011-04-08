/* *********************************************************************** *
 * project: org.matsim.*
 * LinkReplanningMapTest.java
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

package org.matsim.withinday.replanning.identifiers.tools;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.testcases.MatsimTestCase;

public class LinkReplanningMapTest extends MatsimTestCase {

	/**
	 * @author cdobler
	 */
	public void testScenarioRun() {

		// load config and use ParallelQSim with 2 Threads
		Config config = loadConfig("test/scenarios/equil/config.xml");
		QSimConfigGroup qSimConfig = new QSimConfigGroup();
		qSimConfig.setNumberOfThreads(2);
		config.addQSimConfigGroup(qSimConfig);
		config.controler().setMobsim("qsim");
		config.controler().setLastIteration(0);

		Controler controler = new Controler(config);
		ControlerListenerForTests listener = new ControlerListenerForTests();
		controler.addControlerListener(listener);
		controler.run();
	}

	/**
	 * A ControllerListener that creates and registers a LinkReplanningMap
	 * and a MobsimListenerForTests which executes the test cases.
	 *
	 * @author cdobler
	 */
	private static class ControlerListenerForTests implements StartupListener {

		@Override
		public void notifyStartup(final StartupEvent event) {
			LinkReplanningMap lrp = new LinkReplanningMap();
			event.getControler().getEvents().addHandler(lrp);
			MobsimListenerForTests listener = new MobsimListenerForTests(lrp);
			FixedOrderSimulationListener fosl = new FixedOrderSimulationListener();
			fosl.addSimulationListener(lrp);
			fosl.addSimulationListener(listener);
			event.getControler().getQueueSimulationListener().add(fosl);
		}
	}

	/**
	 * Check Agent counts before and after a time step.
	 *
	 * @author cdobler
	 */
	private static class MobsimListenerForTests implements SimulationInitializedListener, SimulationBeforeSimStepListener,
		SimulationAfterSimStepListener {

		private final LinkReplanningMap lrp;
		private static final int t1 = 5*3600 + 58*60 + 30;
		private static final int t2 = 5*3600 + 59*60;
		private static final int t3 = 5*3600 + 59*60 + 30;
		private static final int t4 = 6*3600;

		public MobsimListenerForTests(final LinkReplanningMap lrp) {
			this.lrp = lrp;
		}

		@Override
		public void notifySimulationInitialized(final SimulationInitializedEvent e) {
			assertEquals(0, this.lrp.getLegPerformingAgents().size());	// no agent performs a Leg
			assertEquals(0, this.lrp.getReplanningAgents(0.0).size());	// no agent needs a replanning
		}

		@Override
		public void notifySimulationBeforeSimStep(final SimulationBeforeSimStepEvent e) {

			if (e.getSimulationTime() == t1) {
				assertEquals(0, this.lrp.getLegPerformingAgents().size());	// no agent performs a Leg
				assertEquals(0, this.lrp.getReplanningAgents(e.getSimulationTime()).size());	// no agent needs a replanning
			}

			if (e.getSimulationTime() == t2) {
				assertEquals(1, this.lrp.getLegPerformingAgents().size());	// one performs a Leg
				assertEquals(0, this.lrp.getReplanningAgents(e.getSimulationTime()).size());	// no agent needs a replanning
			}

			if (e.getSimulationTime() == t3) {
				assertEquals(2, this.lrp.getLegPerformingAgents().size());	// two agents perform a Leg
				assertEquals(0, this.lrp.getReplanningAgents(e.getSimulationTime()).size());	// no agent needs a replanning
			}

			if (e.getSimulationTime() == t4) {
				assertEquals(3, this.lrp.getLegPerformingAgents().size());	// three agents perform a Leg
				assertEquals(0, this.lrp.getReplanningAgents(e.getSimulationTime()).size());	// no agent needs a replanning
			}
		}

		@Override
		public void notifySimulationAfterSimStep(final SimulationAfterSimStepEvent e) {
			if (e.getSimulationTime() == t1) {
				assertEquals(1, this.lrp.getLegPerformingAgents().size());	// one agent performs a Leg
				assertEquals(1, this.lrp.getReplanningAgents(e.getSimulationTime()).size());	// one agent has just departed and might do a replanning
			}

			if (e.getSimulationTime() == t2) {
				assertEquals(2, this.lrp.getLegPerformingAgents().size());	// two agents perform a Leg
				assertEquals(1, this.lrp.getReplanningAgents(e.getSimulationTime()).size());	// one agent has just departed and might do a replanning
			}

			if (e.getSimulationTime() == t3) {
				assertEquals(3, this.lrp.getLegPerformingAgents().size());	// three agents perform a Leg
				assertEquals(1, this.lrp.getReplanningAgents(e.getSimulationTime()).size());	// one agent has just departed and might do a replanning
			}

			if (e.getSimulationTime() == t4) {
				assertEquals(100, this.lrp.getLegPerformingAgents().size());	// all agents  perform a Leg
				assertEquals(97, this.lrp.getReplanningAgents(e.getSimulationTime()).size());	// 97 agents have just departed and might do a replanning
			}
		}
	}
}
