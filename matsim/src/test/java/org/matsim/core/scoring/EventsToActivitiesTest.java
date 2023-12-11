/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.scoring;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.facilities.ActivityFacility;

public class EventsToActivitiesTest {

	@Test
	void testCreatesActivty() {
		EventsToActivities testee = new EventsToActivities();
		MockActivityHandler ah = new MockActivityHandler();
		testee.addActivityHandler(ah);
		testee.reset(0);
		testee.handleEvent(new ActivityStartEvent(10.0, Id.create("1", Person.class), Id.create("l1", Link.class), Id.create("l1", ActivityFacility.class),
				"work", new Coord( 123., 4.56 ) ) );
		testee.handleEvent(new ActivityEndEvent(30.0, Id.create("1", Person.class), Id.create("l1", Link.class), Id.create("l1", ActivityFacility.class),
				"work", new Coord( 123., 4.56 )));
		Assertions.assertNotNull(ah.handledActivity);
		Assertions.assertEquals(10.0, ah.handledActivity.getActivity().getStartTime().seconds(), 1e-8);
		Assertions.assertEquals(30.0, ah.handledActivity.getActivity().getEndTime().seconds(), 1e-8);
		Assertions.assertEquals( 123., ah.handledActivity.getActivity().getCoord().getX(), 0. );
		Assertions.assertEquals( 4.56, ah.handledActivity.getActivity().getCoord().getY(), 0. );
	}

	@Test
	void testCreateNightActivity() {
		EventsToActivities testee = new EventsToActivities();
		MockActivityHandler ah = new MockActivityHandler();
		testee.addActivityHandler(ah);
		testee.reset(0);
		testee.handleEvent(new ActivityEndEvent(10.0, Id.create("1", Person.class), Id.create("l1", Link.class), Id.create("l1", ActivityFacility.class),
				"home", new Coord( 123., 4.56 )));
		Assertions.assertNotNull(ah.handledActivity);
		Assertions.assertTrue(ah.handledActivity.getActivity().getStartTime().isUndefined());
		Assertions.assertEquals(10.0, ah.handledActivity.getActivity().getEndTime().seconds(), 1e-8);
		ah.reset();
		testee.handleEvent(new ActivityStartEvent(90.0, Id.create("1", Person.class), Id.create("l1", Link.class), Id.create("l1", ActivityFacility.class),
				"home", new Coord( 123., 4.56 ) ) );
		testee.finish();
		Assertions.assertNotNull(ah.handledActivity);
		Assertions.assertTrue(ah.handledActivity.getActivity().getEndTime().isUndefined());
		Assertions.assertEquals(90.0, ah.handledActivity.getActivity().getStartTime().seconds(), 1e-8);
		Assertions.assertEquals( 123., ah.handledActivity.getActivity().getCoord().getX(), 0. );
		Assertions.assertEquals( 4.56, ah.handledActivity.getActivity().getCoord().getY(), 0. );
	}

	@Test
	void testDontCreateNightActivityIfNoneIsBeingPerformedWhenSimulationEnds() {
		EventsToActivities testee = new EventsToActivities();
		MockActivityHandler ah = new MockActivityHandler();
		testee.addActivityHandler(ah);
		testee.reset(0);
		testee.handleEvent(new ActivityEndEvent(10.0, Id.create("1", Person.class), Id.create("l1", Link.class), Id.create("f1", ActivityFacility.class),
				"home", new Coord( 123., 4.56 )));
		Assertions.assertNotNull(ah.handledActivity);
		Assertions.assertTrue(ah.handledActivity.getActivity().getStartTime().isUndefined()) ;
		Assertions.assertEquals(10.0, ah.handledActivity.getActivity().getEndTime().seconds(), 1e-8);
		ah.reset();
		testee.finish();
		Assertions.assertNull(ah.handledActivity);
	}

	private static class MockActivityHandler implements ActivityHandler {
		public PersonExperiencedActivity handledActivity = null;
		@Override
		public void handleActivity(PersonExperiencedActivity activity) {
			this.handledActivity = activity;
		}
		public void reset() {
			this.handledActivity = null;
		}
	}

}
