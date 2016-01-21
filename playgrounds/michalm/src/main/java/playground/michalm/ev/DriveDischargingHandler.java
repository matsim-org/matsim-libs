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
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;


public class DriveDischargingHandler
    implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler
{
    private final Network network;
    private final TravelTime travelTime;

    private final Map<Id<Vehicle>, ? extends ElectricVehicle> vehicleToEV;
    private final Map<Id<Vehicle>, Double> linkEnterTimes = new HashMap<>();


    public DriveDischargingHandler(Map<Id<Vehicle>, ? extends ElectricVehicle> vehicleToEV,
            Network network, TravelTime travelTime)
    {
        this.vehicleToEV = vehicleToEV;
        this.network = network;
        this.travelTime = travelTime;
    }


    @Override
    public void handleEvent(LinkEnterEvent event)
    {
        if (vehicleToEV.containsKey(event.getVehicleId())) {// handle only our EVs
            linkEnterTimes.put(event.getVehicleId(), event.getTime());
        }
    }


    @Override
    public void handleEvent(LinkLeaveEvent event)
    {
        dischargeVehicle(event.getVehicleId(), event.getLinkId(), event.getTime());
    }


    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event)
    {
        dischargeVehicle(event.getVehicleId(), event.getLinkId(), Double.NaN);
    }


    private void dischargeVehicle(Id<Vehicle> vehicleId, Id<Link> linkId, double linkLeaveTime)
    {
        Double linkEnterTime = linkEnterTimes.get(vehicleId);
        if (linkEnterTime != null) {
            ElectricVehicle ev = vehicleToEV.get(vehicleId);
            Link link = network.getLinks().get(linkId);

            boolean vehLeftTraffic = Double.isNaN(linkLeaveTime);
            double tt = vehLeftTraffic ? // 
                    travelTime.getLinkTravelTime(link, linkEnterTime, null, null)
                    : linkLeaveTime - linkEnterTime;

            ev.getDriveEnergyConsumption().useEnergy(link, tt);

            if (vehLeftTraffic) {
                linkEnterTimes.remove(vehicleId);
            }
        }
    }


    @Override
    public void reset(int iteration)
    {
        linkEnterTimes.clear();
    }
}
