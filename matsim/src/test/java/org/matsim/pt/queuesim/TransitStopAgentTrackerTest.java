/* *********************************************************************** *
 * project: org.matsim.*
 * TransitStopAgentTrackerTest.java
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

package org.matsim.pt.queuesim;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.fakes.FakeAgent;
import org.matsim.pt.qsim.PassengerAgent;
import org.matsim.pt.qsim.TransitStopAgentTracker;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitStopFacility;


/**
 * @author mrieser
 */
public class TransitStopAgentTrackerTest extends TestCase {

	private static final Logger log = Logger.getLogger(TransitStopAgentTrackerTest.class);

	public void testAddAgent() {
		TransitStopAgentTracker tracker = new TransitStopAgentTracker();
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		PassengerAgent agent1 = new FakeAgent(null, null);
		PassengerAgent agent2 = new FakeAgent(null, null);
		PassengerAgent agent3 = new FakeAgent(null, null);
		TransitStopFacility stop1 = builder.createTransitStopFacility(new IdImpl(1), new CoordImpl(2, 3), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(new IdImpl(2), new CoordImpl(3, 4), false);

		assertFalse(tracker.getAgentsAtStop(stop1).contains(agent1));
		tracker.addAgentToStop(agent1, stop1);
		assertTrue(tracker.getAgentsAtStop(stop1).contains(agent1));
		assertFalse(tracker.getAgentsAtStop(stop2).contains(agent1));
		assertFalse(tracker.getAgentsAtStop(stop1).contains(agent2));
		tracker.addAgentToStop(agent2, stop1);
		assertTrue(tracker.getAgentsAtStop(stop1).contains(agent2));

		tracker.addAgentToStop(agent3, stop2);
		assertFalse(tracker.getAgentsAtStop(stop1).contains(agent3));
		assertFalse(tracker.getAgentsAtStop(stop2).contains(agent1));
		assertFalse(tracker.getAgentsAtStop(stop2).contains(agent2));
		assertTrue(tracker.getAgentsAtStop(stop2).contains(agent3));
	}

	public void testRemoveAgent() {
		TransitStopAgentTracker tracker = new TransitStopAgentTracker();
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		PassengerAgent agent1 = new FakeAgent(null, null);
		PassengerAgent agent2 = new FakeAgent(null, null);
		PassengerAgent agent3 = new FakeAgent(null, null);
		TransitStopFacility stop1 = builder.createTransitStopFacility(new IdImpl(1), new CoordImpl(2, 3), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(new IdImpl(2), new CoordImpl(3, 4), false);

		tracker.addAgentToStop(agent1, stop1);
		tracker.addAgentToStop(agent2, stop1);
		tracker.addAgentToStop(agent3, stop2);
		assertEquals(2, tracker.getAgentsAtStop(stop1).size());
		assertEquals(1, tracker.getAgentsAtStop(stop2).size());
		assertTrue(tracker.getAgentsAtStop(stop1).contains(agent1));
		tracker.removeAgentFromStop(agent1, stop1);
		assertFalse(tracker.getAgentsAtStop(stop1).contains(agent1));
		assertTrue(tracker.getAgentsAtStop(stop1).contains(agent2));
		assertTrue(tracker.getAgentsAtStop(stop2).contains(agent3));
		assertEquals(1, tracker.getAgentsAtStop(stop1).size());
		tracker.removeAgentFromStop(agent1, stop1); // cannot be removed
		assertEquals(1, tracker.getAgentsAtStop(stop1).size()); // should stay the same
	}

	public void testGetAgentsAtStopImmutable() {
		TransitStopAgentTracker tracker = new TransitStopAgentTracker();
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		PassengerAgent agent1 = new FakeAgent(null, null);
		TransitStopFacility stop1 = builder.createTransitStopFacility(new IdImpl(1), new CoordImpl(2, 3), false);

		try {
			tracker.getAgentsAtStop(stop1).add(agent1);
			fail("missing exception, empty list should be immutable.");
		}
		catch (UnsupportedOperationException e) {
			log.info("catched expected exception.", e);
		}

		tracker.addAgentToStop(agent1, stop1);
		try {
			tracker.getAgentsAtStop(stop1).remove(0);
			fail("missing exception, non-empty list should be immutable.");
		}
		catch (UnsupportedOperationException e) {
			log.info("catched expected exception.", e);
		}
	}
}
