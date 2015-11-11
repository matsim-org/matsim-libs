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

package playground.michalm.taxi.optimizer.rules;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.data.Vehicle;

import playground.michalm.taxi.schedule.TaxiStayTask;
import playground.michalm.zone.util.*;


public class IdleTaxiZonalRegistry
{
    private final ZonalSystem zonalSystem;

    private final Map<Id<Vehicle>, Vehicle>[] vehiclesInZones;
    private final Map<Id<Vehicle>, Vehicle> vehicles = new LinkedHashMap<>();


    @SuppressWarnings("unchecked")
    public IdleTaxiZonalRegistry(ZonalSystem zonalSystem)
    {
        this.zonalSystem = zonalSystem;

        this.vehiclesInZones = (Map<Id<Vehicle>, Vehicle>[])new Map[zonalSystem.getZoneCount()];
        for (int i = 0; i < vehiclesInZones.length; i++) {
            vehiclesInZones[i] = new HashMap<>();
        }
    }


    public void addVehicle(Vehicle vehicle)
    {
        int zoneIdx = getZone(vehicle);

        if (vehiclesInZones[zoneIdx].put(vehicle.getId(), vehicle) != null) {
            throw new RuntimeException("The vehicle was already there");
        }

        if (vehicles.put(vehicle.getId(), vehicle) != null) {
            throw new RuntimeException("The vehicle was already there");
        }
    }


    public void removeVehicle(Vehicle vehicle)
    {
        int zoneIdx = getZone(vehicle);

        if (vehiclesInZones[zoneIdx].remove(vehicle.getId()) == null) {
            throw new RuntimeException("The vehicle was not there");
        }

        if (vehicles.remove(vehicle.getId()) == null) {
            throw new RuntimeException("The vehicle was not there");
        }
    }


    public Iterable<Vehicle> findNearestVehicles(Node node, int minCount)
    {
        Iterable<Integer> zonesIdxByDistance = ((SquareGridSystem)zonalSystem)
                .getZonesIdxByDistance(node);
        List<Vehicle> nearestVehs = new ArrayList<>();

        for (int idx : zonesIdxByDistance) {
            nearestVehs.addAll(vehiclesInZones[idx].values());
            if (nearestVehs.size() >= minCount) {
                return nearestVehs;
            }
        }

        return nearestVehs;
    }


    private int getZone(Vehicle vehicle)
    {
        TaxiStayTask stayTask = (TaxiStayTask)vehicle.getSchedule().getCurrentTask();
        return zonalSystem.getZoneIdx(stayTask.getLink().getToNode());
    }


    public Map<Id<Vehicle>, Vehicle> getVehiclesInZone(Node node)
    {
        return vehiclesInZones[zonalSystem.getZoneIdx(node)];//TODO return immutables?
    }


    public Map<Id<Vehicle>, Vehicle> getVehiclesInZone(int zoneIdx)
    {
        return vehiclesInZones[zoneIdx];//TODO return immutables?
    }
    
    
    public Iterable<Vehicle> getVehicles()
    {
        return vehicles.values();
    }


    public int getVehicleCount()
    {
        return vehicles.size();
    }
}
