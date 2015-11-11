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
public class VehicleZonalRegistry
{
    private static class ZoneCrossing
    {
        private final int fromIdx;
        private final int toIdx;


        private ZoneCrossing(int fromIdx, int toIdx)
        {
            this.fromIdx = fromIdx;
            this.toIdx = toIdx;
        }
    }


    private final Network network;
    private final ZonalSystem zonalSystem;

    private final Map<Id<Vehicle>, Vehicle>[] vehiclesInZone;
    private final Map<Id<Link>, ZoneCrossing> zoneCrossings = new HashMap<>();
    private final Map<Id<Link>, Integer> linkToZone = new HashMap<>();


    @SuppressWarnings("unchecked")
    public VehicleZonalRegistry(Network network, ZonalSystem zonalSystem)
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
            int fromCellIdx = zonalSystem.getZoneIdx(l.getFromNode());
            int toCellIdx = zonalSystem.getZoneIdx(l.getToNode());

            linkToZone.put(l.getId(), toCellIdx);
            if (fromCellIdx != toCellIdx) {
                zoneCrossings.put(l.getId(), new ZoneCrossing(fromCellIdx, toCellIdx));
            }
        }
    }


    public void addVehicle(Vehicle vehicle)
    {
        Id<Link> linkId = vehicle.getAgentLogic().getDynAgent().getCurrentLinkId();
        int cellIdx = getCellIdx(linkId);
        vehiclesInZone[cellIdx].put(vehicle.getId(), vehicle);
    }


    //in reaction to: movedOverNode();
    public void vehicleMovedOverNode(Vehicle vehicle, Id<Link> newLinkId)
    {
        ZoneCrossing cellCrossing = zoneCrossings.get(newLinkId);
        if (cellCrossing != null) {
            vehiclesInZone[cellCrossing.fromIdx].remove(vehicle.getId());
            vehiclesInZone[cellCrossing.toIdx].put(vehicle.getId(), vehicle);
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
        int cellIdx = getCellIdx(linkId);
        vehiclesInZone[cellIdx].remove(vehicle.getId());
    }


    protected int getCellIdx(Id<Link> linkId)
    {
        return linkToZone.get(linkId);
    }
    
    
    public Map<Id<Vehicle>, Vehicle> getVehicles(Node node)
    {
        return vehiclesInZone[zonalSystem.getZoneIdx(node)];
    }
}