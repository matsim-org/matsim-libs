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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.population.ActivityImpl;

import javax.inject.Inject;

/**
 * 
 * Converts a stream of Events into a stream of Activities. Passes Activities to a single ActivityHandler which must be registered with this class.
 * Mainly intended for scoring, but can be used for any kind of Activity related statistics. Essentially, it allows you to read
 * Activities from the simulation like you would read Activities from Plans, except that the Plan does not even need to exist.
 * <p/>
 * Note that the instances of Activity passed to the LegHandler will never be identical to those in the Scenario! Even
 * in a "no-op" simulation which only reproduces the Plan, new instances will be created. So if you attach your own data
 * to the Activities in the Scenario, that's your own lookout.
 * 
 * @author michaz
 *
 */
public class EventsToActivities implements ActivityStartEventHandler, ActivityEndEventHandler {
	
	public interface ActivityHandler {
	    void handleActivity(PersonExperiencedActivity activity);
	}

    private Map<Id<Person>, ActivityImpl> activities = new HashMap<>();
    private List<ActivityHandler> activityHandlers = new ArrayList<>();

    public EventsToActivities() {

    }

    @Inject
    EventsToActivities(ControlerListenerManager controlerListenerManager, EventsManager eventsManager) {
        controlerListenerManager.addControlerListener(new AfterMobsimListener() {
            @Override
            public void notifyAfterMobsim(AfterMobsimEvent event) {
                finish();
            }
        });
        eventsManager.addHandler(this);
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        ActivityImpl activity = activities.get(event.getPersonId());
        if (activity == null) {
            ActivityImpl firstActivity = new ActivityImpl(event.getActType(), event.getLinkId());
            firstActivity.setFacilityId(event.getFacilityId());
            activity = firstActivity;
        }
        activity.setEndTime(event.getTime());
        for (ActivityHandler activityHandler : activityHandlers) {
            activityHandler.handleActivity(new PersonExperiencedActivity(event.getPersonId(), activity));
        }
        activities.remove(event.getPersonId());
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        ActivityImpl activity = new ActivityImpl(event.getActType(), event.getLinkId());
        activity.setFacilityId(event.getFacilityId());
        activity.setStartTime(event.getTime());
        activities.put(event.getPersonId(), activity);
    }

    @Override
    public void reset(int iteration) {
        activities.clear();
    }

    public void addActivityHandler(ActivityHandler activityHandler) {
        this.activityHandlers.add(activityHandler);
    }

    public void finish() {
        for (Map.Entry<Id<Person>, ActivityImpl> entry : activities.entrySet()) {
            for (ActivityHandler activityHandler : activityHandlers) {
                activityHandler.handleActivity(new PersonExperiencedActivity(entry.getKey(), entry.getValue()));
            }
        }
    }

}
