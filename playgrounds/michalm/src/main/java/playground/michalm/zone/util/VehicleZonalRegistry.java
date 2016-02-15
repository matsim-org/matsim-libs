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

package playground.michalm.zone.util;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.data.Vehicle;

import com.google.common.collect.Maps;

import playground.michalm.zone.*;


//TODO never used anywhere...
public class VehicleZonalRegistry
{
    private static class ZoneCrossing
    {
        private final Zone fromZone;
        private final Zone toZone;


        private ZoneCrossing(Zone fromZone, Zone toZone)
        {
            this.fromZone = fromZone;
            this.toZone = toZone;
        }
    }


    private final Network network;
    private final ZonalSystem zonalSystem;

    private final Map<Id<Zone>, Map<Id<Vehicle>, Vehicle>> vehiclesInZones;
    private final Map<Id<Link>, ZoneCrossing> zoneCrossings = new HashMap<>();
    private final Map<Id<Link>, Zone> linkToZone = new HashMap<>();


    public VehicleZonalRegistry(Network network, ZonalSystem zonalSystem)
    {
        this.network = network;
        this.zonalSystem = zonalSystem;

        vehiclesInZones = Maps.newHashMapWithExpectedSize(zonalSystem.getZones().size());
        for (Id<Zone> id : zonalSystem.getZones().keySet()) {
            vehiclesInZones.put(id, new HashMap<Id<Vehicle>, Vehicle>());
        }

        preProcessNetwork();
    }


    private void preProcessNetwork()
    {
        for (Link l : network.getLinks().values()) {
            Zone fromZone = zonalSystem.getZone(l.getFromNode());
            Zone toZone = zonalSystem.getZone(l.getToNode());

            linkToZone.put(l.getId(), toZone);
            if (fromZone != toZone) {
                zoneCrossings.put(l.getId(), new ZoneCrossing(fromZone, toZone));
            }
        }
    }


    public void addVehicle(Vehicle vehicle)
    {
        Id<Link> linkId = vehicle.getAgentLogic().getDynAgent().getCurrentLinkId();
        Id<Zone> zoneId = getZoneId(linkId);
        vehiclesInZones.get(zoneId).put(vehicle.getId(), vehicle);
    }


    //in reaction to: movedOverNode();
    public void vehicleMovedOverNode(Vehicle vehicle, Id<Link> newLinkId)
    {
        ZoneCrossing cellCrossing = zoneCrossings.get(newLinkId);
        if (cellCrossing != null) {
            vehiclesInZones.get(cellCrossing.fromZone.getId()).remove(vehicle.getId());
            vehiclesInZones.get(cellCrossing.toZone.getId()).put(vehicle.getId(), vehicle);
        }
    }


    //also call when: arrivedOnLinkByNonNetworkMode();
    public void vehicleArrivedOnLinkByNonNetworkMode(Vehicle vehicle, Id<Link> newLinkId)
    {
        throw new UnsupportedOperationException();
    }


    public void removeVehicle(Vehicle vehicle)
    {
        Id<Link> linkId = vehicle.getAgentLogic().getDynAgent().getCurrentLinkId();
        Id<Zone> zoneId = getZoneId(linkId);
        vehiclesInZones.get(zoneId).remove(vehicle.getId());
    }


    protected Id<Zone> getZoneId(Id<Link> linkId)
    {
        return linkToZone.get(linkId).getId();
    }


    public Map<Id<Vehicle>, Vehicle> getVehicles(Node node)
    {
        return vehiclesInZones.get(zonalSystem.getZone(node).getId());
    }
}