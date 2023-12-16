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

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.withinday.controller.WithinDayModule;
import org.matsim.withinday.events.ReplanningEvent;
import org.matsim.withinday.mobsim.WithinDayEngine;

public class ActivityReplanningMapTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testGetTimeBin() {
		ActivityReplanningMap arp = new ActivityReplanningMap(null, EventsUtils.createEventsManager());

		// test default setting with start time = 0.0 and time step size = 1.0
		arp.simStartTime = 0.0;
		arp.timeStepSize = 1.0;
		assertEquals(0, arp.getTimeBin(0.0));
		assertEquals(1, arp.getTimeBin(0.9));
		assertEquals(1, arp.getTimeBin(1.0));

		// test default setting with start time = 0.5 and time step size = 1.0
		arp.simStartTime = 0.5;
		arp.timeStepSize = 1.0;
		assertEquals(0, arp.getTimeBin(0.0));
		assertEquals(0, arp.getTimeBin(0.4));
		assertEquals(0, arp.getTimeBin(0.5));
		assertEquals(1, arp.getTimeBin(0.9));
		assertEquals(1, arp.getTimeBin(1.0));
		assertEquals(1, arp.getTimeBin(1.5));
		assertEquals(2, arp.getTimeBin(1.6));

		// test setting with start time = 10.0 and time step size = 1.0
		arp.simStartTime = 10.0;
		arp.timeStepSize =  1.0;
		assertEquals(0, arp.getTimeBin(0.0));
		assertEquals(0, arp.getTimeBin(10.0));
		assertEquals(1, arp.getTimeBin(10.9));
		assertEquals(1, arp.getTimeBin(11.0));

		// test setting with start time = 10.0 and time step size = 2.0
		arp.simStartTime = 10.0;
		arp.timeStepSize =  2.0;
		assertEquals(0, arp.getTimeBin(0.0));
		assertEquals(1, arp.getTimeBin(11.0));
		assertEquals(1, arp.getTimeBin(11.9));
		assertEquals(1, arp.getTimeBin(12.0));
		assertEquals(2, arp.getTimeBin(12.1));
	}

	@Test
	void testScenarioRun() {

		// load config and use ParallelQSim with 2 Threads
		Config config = utils.loadConfig("test/scenarios/equil/config.xml");
		QSimConfigGroup qSimConfig = config.qsim();
		qSimConfig.setNumberOfThreads(2);
		config.controller().setMobsim("qsim");
		config.controller().setLastIteration(0);
		config.qsim().setStartTime(0.0);
		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		config.controller().setRoutingAlgorithmType( ControllerConfigGroup.RoutingAlgorithmType.Dijkstra );

		Controler controler = new Controler(config);
		controler.addOverridingModule(new WithinDayModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addMobsimListenerBinding().to(MobsimListenerForTests.class);
			}
		});
        controler.getConfig().controller().setCreateGraphs(false);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.getConfig().controller().setWriteEventsInterval(0);
		controler.getConfig().controller().setWritePlansInterval(0);
		controler.run();
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
		private final Map<Id<Person>, MobsimAgent> agents;
		private static final int t1 = 5*3600 + 58*60 + 30;
		private static final int t2 = 5*3600 + 59*60;
		private static final int t3 = 5*3600 + 59*60 + 30;
		private static final int t4 = 6*3600;
		private static final int t5 = 6*3600 + 60;
		private static final int t6 = 6*3600 + 120;

		@Inject
		MobsimListenerForTests(final ActivityReplanningMap arp, WithinDayEngine withinDayEngine) {
			this.arp = arp;
			this.withinDayEngine = withinDayEngine;
			this.agents = new LinkedHashMap<>();
		}

		@Override
		public void notifyMobsimInitialized(final MobsimInitializedEvent e) {
			assertEquals(100, this.arp.getActivityPerformingAgents().size());	// all agents perform an activity
			assertEquals(0, this.arp.getActivityEndingAgents(0.0).size());		// no agent ends an activity

			QSim sim = (QSim) e.getQueueSimulation();
			for (MobsimAgent agent : sim.getAgents().values()) this.agents.put(agent.getId(), agent);
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

				// now reschedule the activity end time of an agent
				MobsimAgent agent = this.agents.get(Id.create("40", Person.class));
                Activity currentActivity = (Activity) WithinDayAgentUtils.getCurrentPlanElement(agent);
				currentActivity.setEndTime(e.getSimulationTime() + 60);
				WithinDayAgentUtils.resetCaches(agent);
				this.withinDayEngine.getActivityRescheduler().rescheduleActivityEnd(agent);
				((QSim) e.getQueueSimulation()).getEventsManager().processEvent(new ReplanningEvent(e.getSimulationTime(), agent.getId(), "ActivityRescheduler"));

				// reschedule a second time to check what happens if the agent is replanned multiple times in one time step
				currentActivity.setEndTime(e.getSimulationTime() + 120);
				WithinDayAgentUtils.resetCaches(agent);
				this.withinDayEngine.getActivityRescheduler().rescheduleActivityEnd(agent);
				((QSim) e.getQueueSimulation()).getEventsManager().processEvent(new ReplanningEvent(e.getSimulationTime(), agent.getId(), "ActivityRescheduler"));
			}

			if (e.getSimulationTime() == t5) {
				assertEquals(1, this.arp.getActivityPerformingAgents().size());	// one agent performs an activity before the time step
				assertEquals(0, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());	// no agent ends an activity
			}

			if (e.getSimulationTime() == t6) {
				assertEquals(1, this.arp.getActivityPerformingAgents().size());	// one agent performs an activity before the time step
				assertEquals(1, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());	// one agent ends an activity
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
				assertEquals(1, this.arp.getActivityPerformingAgents().size());	// one agents perform an activity after the time step
				assertEquals(0, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());		// no agent ends an activity
			}

			if (e.getSimulationTime() == t5) {
				assertEquals(1, this.arp.getActivityPerformingAgents().size());	// one agents perform an activity after the time step
				assertEquals(0, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());		// no agent ends an activity
			}

			if (e.getSimulationTime() == t6) {
				assertEquals(0, this.arp.getActivityPerformingAgents().size());	// no agents perform an activity after the time step
				assertEquals(0, this.arp.getActivityEndingAgents(e.getSimulationTime()).size());		// no agent ends an activity
			}
		}
	}
}
