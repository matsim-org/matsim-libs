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

package org.matsim.contrib.dvrp.data;

import java.util.*;


public class VehicleCounter
{
    private final List<Vehicle> vehicles;
    private final Queue<Vehicle> waitingVehicles;
    private final Queue<Vehicle> activeVehicles;


    public VehicleCounter(List<Vehicle> vehicles)
    {
        this.vehicles = vehicles;

        int queueCapacity = vehicles.size();
        this.waitingVehicles = new PriorityQueue<Vehicle>(queueCapacity, Vehicles.T0_COMPARATOR);
        this.activeVehicles = new PriorityQueue<Vehicle>(queueCapacity, Vehicles.T1_COMPARATOR);
    }


    public List<Integer> countVehiclesOverTime(double timeStep)
    {
        List<Integer> vehicleCounts = new ArrayList<>();
        double currentTime = 0;
        waitingVehicles.addAll(vehicles);

        while (!waitingVehicles.isEmpty() || !activeVehicles.isEmpty()) {
            //move waiting->active
            while (!waitingVehicles.isEmpty()) {
                if (waitingVehicles.peek().getT0() > currentTime) {
                    break;
                }

                Vehicle newActiveVehicle = waitingVehicles.poll();
                activeVehicles.add(newActiveVehicle);
            }

            //remove from active
            while (!activeVehicles.isEmpty()) {
                if (activeVehicles.peek().getT1() > currentTime) {
                    break;
                }

                activeVehicles.poll();
            }

            vehicleCounts.add(activeVehicles.size());
            currentTime += timeStep;
        }

        return vehicleCounts;
    }
}
