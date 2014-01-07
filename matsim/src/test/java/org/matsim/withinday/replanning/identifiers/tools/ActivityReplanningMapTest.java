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

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.withinday.events.ReplanningEvent;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.mobsim.WithinDayQSimFactory;

public class ActivityReplanningMapTest extends MatsimTestCase {

	/**
	 * @author cdobler
	 */
	public void testScenarioRun() {

		// load config and use ParallelQSim with 2 Threads
		Config config = loadConfig("test/scenarios/equil/config.xml");
		QSimConfigGroup qSimConfig = config.qsim();
		qSimConfig.setNumberOfThreads(2);
		config.controler().setMobsim("qsim");
		config.controler().setLastIteration(0);

		Controler controler = new Controler(config);
		WithinDayEngine withinDayEngine = new WithinDayEngine(controler.getEvents());
		withinDayEngine.initializeReplanningModules(2);
		controler.setMobsimFactory(new WithinDayQSimFactory(withinDayEngine));
		ControlerListenerForTests listener = new ControlerListenerForTests(withinDayEngine);
		controler.addControlerListener(listener);
		controler.setCreateGraphs(false);
		controler.setDumpDataAtEnd(false);
		controler.getConfig().controler().setWriteEventsInterval(0);
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

		private final WithinDayEngine withinDayEngine;
		
		public ControlerListenerForTests(WithinDayEngine withinDayEngine) {
			this.withinDayEngine = withinDayEngine;
		}
		
		@Override
		public void notifyStartup(final StartupEvent event) {
			MobsimDataProvider mobsimDataProvider = new MobsimDataProvider();
			ActivityReplanningMap arp = new ActivityReplanningMap(mobsimDataProvider);
			event.getControler().getEvents().addHandler(arp);
			MobsimListenerForTests listener = new MobsimListenerForTests(arp, withinDayEngine);
			FixedOrderSimulationListener fosl = new FixedOrderSimulationListener();
			fosl.addSimulationListener(mobsimDataProvider);
			fosl.addSimulationListener(arp);
			fosl.addSimulationListener(listener);
			event.getControler().getMobsimListeners().add(fosl);
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
		private final WithinDayEngine withinDayEngine;
		private final Map<Id, MobsimAgent> agents;
		private final WithinDayAgentUtils withinDayAgentUtils;
		private static final int t1 = 5*3600 + 58*60 + 30;
		private static final int t2 = 5*3600 + 59*60;
		private static final int t3 = 5*3600 + 59*60 + 30;
		private static final int t4 = 6*3600;
		private static final int t5 = 6*3600 + 60;
		private static final int t6 = 6*3600 + 120;
		
		public MobsimListenerForTests(final ActivityReplanningMap arp, WithinDayEngine withinDayEngine) {
			this.arp = arp;
			this.withinDayEngine = withinDayEngine;
			this.agents = new LinkedHashMap<Id, MobsimAgent>();
			this.withinDayAgentUtils = new WithinDayAgentUtils();
		}

		@Override
		public void notifyMobsimInitialized(final MobsimInitializedEvent e) {
			assertEquals(100, this.arp.getActivityPerformingAgents(0.0).size());	// all agents perform an activity
			assertEquals(0, this.arp.getActivityEndingAgents(0.0).size());		// no agent ends an activity
			
			QSim sim = (QSim) e.getQueueSimulation();
			for (MobsimAgent agent : sim.getAgents()) this.agents.put(agent.getId(), agent);
		}

		@Override
		public void notifyMobsimBeforeSimStep(final MobsimBeforeSimStepEvent e) {
			if (e.getSimulationTime() == t1) {
				assertEquals(100, this.arp.getActivityPerformingAgents(e.getSimulationTime()).size());	// all agents perform an activity before the time step
				assertEquals(1, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());	// one agent ends an activity
			}

			if (e.getSimulationTime() == t2) {
				assertEquals(99, this.arp.getActivityPerformingAgents(e.getSimulationTime()).size());	// 99 agents perform an activity before the time step
				assertEquals(1, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());	// one agent ends an activity
			}

			if (e.getSimulationTime() == t3) {
				assertEquals(98, this.arp.getActivityPerformingAgents(e.getSimulationTime()).size());	// 98 agents perform an activity before the time step
				assertEquals(1, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());	// one agent ends an activity
			}

			if (e.getSimulationTime() == t4) {
				assertEquals(97, this.arp.getActivityPerformingAgents(e.getSimulationTime()).size());	// 97 agents perform an activity before the time step
				assertEquals(97, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());	// 97 agents end an activity
								
				// now reschedule the activity end time of an agent
				MobsimAgent agent = this.agents.get(new IdImpl("40"));
				PlanAgent planAgent = (PlanAgent) agent;
				Activity currentActivity = (Activity) planAgent.getCurrentPlanElement();
				currentActivity.setEndTime(e.getSimulationTime() + 60);
				this.withinDayAgentUtils.resetCaches(agent);
				this.withinDayEngine.getInternalInterface().rescheduleActivityEnd(agent);
				((QSim) e.getQueueSimulation()).getEventsManager().processEvent(new ReplanningEvent(e.getSimulationTime(), agent.getId(), "ActivityRescheduler"));
				
				// reschedule a second time to check what happens if the agent is replanned multiple times in one time step
				currentActivity.setEndTime(e.getSimulationTime() + 120);
				this.withinDayAgentUtils.resetCaches(agent);
				this.withinDayEngine.getInternalInterface().rescheduleActivityEnd(agent);
				((QSim) e.getQueueSimulation()).getEventsManager().processEvent(new ReplanningEvent(e.getSimulationTime(), agent.getId(), "ActivityRescheduler"));
			}
			
			if (e.getSimulationTime() == t5) {
				assertEquals(1, this.arp.getActivityPerformingAgents(e.getSimulationTime()).size());	// one agent performs an activity before the time step
				assertEquals(0, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());	// no agent ends an activity
			}
			
			if (e.getSimulationTime() == t6) {
				assertEquals(1, this.arp.getActivityPerformingAgents(e.getSimulationTime()).size());	// one agent performs an activity before the time step
				assertEquals(1, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());	// one agent ends an activity
			}

		}

		@Override
		public void notifyMobsimAfterSimStep(final MobsimAfterSimStepEvent e) {
			if (e.getSimulationTime() == t1) {
				assertEquals(99, this.arp.getActivityPerformingAgents(e.getSimulationTime()).size());	// 99 agents perform an activity after the time step
				assertEquals(0, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());		// no agent ends an activity
			}

			if (e.getSimulationTime() == t2) {
				assertEquals(98, this.arp.getActivityPerformingAgents(e.getSimulationTime()).size());	// 98 agents perform an activity after the time step
				assertEquals(0, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());		// no agent ends an activity
			}

			if (e.getSimulationTime() == t3) {
				assertEquals(97, this.arp.getActivityPerformingAgents(e.getSimulationTime()).size());	// 97 agents perform an activity after the time step
				assertEquals(0, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());		// no agent ends an activity
			}

			if (e.getSimulationTime() == t4) {
				assertEquals(1, this.arp.getActivityPerformingAgents(e.getSimulationTime()).size());	// one agents perform an activity after the time step
				assertEquals(0, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());		// no agent ends an activity
			}
			
			if (e.getSimulationTime() == t5) {
				assertEquals(1, this.arp.getActivityPerformingAgents(e.getSimulationTime()).size());	// one agents perform an activity after the time step
				assertEquals(0, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());		// no agent ends an activity
			}
			
			if (e.getSimulationTime() == t6) {
				assertEquals(0, this.arp.getActivityPerformingAgents(e.getSimulationTime()).size());	// no agents perform an activity after the time step
				assertEquals(0, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());		// no agent ends an activity
			}
		}
	}
}
