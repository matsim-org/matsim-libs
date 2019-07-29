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

package commercialtraffic.analysis;/*
 * created by jbischoff, 19.06.2019
 */

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.FreightConstants;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TourLengthAnalyzer implements ActivityEndEventHandler, LinkEnterEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

    private final Map<Id<Vehicle>, Set<Id<Person>>> currentFreightVehicleForDelivery = new HashMap<>();
    private final Map<Id<Person>, Double> deliveryAgentDistances = new HashMap<>();


    private final Network network;

    @Inject
    public TourLengthAnalyzer(Network network, EventsManager eventsManager) {
        this.network = network;
        eventsManager.addHandler(this);
    }


    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (event.getActType().equals(FreightConstants.START)) {
            deliveryAgentDistances.put(event.getPersonId(), 0.0);
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if (currentFreightVehicleForDelivery.containsKey(event.getVehicleId())) {
            for (Id<Person> p : currentFreightVehicleForDelivery.get(event.getVehicleId())) {
                double currentDistance = deliveryAgentDistances.get(p);
                currentDistance += network.getLinks().get(event.getLinkId()).getLength();
                deliveryAgentDistances.put(p, currentDistance);
            }
        }

    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (deliveryAgentDistances.containsKey(event.getPersonId())) {
            currentFreightVehicleForDelivery.putIfAbsent(event.getVehicleId(), new HashSet<>());
            currentFreightVehicleForDelivery.get(event.getVehicleId()).add(event.getPersonId());
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        if (currentFreightVehicleForDelivery.containsKey(event.getVehicleId())) {
            currentFreightVehicleForDelivery.get(event.getVehicleId()).remove(event.getPersonId());
        }
    }

    @Override
    public void reset(int iteration) {
        currentFreightVehicleForDelivery.clear();
    }

    public Map<Id<Person>, Double> getDeliveryAgentDistances() {
        return deliveryAgentDistances;
    }
}
