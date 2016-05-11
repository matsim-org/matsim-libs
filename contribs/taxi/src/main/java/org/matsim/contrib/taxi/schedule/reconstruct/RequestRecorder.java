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

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.taxi.data.TaxiRequest;


public class RequestRecorder
    implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler
{
    private final Map<Id<Person>, TaxiRequest> ongoingRequests = new HashMap<>();
    private final ScheduleReconstructor reconstructor;
    private final String legMode;
    private long requestCounter = 0;//TODO necessary until RequestSubmittedEvent is introduced


    public RequestRecorder(ScheduleReconstructor reconstructor, String legMode)
    {
        this.reconstructor = reconstructor;
        this.legMode = legMode;
    }


    boolean hasAwaitingRequests()
    {
        return !ongoingRequests.isEmpty();
    }


    @Override
    public void handleEvent(PersonDepartureEvent event)
    {
        if (event.getLegMode().equals(legMode)) {
            Id<Request> id = Id.create(requestCounter++, Request.class);
            Link fromLink = reconstructor.links.get(event.getLinkId());
            double time = event.getTime();
            TaxiRequest request = new TaxiRequest(id, null, fromLink, time, time);
            TaxiRequest oldRequest = ongoingRequests.put(event.getPersonId(), request);
            if (oldRequest != null) {
                throw new IllegalStateException("Currently only one request per passenger");
            }
            reconstructor.taxiData.addRequest(request);
        }
    }


    @Override
    public void handleEvent(PersonEntersVehicleEvent event)
    {
        TaxiRequest request = ongoingRequests.remove(event.getPersonId());
        if (request != null) {
            Id<Person> driverId = reconstructor.getDriver(event.getVehicleId());
            reconstructor.scheduleBuilders.get(driverId).addRequest(request);
        }
    }


    @Override
    public void reset(int iteration)
    {}
}
