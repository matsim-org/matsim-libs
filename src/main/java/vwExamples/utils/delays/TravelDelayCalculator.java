/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package vwExamples.utils.delays;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

import java.util.*;

public class TravelDelayCalculator implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

    private List<String> trips = new ArrayList<>();
    private Map<Id<Person>, Tuple<MutableDouble, MutableDouble>> travelTimes = new HashMap<>();
    private Map<Id<Vehicle>, Double> linkEnterTimes = new HashMap<>();
    private Network network;
    private Set<Id<Person>> relevantAgents = new HashSet<>();

    public TravelDelayCalculator(Network network, Set<Id<Person>> relevantAgents) {
        this.network = network;
        this.relevantAgents = relevantAgents;
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        if (linkEnterTimes.containsKey(event.getVehicleId())) {
            double enterTime = linkEnterTimes.remove(event.getVehicleId());
            double travelTime = event.getTime() - enterTime;
            Link l = network.getLinks().get(event.getLinkId());
            double freeSpeedTravelTime = Math.min(l.getLength() / l.getFreespeed(), travelTime);
            //the last link
            Id<Person> pid = Id.createPersonId(event.getVehicleId());
            if (travelTimes.containsKey(pid)) {
                Tuple<MutableDouble, MutableDouble> t = travelTimes.get(pid);
                t.getFirst().add(freeSpeedTravelTime);
                t.getSecond().add(travelTime);
            }

        }

    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        linkEnterTimes.put(event.getVehicleId(), event.getTime());
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (event.getLegMode().equals(TransportMode.car)) {
            if (travelTimes.containsKey(event.getPersonId())) {
                Id<Person> pid = event.getPersonId();
                Id<Vehicle> vid = Id.createVehicleId(pid);
                if (linkEnterTimes.containsKey(vid))
                //this will ignore all pt drivers, whose vehicleId != personId
                {
                    double enterTime = linkEnterTimes.remove(vid);

                    double travelTime = event.getTime() - enterTime;
                    Link l = network.getLinks().get(event.getLinkId());
                    double freeSpeedTravelTime = Math.min(l.getLength() / l.getFreespeed(), travelTime);

                    if (travelTimes.containsKey(pid)) {
                        Tuple<MutableDouble, MutableDouble> t = travelTimes.remove(pid);
                        t.getFirst().add(freeSpeedTravelTime);
                        t.getSecond().add(travelTime);

                        if (relevantAgents.contains(event.getPersonId())) {
                            String result = event.getPersonId() + ";" + event.getTime() + ";" + t.getFirst().intValue() + ";" + t.getSecond().intValue() + ";" + (t.getSecond().intValue()-t.getFirst().intValue());
                            trips.add(result);
                        }
                    }
                }
            }

        }

    }

    public List<String> getTrips() {
        return trips;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getLegMode().equals(TransportMode.car)) {
            this.travelTimes.put(event.getPersonId(), new Tuple<MutableDouble, MutableDouble>(new MutableDouble(), new MutableDouble()));
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (travelTimes.containsKey(event.getPersonId())) {
            linkEnterTimes.put(event.getVehicleId(), event.getTime());
        }
    }

}
