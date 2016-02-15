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

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;

public class EventsToActivitiesTest {

	@Test
	public void testCreatesActivty() {
		EventsToActivities testee = new EventsToActivities();
		MockActivityHandler ah = new MockActivityHandler();
		testee.addActivityHandler(ah);
		testee.reset(0);
		testee.handleEvent(new ActivityStartEvent(10.0, Id.create("1", Person.class), Id.create("l1", Link.class), Id.create("l1", ActivityFacility.class), "work"));
		testee.handleEvent(new ActivityEndEvent(30.0, Id.create("1", Person.class), Id.create("l1", Link.class), Id.create("l1", ActivityFacility.class), "work"));
		Assert.assertNotNull(ah.handledActivity);
		Assert.assertEquals(10.0, ah.handledActivity.getActivity().getStartTime(), 1e-8);
		Assert.assertEquals(30.0, ah.handledActivity.getActivity().getEndTime(), 1e-8);
	}

	@Test
	public void testCreateNightActivity() {
		EventsToActivities testee = new EventsToActivities();
		MockActivityHandler ah = new MockActivityHandler();
		testee.addActivityHandler(ah);
		testee.reset(0);
		testee.handleEvent(new ActivityEndEvent(10.0, Id.create("1", Person.class), Id.create("l1", Link.class), Id.create("l1", ActivityFacility.class), "home"));
		Assert.assertNotNull(ah.handledActivity);
		Assert.assertEquals(Time.UNDEFINED_TIME, ah.handledActivity.getActivity().getStartTime(), 1e-8);
		Assert.assertEquals(10.0, ah.handledActivity.getActivity().getEndTime(), 1e-8);
		ah.reset();
		testee.handleEvent(new ActivityStartEvent(90.0, Id.create("1", Person.class), Id.create("l1", Link.class), Id.create("l1", ActivityFacility.class), "home"));
		testee.finish();
		Assert.assertNotNull(ah.handledActivity);
		Assert.assertEquals(Time.UNDEFINED_TIME, ah.handledActivity.getActivity().getEndTime(), 1e-8);
		Assert.assertEquals(90.0, ah.handledActivity.getActivity().getStartTime(), 1e-8);
	}
	
	@Test
	public void testDontCreateNightActivityIfNoneIsBeingPerformedWhenSimulationEnds() {
		EventsToActivities testee = new EventsToActivities();
		MockActivityHandler ah = new MockActivityHandler();
		testee.addActivityHandler(ah);
		testee.reset(0);
		testee.handleEvent(new ActivityEndEvent(10.0, Id.create("1", Person.class), Id.create("l1", Link.class), Id.create("f1", ActivityFacility.class), "home"));
		Assert.assertNotNull(ah.handledActivity);
		Assert.assertEquals(Time.UNDEFINED_TIME, ah.handledActivity.getActivity().getStartTime(), 1e-8);
		Assert.assertEquals(10.0, ah.handledActivity.getActivity().getEndTime(), 1e-8);
		ah.reset();
		testee.finish();
		Assert.assertNull(ah.handledActivity);
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
