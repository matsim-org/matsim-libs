/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 *
 */
package org.matsim.contrib.av.robotaxi.fares.drt;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jbischoff
 * A simple implementation for taxi fares.
 * Note that these fares are scored in excess to anything set in the modeparams in the config file.
 */
public class DrtFareHandler
        implements DrtRequestSubmittedEventHandler, PersonArrivalEventHandler {


    @Inject
    private EventsManager events;

    private final double distanceFare_Meter;
    private final double baseFare;
    private final double timeFare_sec;
    private final double dailyFee;
    Set<Id<Person>> dailyFeeCharged = new HashSet<>();
    Map<Id<Person>, DrtRequestSubmittedEvent> lastRequestSubmission = new HashMap();
    private String mode;

    /**
     * @params drtFareConfigGroup: DrtFareConfigGroup for the specific mode
     */
    public DrtFareHandler(DrtFareConfigGroup drtFareConfigGroup) {
        this.mode = drtFareConfigGroup.getMode();
        this.distanceFare_Meter = drtFareConfigGroup.getDistanceFare_m();
        this.baseFare = drtFareConfigGroup.getBasefare();
        this.dailyFee = drtFareConfigGroup.getDailySubscriptionFee();
        this.timeFare_sec = drtFareConfigGroup.getTimeFare_h() / 3600.0;
    }

    DrtFareHandler(DrtFareConfigGroup drtFareConfigGroup, EventsManager events) {
        this(drtFareConfigGroup);
        this.events = events;

    }

    @Override
    public void reset(int iteration) {
        dailyFeeCharged.clear();
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (event.getLegMode().equals(this.mode)) {
            if (!dailyFeeCharged.contains(event.getPersonId())) {
                dailyFeeCharged.add(event.getPersonId());
                events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), -dailyFee));
            }
            DrtRequestSubmittedEvent e = this.lastRequestSubmission.get(event.getPersonId());
            double fare = distanceFare_Meter * e.getUnsharedRideDistance() + timeFare_sec * e.getUnsharedRideTime() + baseFare;
            events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), -fare));
        }

    }


    @Override
    public void handleEvent(DrtRequestSubmittedEvent event) {
        if (this.mode.equals(event.getMode())) {
            this.lastRequestSubmission.put(event.getPersonId(), event);
        }
    }

}
