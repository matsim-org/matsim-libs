/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityReplanningMapTest.java
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

public class ActivityReplanningMapTest extends MatsimTestCase {

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
	 * A ControllerListener that creates and registers an ActivityReplanningMap
	 * and a MobsimListenerForTests which executes the test cases.
	 *
	 * @author cdobler
	 */
	private static class ControlerListenerForTests implements StartupListener {

		@Override
		public void notifyStartup(final StartupEvent event) {
			ActivityReplanningMap arp = new ActivityReplanningMap(event.getControler().getEvents());
			MobsimListenerForTests listener = new MobsimListenerForTests(arp);
			FixedOrderSimulationListener fosl = new FixedOrderSimulationListener();
			fosl.addSimulationListener(arp);
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

		private final ActivityReplanningMap arp;
		private static final int t1 = 5*3600 + 58*60 + 30;
		private static final int t2 = 5*3600 + 59*60;
		private static final int t3 = 5*3600 + 59*60 + 30;
		private static final int t4 = 6*3600;

		public MobsimListenerForTests(final ActivityReplanningMap arp) {
			this.arp = arp;
		}

		@Override
		public void notifySimulationInitialized(final SimulationInitializedEvent e) {
			assertEquals(100, this.arp.getActivityPerformingAgents().size());	// all agents perform an activity
			assertEquals(0, this.arp.getReplanningDriverAgents(0.0).size());		// no agent ends an activity
		}

		@Override
		public void notifySimulationBeforeSimStep(final SimulationBeforeSimStepEvent e) {
			if (e.getSimulationTime() == t1) {
				assertEquals(100, this.arp.getActivityPerformingAgents().size());	// all agents perform an activity before the time step
				assertEquals(1, this.arp.getReplanningDriverAgents(e.getSimulationTime()).size());	// one agent ends an activity
			}

			if (e.getSimulationTime() == t2) {
				assertEquals(99, this.arp.getActivityPerformingAgents().size());	// 99 agents perform an activity before the time step
				assertEquals(1, this.arp.getReplanningDriverAgents(e.getSimulationTime()).size());	// one agent ends an activity
			}

			if (e.getSimulationTime() == t3) {
				assertEquals(98, this.arp.getActivityPerformingAgents().size());	// 98 agents perform an activity before the time step
				assertEquals(1, this.arp.getReplanningDriverAgents(e.getSimulationTime()).size());	// one agent ends an activity
			}

			if (e.getSimulationTime() == t4) {
				assertEquals(97, this.arp.getActivityPerformingAgents().size());	// 97 agents perform an activity before the time step
				assertEquals(97, this.arp.getReplanningDriverAgents(e.getSimulationTime()).size());	// 97 agents end an activity
			}
		}

		@Override
		public void notifySimulationAfterSimStep(final SimulationAfterSimStepEvent e) {
			if (e.getSimulationTime() == t1) {
				assertEquals(99, this.arp.getActivityPerformingAgents().size());	// 99 agents perform an activity after the time step
				assertEquals(0, this.arp.getReplanningDriverAgents(e.getSimulationTime()).size());		// no agent ends an activity
			}

			if (e.getSimulationTime() == t2) {
				assertEquals(98, this.arp.getActivityPerformingAgents().size());	// 98 agents perform an activity after the time step
				assertEquals(0, this.arp.getReplanningDriverAgents(e.getSimulationTime()).size());		// no agent ends an activity
			}

			if (e.getSimulationTime() == t3) {
				assertEquals(97, this.arp.getActivityPerformingAgents().size());	// 97 agents perform an activity after the time step
				assertEquals(0, this.arp.getReplanningDriverAgents(e.getSimulationTime()).size());		// no agent ends an activity
			}

			if (e.getSimulationTime() == t4) {
				assertEquals(0, this.arp.getActivityPerformingAgents().size());	// no agents perform an activity after the time step
				assertEquals(0, this.arp.getReplanningDriverAgents(e.getSimulationTime()).size());		// no agent ends an activity
			}
		}
	}
}
