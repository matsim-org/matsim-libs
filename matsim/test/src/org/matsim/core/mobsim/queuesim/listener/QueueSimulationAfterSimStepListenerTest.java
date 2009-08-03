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

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationAfterSimStepEvent;
import org.matsim.core.scenario.ScenarioLoader;
import org.matsim.testcases.MatsimTestCase;

public class QueueSimulationAfterSimStepListenerTest extends MatsimTestCase {

	public void testEventIsFired() {
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.simulation().setStartTime(6.0 * 3600);
		config.simulation().setEndTime(6.0 * 3600 + 10);
		ScenarioImpl scenario = new ScenarioLoader(config).loadScenario();
		QueueSimulation qsim = new QueueSimulation(scenario, new Events());
		
		List<QueueSimulationListener> listeners = new LinkedList<QueueSimulationListener>();
		MockQueueSimStepListener mockListener = new MockQueueSimStepListener(1.0);
		listeners.add(mockListener);
		qsim.addQueueSimulationListeners(listeners);
		qsim.run();
		assertEquals("wrong number of invocations.", 11, mockListener.getCount());
		
		// redo the test with different settings
		config.simulation().setEndTime(6.0 * 3600 + 50);
		config.simulation().setTimeStepSize(10.0);
		Gbl.reset(); Gbl.setConfig(config); // reset...
		scenario = new ScenarioLoader(config).loadScenario();
		qsim = new QueueSimulation(scenario, new Events());
		
		listeners = new LinkedList<QueueSimulationListener>();
		mockListener = new MockQueueSimStepListener(10.0);
		listeners.add(mockListener);
		qsim.addQueueSimulationListeners(listeners);
		qsim.run();
		assertEquals("wrong number of invocations.", 6, mockListener.getCount());
	}
	
	public static class MockQueueSimStepListener implements QueueSimulationAfterSimStepListener {

		private final double expectedTimeDiff;
		private double previousTime = -1.0;
		private int counter = 0;
		
		public MockQueueSimStepListener(final double expectedTimeDifference) {
			this.expectedTimeDiff = expectedTimeDifference;
		}
		
		public void notifySimulationAfterSimStep(QueueSimulationAfterSimStepEvent e) {
			if (this.previousTime >= 0.0) {
				assertEquals("wrong time difference between two AfterSimStepEvents.", 
						this.expectedTimeDiff, e.getSimulationTime() - this.previousTime, EPSILON);
			}
			this.previousTime = e.getSimulationTime();
			System.out.println(this.previousTime);
			this.counter++;
		}
		
		public int getCount() {
			return this.counter;
		}

	}
}
