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
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.mobsim.WithinDayQSimFactory;

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
		WithinDayEngine withinDayEngine = new WithinDayEngine(controler.getEvents());
		withinDayEngine.initializeReplanningModules(2);
		controler.setMobsimFactory(new WithinDayQSimFactory(withinDayEngine));
		ControlerListenerForTests listener = new ControlerListenerForTests();
		controler.addControlerListener(listener);
		controler.setCreateGraphs(false);
		controler.setDumpDataAtEnd(false);
		controler.setWriteEventsInterval(0);
		controler.getConfig().controler().setWritePlansInterval(0);
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
			ActivityReplanningMap arp = new ActivityReplanningMap();
			event.getControler().getEvents().addHandler(arp);
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
	private static class MobsimListenerForTests implements MobsimInitializedListener, MobsimBeforeSimStepListener,
		MobsimAfterSimStepListener {

		private final ActivityReplanningMap arp;
		private static final int t1 = 5*3600 + 58*60 + 30;
		private static final int t2 = 5*3600 + 59*60;
		private static final int t3 = 5*3600 + 59*60 + 30;
		private static final int t4 = 6*3600;

		public MobsimListenerForTests(final ActivityReplanningMap arp) {
			this.arp = arp;
		}

		@Override
		public void notifyMobsimInitialized(final MobsimInitializedEvent e) {
			assertEquals(100, this.arp.getActivityPerformingAgents().size());	// all agents perform an activity
			assertEquals(0, this.arp.getActivityEndingAgents(0.0).size());		// no agent ends an activity
		}

		@Override
		public void notifyMobsimBeforeSimStep(final MobsimBeforeSimStepEvent e) {
			if (e.getSimulationTime() == t1) {
				assertEquals(100, this.arp.getActivityPerformingAgents().size());	// all agents perform an activity before the time step
				assertEquals(1, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());	// one agent ends an activity
			}

			if (e.getSimulationTime() == t2) {
				assertEquals(99, this.arp.getActivityPerformingAgents().size());	// 99 agents perform an activity before the time step
				assertEquals(1, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());	// one agent ends an activity
			}

			if (e.getSimulationTime() == t3) {
				assertEquals(98, this.arp.getActivityPerformingAgents().size());	// 98 agents perform an activity before the time step
				assertEquals(1, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());	// one agent ends an activity
			}

			if (e.getSimulationTime() == t4) {
				assertEquals(97, this.arp.getActivityPerformingAgents().size());	// 97 agents perform an activity before the time step
				assertEquals(97, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());	// 97 agents end an activity
			}
		}

		@Override
		public void notifyMobsimAfterSimStep(final MobsimAfterSimStepEvent e) {
			if (e.getSimulationTime() == t1) {
				assertEquals(99, this.arp.getActivityPerformingAgents().size());	// 99 agents perform an activity after the time step
				assertEquals(0, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());		// no agent ends an activity
			}

			if (e.getSimulationTime() == t2) {
				assertEquals(98, this.arp.getActivityPerformingAgents().size());	// 98 agents perform an activity after the time step
				assertEquals(0, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());		// no agent ends an activity
			}

			if (e.getSimulationTime() == t3) {
				assertEquals(97, this.arp.getActivityPerformingAgents().size());	// 97 agents perform an activity after the time step
				assertEquals(0, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());		// no agent ends an activity
			}

			if (e.getSimulationTime() == t4) {
				assertEquals(0, this.arp.getActivityPerformingAgents().size());	// no agents perform an activity after the time step
				assertEquals(0, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());		// no agent ends an activity
			}
		}
	}
}
