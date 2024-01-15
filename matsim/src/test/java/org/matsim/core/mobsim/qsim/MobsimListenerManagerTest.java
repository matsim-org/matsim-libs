/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;

/**
 * @author mrieser
 */
public class MobsimListenerManagerTest {

	@Test
	void testAddQueueSimulationListener() {
		MobsimListenerManager manager = new MobsimListenerManager(null);
		TestSimListener simpleListener = new TestSimListener();
		TestSubSimListener subListener = new TestSubSimListener();
		TestExtendedSimListener extendedListener = new TestExtendedSimListener();
		TestDoubleSimListener doubleListener = new TestDoubleSimListener();
		manager.addQueueSimulationListener(simpleListener);
		manager.addQueueSimulationListener(subListener);
		manager.addQueueSimulationListener(extendedListener);
		manager.addQueueSimulationListener(doubleListener);
		manager.fireQueueSimulationInitializedEvent();
		Assertions.assertEquals(1, simpleListener.count);
		Assertions.assertEquals(1, subListener.count);
		Assertions.assertEquals(1, extendedListener.count);
		Assertions.assertEquals(1, doubleListener.count);
	}

	@Test
	void testRemoveQueueSimulationListener() {
		MobsimListenerManager manager = new MobsimListenerManager(null);
		TestSimListener simpleListener = new TestSimListener();
		TestSubSimListener subListener = new TestSubSimListener();
		TestExtendedSimListener extendedListener = new TestExtendedSimListener();
		TestDoubleSimListener doubleListener = new TestDoubleSimListener();
		manager.addQueueSimulationListener(simpleListener);
		manager.addQueueSimulationListener(subListener);
		manager.addQueueSimulationListener(extendedListener);
		manager.addQueueSimulationListener(doubleListener);
		manager.fireQueueSimulationInitializedEvent();
		Assertions.assertEquals(1, simpleListener.count);
		Assertions.assertEquals(1, subListener.count);
		Assertions.assertEquals(1, extendedListener.count);
		Assertions.assertEquals(1, doubleListener.count);

		manager.removeQueueSimulationListener(simpleListener);
		manager.removeQueueSimulationListener(subListener);
		manager.removeQueueSimulationListener(extendedListener);
		manager.removeQueueSimulationListener(doubleListener);
		manager.fireQueueSimulationInitializedEvent();
		Assertions.assertEquals(1, simpleListener.count); // should stay at 1
		Assertions.assertEquals(1, subListener.count);
		Assertions.assertEquals(1, extendedListener.count);
		Assertions.assertEquals(1, doubleListener.count);
	}

	/*package*/ static class TestSimListener implements MobsimInitializedListener {
		public int count = 0;
		@Override
		public void notifyMobsimInitialized(MobsimInitializedEvent e) {
			this.count++;
		}
	}

	/*package*/ static class TestSubSimListener extends TestSimListener {
		// interface implemented by super class
	}

	/*package*/ interface ExtendedSimListener extends MobsimInitializedListener {
		// interface inherited
	}

	/*package*/ static class TestExtendedSimListener implements ExtendedSimListener {
		public int count = 0;
		@Override
		public void notifyMobsimInitialized(MobsimInitializedEvent e) {
			this.count++;
		}
	}

	/*package*/ static class TestDoubleSimListener extends TestExtendedSimListener implements MobsimInitializedListener {
		// class implements an already implemented interface, should only be called once!
	}
}
