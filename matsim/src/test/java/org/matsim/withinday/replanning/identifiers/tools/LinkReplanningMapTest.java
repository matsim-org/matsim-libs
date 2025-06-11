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

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.withinday.controller.WithinDayModule;

/**
 * @author cdobler
 */
public class LinkReplanningMapTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testScenarioRun() {

		// load config and use ParallelQSim with 2 Threads
		Config config = utils.loadConfig("test/scenarios/equil/config.xml");
		QSimConfigGroup qSimConfig = config.qsim();
		qSimConfig.setNumberOfThreads(2);
		qSimConfig.setFlowCapFactor(100.0);	// ensure that agents don't have to wait at an intersection
		qSimConfig.setStorageCapFactor(100.0);	// ensure that agents don't have to wait at an intersection
		config.controller().setMobsim("qsim");
		config.controller().setLastIteration(0);
		config.controller().setRoutingAlgorithmType( ControllerConfigGroup.RoutingAlgorithmType.Dijkstra );

		Controler controler = new Controler(config);
		controler.addOverridingModule(new WithinDayModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addMobsimListenerBinding().to(MobsimListenerForTests.class);
			}
		});
		controler.run();
	}

	/**
	 * Check Agent counts before and after a time step.
	 *
	 * @author cdobler
	 */
	private static class MobsimListenerForTests implements MobsimInitializedListener, MobsimBeforeSimStepListener,
		MobsimAfterSimStepListener {

		private final LinkReplanningMap lrp;
		private static final int t1 = 5*3600 + 58*60 + 30;
		private static final int t2 = 5*3600 + 59*60;
		private static final int t3 = 5*3600 + 59*60 + 30;
		private static final int t4 = 6*3600;
		private static final int linkTravelTime = 360;

		@Inject
		MobsimListenerForTests(final LinkReplanningMap lrp) {
			this.lrp = lrp;
		}

		@Override
		public void notifyMobsimInitialized(final MobsimInitializedEvent e) {
			assertEquals(0, this.lrp.getLegPerformingAgents().size());	// no agent performs a Leg
			assertEquals(0, this.lrp.getReplanningAgents(0.0).size());	// no agent needs a replanning
		}

		@Override
		public void notifyMobsimBeforeSimStep(final MobsimBeforeSimStepEvent e) {

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
		public void notifyMobsimAfterSimStep(final MobsimAfterSimStepEvent e) {
			if (e.getSimulationTime() == t1) {
				assertEquals(1, this.lrp.getLegPerformingAgents().size());	// one agent performs a Leg
				assertEquals(1, this.lrp.getReplanningAgents(e.getSimulationTime()).size());	// one agent has just departed but cannot do a replanning
			}
			// the agent hast moved to the next link
			if (e.getSimulationTime() == t1 + 1) {
				assertEquals(1, this.lrp.getUnrestrictedReplanningAgents(e.getSimulationTime()).size());
				assertEquals(0, this.lrp.getRestrictedReplanningAgents(e.getSimulationTime()).size());
			}
			if (e.getSimulationTime() == t1 + linkTravelTime) {
				assertEquals(1, this.lrp.getReplanningAgents(e.getSimulationTime()).size());	// one agent could leave the second link in its route and should be identified as to be replanned
			}

			if (e.getSimulationTime() == t2) {
				assertEquals(2, this.lrp.getLegPerformingAgents().size());	// two agents perform a Leg
				assertEquals(1, this.lrp.getReplanningAgents(e.getSimulationTime()).size());	// one agent has just departed but cannot do a replanning
			}
			if (e.getSimulationTime() == t2 + linkTravelTime) {
				assertEquals(1, this.lrp.getReplanningAgents(e.getSimulationTime()).size());	// one agent could leave the second link in its route and should be identified as to be replanned
			}

			if (e.getSimulationTime() == t3) {
				assertEquals(3, this.lrp.getLegPerformingAgents().size());	// three agents perform a Leg
				assertEquals(1, this.lrp.getReplanningAgents(e.getSimulationTime()).size());	// one agent has just departed and might do a replanning
			}

			if (e.getSimulationTime() == t4) {
				assertEquals(100, this.lrp.getLegPerformingAgents().size());	// all agents  perform a Leg
				assertEquals(97, this.lrp.getReplanningAgents(e.getSimulationTime()).size());	// 97 agents have just departed but cannot do a replanning
			}
			if (e.getSimulationTime() == t4 + linkTravelTime) {
				assertEquals(97, this.lrp.getReplanningAgents(e.getSimulationTime()).size());	// 97 agents could leave the second link in its route and should be identified as to be replanned
			}
		}
	}
}
