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

package org.matsim.core.mobsim.qsim.pt;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.pt.fakes.FakeAgent;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * @author mrieser
 */
public class TransitStopAgentTrackerTest {

	private static final Logger log = LogManager.getLogger(TransitStopAgentTrackerTest.class);

	@Test
	void testAddAgent() {
		EventsManager events = EventsUtils.createEventsManager();
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(events);
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		PTPassengerAgent agent1 = new FakeAgent(null, null);
		PTPassengerAgent agent2 = new FakeAgent(null, null);
		PTPassengerAgent agent3 = new FakeAgent(null, null);
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord((double) 2, (double) 3), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord((double) 3, (double) 4), false);

		assertFalse(tracker.getAgentsAtFacility(stop1.getId()).contains(agent1));
		tracker.addAgentToStop(10, agent1, stop1.getId());
		assertTrue(tracker.getAgentsAtFacility(stop1.getId()).contains(agent1));
		assertFalse(tracker.getAgentsAtFacility(stop2.getId()).contains(agent1));
		assertFalse(tracker.getAgentsAtFacility(stop1.getId()).contains(agent2));
		tracker.addAgentToStop(10, agent2, stop1.getId());
		assertTrue(tracker.getAgentsAtFacility(stop1.getId()).contains(agent2));

		tracker.addAgentToStop(10, agent3, stop2.getId());
		assertFalse(tracker.getAgentsAtFacility(stop1.getId()).contains(agent3));
		assertFalse(tracker.getAgentsAtFacility(stop2.getId()).contains(agent1));
		assertFalse(tracker.getAgentsAtFacility(stop2.getId()).contains(agent2));
		assertTrue(tracker.getAgentsAtFacility(stop2.getId()).contains(agent3));
	}

	@Test
	void testRemoveAgent() {
		EventsManager events = EventsUtils.createEventsManager();
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(events);
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		PTPassengerAgent agent1 = new FakeAgent(null, null);
		PTPassengerAgent agent2 = new FakeAgent(null, null);
		PTPassengerAgent agent3 = new FakeAgent(null, null);
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord((double) 2, (double) 3), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord((double) 3, (double) 4), false);

		tracker.addAgentToStop(10, agent1, stop1.getId());
		tracker.addAgentToStop(10, agent2, stop1.getId());
		tracker.addAgentToStop(10, agent3, stop2.getId());
		assertEquals(2, tracker.getAgentsAtFacility(stop1.getId()).size());
		assertEquals(1, tracker.getAgentsAtFacility(stop2.getId()).size());
		assertTrue(tracker.getAgentsAtFacility(stop1.getId()).contains(agent1));
		tracker.removeAgentFromStop(agent1, stop1.getId());
		assertFalse(tracker.getAgentsAtFacility(stop1.getId()).contains(agent1));
		assertTrue(tracker.getAgentsAtFacility(stop1.getId()).contains(agent2));
		assertTrue(tracker.getAgentsAtFacility(stop2.getId()).contains(agent3));
		assertEquals(1, tracker.getAgentsAtFacility(stop1.getId()).size());
		tracker.removeAgentFromStop(agent1, stop1.getId()); // cannot be removed
		assertEquals(1, tracker.getAgentsAtFacility(stop1.getId()).size()); // should stay the same
	}

	@Test
	void testGetAgentsAtStopImmutable() {
		EventsManager events = EventsUtils.createEventsManager();
		TransitStopAgentTracker tracker = new TransitStopAgentTracker(events);
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		PTPassengerAgent agent1 = new FakeAgent(null, null);
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord((double) 2, (double) 3), false);

		try {
			tracker.getAgentsAtFacility(stop1.getId()).add(agent1);
			fail("missing exception, empty list should be immutable.");
		}
		catch (UnsupportedOperationException e) {
			log.info("catched expected exception.", e);
		}

		tracker.addAgentToStop(10, agent1, stop1.getId());
		try {
			tracker.getAgentsAtFacility(stop1.getId()).remove(0);
			fail("missing exception, non-empty list should be immutable.");
		}
		catch (UnsupportedOperationException e) {
			log.info("catched expected exception.", e);
		}
	}
}
