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
import org.matsim.contrib.dvrp.path.*;
import org.matsim.vehicles.Vehicle;


public class DriveRecorder
    implements PersonEntersVehicleEventHandler, LinkLeaveEventHandler,
    VehicleLeavesTrafficEventHandler
{
    private class Drive
    {
        private final Id<Person> personId;
        private final double departureTime;
        private final List<Link> links = new ArrayList<>();
        private final List<Double> linkLeaveTimes = new ArrayList<>();


        private Drive(Id<Person> personId, double departureTime)
        {
            this.personId = personId;
            this.departureTime = departureTime;
        }
    }


    private final Map<Id<Vehicle>, Drive> drives = new HashMap<>();
    private final ScheduleReconstructor reconstructor;


    DriveRecorder(ScheduleReconstructor reconstructor)
    {
        this.reconstructor = reconstructor;
    }

    
    boolean hasOngoingDrives()
    {
        return !drives.isEmpty();
    }
    

    @Override
    public void handleEvent(PersonEntersVehicleEvent event)
    {
        Id<Person> personId = event.getPersonId();
        if (reconstructor.scheduleBuilders.containsKey(personId)) {
            Id<Vehicle> vehicleId = event.getVehicleId();
            drives.put(vehicleId, new Drive(personId, event.getTime()));
        }
    }


    @Override
    public void handleEvent(LinkLeaveEvent event)
    {
        Drive drive = drives.get(event.getVehicleId());
        if (drive != null) {
            updateDrive(drive, event.getLinkId(), event.getTime());
        }
    }


    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event)
    {
        Drive drive = drives.remove(event.getVehicleId());
        if (drive != null) {
            updateDrive(drive, event.getLinkId(), event.getTime());
            double duration = event.getTime() - drive.departureTime;
            Link[] links = drive.links.toArray(new Link[drive.links.size()]);
            double[] linkTTs = createLinkTTs(drive);
            VrpPathWithTravelData vrpPath = new VrpPathWithTravelDataImpl(drive.departureTime,
                    duration, links, linkTTs);
            reconstructor.scheduleBuilders.get(drive.personId).addDrive(vrpPath);
        }
    }


    private void updateDrive(Drive drive, Id<Link> linkId, double leaveTime)
    {
        drive.links.add(reconstructor.links.get(linkId));
        drive.linkLeaveTimes.add(leaveTime);
    }


    private double[] createLinkTTs(Drive drive)
    {
        double[] linkTTs = new double[drive.linkLeaveTimes.size()];
        double prevLLT = drive.departureTime;
        for (int i = 0; i < drive.linkLeaveTimes.size(); i++) {
            double currentLLT = drive.linkLeaveTimes.get(i);
            linkTTs[i] = currentLLT - prevLLT;
            prevLLT = currentLLT;
        }
        return linkTTs;
    }


    @Override
    public void reset(int iteration)
    {}
}
