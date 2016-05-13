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

package org.matsim.contrib.taxi.schedule.reconstruct;

import java.nio.channels.IllegalSelectorException;
import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.*;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;


public class ScheduleReconstructor
{
    final TaxiData taxiData = new TaxiData();
    final Map<Id<Link>, ? extends Link> links;

    final Map<Id<Person>, ScheduleBuilder> scheduleBuilders = new LinkedHashMap<>();
    private boolean schedulesValidated = false;

    private final DriveRecorder driveRecorder;
    private final StayRecorder stayRecorder;
    private final RequestRecorder requestRecorder;


    @Inject
    public ScheduleReconstructor(Network network, EventsManager eventsManager)
    {
        links = network.getLinks();

        driveRecorder = new DriveRecorder(this);
        eventsManager.addHandler(driveRecorder);

        stayRecorder = new StayRecorder(this);
        eventsManager.addHandler(stayRecorder);

        requestRecorder = new RequestRecorder(this, TaxiModule.TAXI_MODE);
        eventsManager.addHandler(requestRecorder);
    }


    Id<Person> getDriver(Id<Vehicle> vehicleId)
    {
        return Id.createPersonId(vehicleId);
    }


    ScheduleBuilder getBuilder(Id<Person> personId)
    {
        return scheduleBuilders.get(personId);
    }


    private void validateSchedules()
    {
        if (driveRecorder.hasOngoingDrives() || stayRecorder.hasOngoingStays()
                || requestRecorder.hasAwaitingRequests()) {
            throw new IllegalStateException();
        }

        for (ScheduleBuilder sb : scheduleBuilders.values()) {
            if (!sb.isScheduleBuilt()) {
                throw new IllegalSelectorException();
            }
        }
    }


    public TaxiData getTaxiData()
    {
        if (!schedulesValidated) {
            validateSchedules();
        }

        return taxiData;
    }


    public static TaxiData reconstructFromFile(Network network, String eventsFile)
    {
        EventsManager eventsManager = EventsUtils.createEventsManager();
        ScheduleReconstructor reconstructor = new ScheduleReconstructor(network,
                eventsManager);
        new MatsimEventsReader(eventsManager).readFile(eventsFile);
        return reconstructor.getTaxiData();
    }
}
