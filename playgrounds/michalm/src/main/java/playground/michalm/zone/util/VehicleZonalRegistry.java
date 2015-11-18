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

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.data.Vehicle;

//TODO never used anywhere...
public class VehicleZonalRegistry<Z extends ZonalSystem.Zone>
{
    private static class ZoneCrossing<Z extends ZonalSystem.Zone>
    {
        private final Z fromZone;
        private final Z toZone;


        private ZoneCrossing(Z fromZone, Z toZone)
        {
            this.fromZone = fromZone;
            this.toZone = toZone;
        }
    }


    private final Network network;
    private final ZonalSystem<Z> zonalSystem;

    private final Map<Id<Vehicle>, Vehicle>[] vehiclesInZone;
    private final Map<Id<Link>, ZoneCrossing<Z>> zoneCrossings = new HashMap<>();
    private final Map<Id<Link>, Z> linkToZone = new HashMap<>();


    @SuppressWarnings("unchecked")
    public VehicleZonalRegistry(Network network, ZonalSystem<Z> zonalSystem)
    {
        this.network = network;
        this.zonalSystem = zonalSystem;

        vehiclesInZone = (Map<Id<Vehicle>, Vehicle>[])new Map[zonalSystem.getZoneCount()];
        for (int i = 0; i < vehiclesInZone.length; i++) {
            vehiclesInZone[i] = new HashMap<>();
        }
        
        preProcessNetwork();
    }


    private void preProcessNetwork()
    {
        for (Link l : network.getLinks().values()) {
            Z fromZone = zonalSystem.getZone(l.getFromNode());
            Z toZone = zonalSystem.getZone(l.getToNode());

            linkToZone.put(l.getId(), toZone);
            if (fromZone != toZone) {
                zoneCrossings.put(l.getId(), new ZoneCrossing<>(fromZone, toZone));
            }
        }
    }


    public void addVehicle(Vehicle vehicle)
    {
        Id<Link> linkId = vehicle.getAgentLogic().getDynAgent().getCurrentLinkId();
        int cellIdx = getZoneIdx(linkId);
        vehiclesInZone[cellIdx].put(vehicle.getId(), vehicle);
    }


    //in reaction to: movedOverNode();
    public void vehicleMovedOverNode(Vehicle vehicle, Id<Link> newLinkId)
    {
        ZoneCrossing<Z> cellCrossing = zoneCrossings.get(newLinkId);
        if (cellCrossing != null) {
            vehiclesInZone[cellCrossing.fromZone.getIdx()].remove(vehicle.getId());
            vehiclesInZone[cellCrossing.toZone.getIdx()].put(vehicle.getId(), vehicle);
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
        int cellIdx = getZoneIdx(linkId);
        vehiclesInZone[cellIdx].remove(vehicle.getId());
    }


    protected int getZoneIdx(Id<Link> linkId)
    {
        return linkToZone.get(linkId).getIdx();
    }
    
    
    public Map<Id<Vehicle>, Vehicle> getVehicles(Node node)
    {
        return vehiclesInZone[zonalSystem.getZone(node).getIdx()];
    }
}