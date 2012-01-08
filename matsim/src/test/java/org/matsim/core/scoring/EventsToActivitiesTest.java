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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

 public class EventsToActivitiesTest extends MatsimTestCase {

    public void testCreatesActivty() {
        EventsManager eventsManager = EventsUtils.createEventsManager();
        EventsToActivities testee = new EventsToActivities();
        MockActivityHandler ah = new MockActivityHandler();
        testee.setActivityHandler(ah);
        testee.reset(0);
        testee.handleEvent(eventsManager.getFactory().createActivityStartEvent(10.0, new IdImpl("1"), new IdImpl("l1"), new IdImpl("f1"), "work"));
        testee.handleEvent(eventsManager.getFactory().createActivityEndEvent(30.0, new IdImpl("1"), new IdImpl("l1"), new IdImpl("f1"), "work"));
        assertNotNull(ah.handledActivity);
        assertEquals(10.0, ah.handledActivity.getStartTime());
        assertEquals(30.0, ah.handledActivity.getEndTime());
    }

    public void testCreateNightActivity() {
        EventsManager eventsManager = EventsUtils.createEventsManager();
        EventsToActivities testee = new EventsToActivities();
        MockActivityHandler ah = new MockActivityHandler();
        testee.setActivityHandler(ah);
        testee.reset(0);
        testee.handleEvent(eventsManager.getFactory().createActivityEndEvent(10.0, new IdImpl("1"), new IdImpl("l1"), new IdImpl("f1"), "home"));
        assertNotNull(ah.handledActivity);
        assertEquals(Time.UNDEFINED_TIME, ah.handledActivity.getStartTime());
        assertEquals(10.0, ah.handledActivity.getEndTime());
        ah.handledActivity = null;
        testee.handleEvent(eventsManager.getFactory().createActivityStartEvent(90.0, new IdImpl("1"), new IdImpl("l1"), new IdImpl("f1"), "home"));
        testee.finish();
        assertNotNull(ah.handledActivity);
        assertEquals(Time.UNDEFINED_TIME, ah.handledActivity.getEndTime());
        assertEquals(90.0, ah.handledActivity.getStartTime());
    }
    
    private static class MockActivityHandler implements ActivityHandler {
    	public Activity handledActivity = null;
    	@Override
    	public void handleActivity(Id agentId, Activity activity) {
    		this.handledActivity = activity;
    	}
    }

}
