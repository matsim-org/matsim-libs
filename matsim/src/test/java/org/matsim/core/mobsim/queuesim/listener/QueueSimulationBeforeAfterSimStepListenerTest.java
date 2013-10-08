/* *********************************************************************** *
 * project: org.matsim.*
 * QueueSimulationAfterSimStepListenerTest.java
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

package org.matsim.core.mobsim.queuesim.listener;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;

public class QueueSimulationBeforeAfterSimStepListenerTest extends MatsimTestCase {

	public void testEventIsFired() {
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setMobsim(MobsimType.queueSimulation.toString());
		SimulationConfigGroup configGroup = new SimulationConfigGroup();
		configGroup.setStartTime(6.0 * 3600);
		configGroup.setEndTime(6.0 * 3600 + 10);
		config.addModule(configGroup);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		QueueSimulation qsim = new QueueSimulation(scenario, EventsUtils.createEventsManager());

		MockQueueSimStepListener mockListener = new MockQueueSimStepListener(1.0);
		qsim.addQueueSimulationListeners(mockListener);
		MockBeforeQueueSimStepListener beforeStepMockListener = new MockBeforeQueueSimStepListener(1.0);
		qsim.addQueueSimulationListeners(beforeStepMockListener);
		qsim.run();
		assertEquals("wrong number of invocations.", 11, mockListener.getCount());
		assertEquals("wrong number of invocations.", 11, beforeStepMockListener.getCount());

		// redo the test with different settings
		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setEndTime(6.0 * 3600 + 50);
		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setTimeStepSize(10.0);

		scenario = ScenarioUtils.loadScenario(config);
		qsim = new QueueSimulation(scenario, EventsUtils.createEventsManager());

		mockListener = new MockQueueSimStepListener(10.0);
		qsim.addQueueSimulationListeners(mockListener);
		beforeStepMockListener = new MockBeforeQueueSimStepListener(10.0);
		qsim.addQueueSimulationListeners(beforeStepMockListener);
		qsim.run();
		assertEquals("wrong number of invocations.", 6, mockListener.getCount());
		assertEquals("wrong number of invocations.", 6, beforeStepMockListener.getCount());
	}

	public static class MockBeforeQueueSimStepListener implements MobsimAfterSimStepListener {


		private static final Logger log = Logger
				.getLogger(QueueSimulationBeforeAfterSimStepListenerTest.MockBeforeQueueSimStepListener.class);

		private final double expectedTimeDiff;
		private double previousTime = -1.0;
		private int counter = 0;

		public MockBeforeQueueSimStepListener(final double expectedTimeDifference) {
			this.expectedTimeDiff = expectedTimeDifference;
		}

		@Override
		public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
			if (this.previousTime >= 0.0) {
				assertEquals("wrong time difference between two BeforeSimStepEvents.",
						this.expectedTimeDiff, e.getSimulationTime() - this.previousTime, EPSILON);
			}
			this.previousTime = e.getSimulationTime();
			log.info(this.previousTime);
			this.counter++;
		}

		public int getCount() {
			return this.counter;
		}
	}

	public static class MockQueueSimStepListener implements MobsimAfterSimStepListener {

		private static final Logger log = Logger
				.getLogger(QueueSimulationBeforeAfterSimStepListenerTest.MockQueueSimStepListener.class);

		private final double expectedTimeDiff;
		private double previousTime = -1.0;
		private int counter = 0;

		public MockQueueSimStepListener(final double expectedTimeDifference) {
			this.expectedTimeDiff = expectedTimeDifference;
		}

		@Override
		public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
			if (this.previousTime >= 0.0) {
				assertEquals("wrong time difference between two AfterSimStepEvents.",
						this.expectedTimeDiff, e.getSimulationTime() - this.previousTime, EPSILON);
			}
			this.previousTime = e.getSimulationTime();
			log.info(this.previousTime);
			this.counter++;
		}

		public int getCount() {
			return this.counter;
		}

	}
}
