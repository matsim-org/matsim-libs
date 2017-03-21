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

package playground.jbischoff.taxi.inclusion.optimizer;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.zone.*;

import com.google.common.base.Predicate;
import com.google.common.collect.*;


public class InclusionIdleTaxiZonalRegistry
{
    private final ZonalSystem zonalSystem;
    private final Map<Id<Zone>, List<Zone>> zonesSortedByDistance;

    private final Map<Id<Zone>, Map<Id<Vehicle>, Vehicle>> vehiclesInZones;
    private final Map<Id<Vehicle>, Vehicle> vehicles = new LinkedHashMap<>();

    private final Predicate<Vehicle> isIdle;
    private final Predicate<Vehicle> isIdleAndBarrierFree;
    private final String barrierFreeTaxiDesignator;


    public InclusionIdleTaxiZonalRegistry(ZonalSystem zonalSystem, ScheduleInquiry scheduleInquiry,String barrierFreeTaxiDesignator)
    {
    	this.barrierFreeTaxiDesignator = barrierFreeTaxiDesignator;
        this.zonalSystem = zonalSystem;
        zonesSortedByDistance = ZonalSystems.initZonesByDistance(zonalSystem.getZones());

        isIdle = ScheduleInquiries.createIsIdle(scheduleInquiry);
        isIdleAndBarrierFree = createIsIdleAndBarrierFreePredicate(scheduleInquiry);
        
        vehiclesInZones = Maps.newHashMapWithExpectedSize(zonalSystem.getZones().size());
        for (Id<Zone> id : zonalSystem.getZones().keySet()) {
            vehiclesInZones.put(id, new HashMap<Id<Vehicle>, Vehicle>());
        }
    }


    public void addVehicle(Vehicle vehicle)
    {
        TaxiStayTask stayTask = (TaxiStayTask)vehicle.getSchedule().getCurrentTask();
        Id<Zone> zoneId = getZoneId(stayTask);

        if (vehiclesInZones.get(zoneId).put(vehicle.getId(), vehicle) != null) {
            throw new IllegalStateException(vehicle + " is already in the registry");
        }

        if (vehicles.put(vehicle.getId(), vehicle) != null) {
            throw new IllegalStateException(vehicle + " is already in the registry");
        }
    }


    public void removeVehicle(Vehicle vehicle)
    {
        TaxiStayTask stayTask = (TaxiStayTask)Schedules.getPreviousTask(vehicle.getSchedule());
        Id<Zone> zoneId = getZoneId(stayTask);

        if (vehiclesInZones.get(zoneId).remove(vehicle.getId()) == null) {
            throw new IllegalStateException(vehicle + " is not in the registry");
        }

        if (vehicles.remove(vehicle.getId()) == null) {
            throw new IllegalStateException(vehicle + " is not in the registry");
        }
    }


    public Iterable<Vehicle> findNearestVehicles(Node node, boolean needsSpecialVehicle)
    {
        

        Zone zone = zonalSystem.getZone(node);
        Iterable<? extends Zone> zonesByDistance = zonesSortedByDistance.get(zone.getId());
        List<Vehicle> nearestVehs = new ArrayList<>();
        for (Zone z : zonesByDistance) {
        	if (needsSpecialVehicle){
        		
        		Iterables.addAll(nearestVehs,Iterables.filter(vehiclesInZones.get(z.getId()).values(), isIdleAndBarrierFree));
        		
        	}
        	else{
        		Iterables.addAll(nearestVehs,Iterables.filter(vehiclesInZones.get(z.getId()).values(), isIdle));
        	}

           
        }

        return nearestVehs;
    }


    private Id<Zone> getZoneId(TaxiStayTask stayTask)
    {
        return zonalSystem.getZone(stayTask.getLink().getToNode()).getId();
    }


    public Iterable<Vehicle> getVehicles()
    {
        return Iterables.filter(vehicles.values(), isIdle);
    }


    public int getVehicleCount()
    {
        return vehicles.size();
    }
    
    private Predicate<Vehicle> createIsIdleAndBarrierFreePredicate(final ScheduleInquiry scheduleInquiry)
    {
        return new Predicate<Vehicle>() {
            public boolean apply(Vehicle vehicle)
            {
            	if (vehicle.getId().toString().startsWith(barrierFreeTaxiDesignator)){
            		
            		return (scheduleInquiry.isIdle(vehicle));
                   	} 
            	else return false;
            }
        };
    }
}
