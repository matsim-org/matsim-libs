/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToActivities.java
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
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.population.ActivityImpl;

import java.util.HashMap;
import java.util.Map;

public class EventsToActivities implements ActivityStartEventHandler, ActivityEndEventHandler {

    private Map<Id, ActivityImpl> activities = new HashMap<Id, ActivityImpl>();
    private ActivityHandler activityHandler;

    @Override
    public void handleEvent(ActivityEndEvent event) {
        ActivityImpl activity = activities.get(event.getPersonId());
        if (activity == null) {
            ActivityImpl firstActivity = new ActivityImpl(event.getActType(), event.getLinkId());
            activity = firstActivity;
        }
        activity.setEndTime(event.getTime());
        activityHandler.handleActivity(event.getPersonId(), activity);
        activities.remove(event.getPersonId());
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        ActivityImpl activity = new ActivityImpl(event.getActType(), event.getLinkId());
        activity.setStartTime(event.getTime());
        activities.put(event.getPersonId(), activity);
    }

    @Override
    public void reset(int iteration) {
        activities.clear();
    }

    public void setActivityHandler(ActivityHandler activityHandler) {
        this.activityHandler = activityHandler;
    }

    public void finish() {
        for (Map.Entry<Id, ActivityImpl> entry : activities.entrySet()) {
            activityHandler.handleActivity(entry.getKey(), entry.getValue());
        }
    }

}
