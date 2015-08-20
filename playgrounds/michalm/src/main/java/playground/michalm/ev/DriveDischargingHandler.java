/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.michalm.ev;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;


public class DriveDischargingHandler
    implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler
{
    private final Network network;
    private final TravelTime travelTime;

    private final Map<Id<Person>, ElectricVehicle> driverToVehicle;
    private final Map<Id<Person>, Double> linkEnterTimes = new HashMap<>();


    public DriveDischargingHandler(Map<Id<Person>, ElectricVehicle> driverToVehicle,
            Network network, TravelTime travelTime)
    {
        this.driverToVehicle = driverToVehicle;
        this.network = network;
        this.travelTime = travelTime;
    }


    @Override
    public void handleEvent(LinkEnterEvent event)
    {
        if (driverToVehicle.containsKey(event.getPersonId())) {// handle only our EVs
            linkEnterTimes.put(event.getPersonId(), event.getTime());
        }
    }


    @Override
    public void handleEvent(LinkLeaveEvent event)
    {
        dischargeVehicle(event.getPersonId(), event.getLinkId(), event);
    }


    @Override
    public void handleEvent(PersonArrivalEvent event)
    {
        dischargeVehicle(event.getPersonId(), event.getLinkId(), event);
    }


    private void dischargeVehicle(Id<Person> driverId, Id<Link> linkId, Event event)
    {
        Double linkEnterTime = linkEnterTimes.get(driverId);
        if (linkEnterTime != null) {
            ElectricVehicle ev = driverToVehicle.get(driverId);
            Link link = network.getLinks().get(linkId);

            boolean arrival = event instanceof PersonArrivalEvent;
            double tt = arrival ? //
                    travelTime.getLinkTravelTime(link, linkEnterTime, null, null)
                    : event.getTime() - linkEnterTime;

            double energy = ev.getDriveEnergyConsumption().calcEnergy(link, tt);
            ev.getBattery().discharge(energy);

            if (arrival) {
                linkEnterTimes.remove(driverId);
            }
        }
    }


    @Override
    public void reset(int iteration)
    {
        linkEnterTimes.clear();
    }
}
