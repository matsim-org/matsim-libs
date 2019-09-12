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

import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.population.PopulationUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Converts a stream of Events into a stream of Activities. Passes Activities to a single ActivityHandler which must be registered with this class.
 * Mainly intended for scoring, but can be used for any kind of Activity related statistics. Essentially, it allows you to read
 * Activities from the simulation like you would read Activities from Plans, except that the Plan does not even need to exist.
 * <p></p>
 * Note that the instances of Activity passed to the LegHandler will never be identical to those in the Scenario! Even
 * in a "no-op" simulation which only reproduces the Plan, new instances will be created. So if you attach your own data
 * to the Activities in the Scenario, that's your own lookout.
 * 
 * @author michaz
 *
 */
public final class EventsToActivities implements ActivityStartEventHandler, ActivityEndEventHandler {
	
	public interface ActivityHandler {
	    void handleActivity(PersonExperiencedActivity activity);
	}

    private IdMap<Person, Activity> activities = new IdMap<>(Person.class, Activity.class);
    private List<ActivityHandler> activityHandlers = new ArrayList<>();

    public EventsToActivities() {

    }

    @Inject
    EventsToActivities(ControlerListenerManager controlerListenerManager) {
        controlerListenerManager.addControlerListener(new AfterMobsimListener() {
            @Override
            public void notifyAfterMobsim(AfterMobsimEvent event) {
                finish();
            }
        });
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        Activity activity = this.activities.remove(event.getPersonId());
        if (activity == null) {
            Activity firstActivity = PopulationUtils.createActivityFromLinkId(event.getActType(), event.getLinkId());
            firstActivity.setFacilityId(event.getFacilityId());
            activity = firstActivity;
        }
        activity.setEndTime(event.getTime());
        for (ActivityHandler activityHandler : this.activityHandlers) {
            activityHandler.handleActivity(new PersonExperiencedActivity(event.getPersonId(), activity));
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        Activity activity = PopulationUtils.createActivityFromLinkId(event.getActType(), event.getLinkId());
        activity.setFacilityId(event.getFacilityId());
        activity.setStartTime(event.getTime());
        this.activities.put(event.getPersonId(), activity);
    }

    @Override
    public void reset(int iteration) {
        this.activities.clear();
    }

    public void addActivityHandler(ActivityHandler activityHandler) {
        this.activityHandlers.add(activityHandler);
    }

    public void finish() {
        this.activities.forEach((id, activity) -> {
            for (ActivityHandler activityHandler : this.activityHandlers) {
                activityHandler.handleActivity(new PersonExperiencedActivity(id, activity));
            }
        });
    }

}
