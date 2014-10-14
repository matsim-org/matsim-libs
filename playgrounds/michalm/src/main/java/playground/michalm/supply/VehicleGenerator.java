/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.supply;

import java.util.*;

import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.matsim.contrib.dvrp.data.Vehicle;

import pl.poznan.put.util.random.*;


public class VehicleGenerator
{
    private final UniformRandom uniform = RandomUtils.getGlobalUniform();
    private final List<Vehicle> vehicles = new ArrayList<Vehicle>();

    private final double minWorkTime;
    private final double maxWorkTime;
    private final VehicleCreator vehicleCreator;

    private Queue<Vehicle> activeVehicles;
    private double periodDuration;
    private double currentTimePeriod;


    public VehicleGenerator(double minWorkTime, double maxWorkTime,
            VehicleCreator vehicleCreator)
    {
        this.minWorkTime = minWorkTime;
        this.maxWorkTime = maxWorkTime;
        this.vehicleCreator = vehicleCreator;
    }


    public void generateVehicles(double[] vehicleCounts, double startTime, double periodDuration)
    {
        initPeriodDuration(periodDuration);
        initQueue(vehicleCounts);

        currentTimePeriod = startTime;
        for (int i = 0; i < vehicleCounts.length; i++) {
            removeVehiclesOnT1();

            int vehsToAdd = calculateNumberOfVehiclesToAdd(vehicleCounts[i]);
            
            if (vehsToAdd > 0) {
                addVehicles(vehsToAdd);
            }
            else {
                removeVehiclesBeforeT1(-vehsToAdd);
            }

            currentTimePeriod += periodDuration;
        }

        removeVehiclesOnT1();
        removeVehiclesBeforeT1(-calculateNumberOfVehiclesToAdd(0));
    }


    private void initPeriodDuration(double periodDuration)
    {
        if (periodDuration > minWorkTime) {
            throw new IllegalArgumentException();
        }

        this.periodDuration = periodDuration;
    }


    private void initQueue(double[] vehicleCounts)
    {
        int queueCapacity = (int)new Max().evaluate(vehicleCounts) + 1;
        activeVehicles = new PriorityQueue<Vehicle>(queueCapacity, new Comparator<Vehicle>() {
            public int compare(Vehicle v1, Vehicle v2)
            {
                int diff = Double.compare(v1.getT1(), v2.getT1());
                return diff != 0 ? diff : Double.compare(v1.getT0(), v2.getT0());
            }
        });
    }


    private void removeVehiclesOnT1()
    {
        double maxT1 = currentTimePeriod + periodDuration;
        while (!activeVehicles.isEmpty()) {
            if (activeVehicles.peek().getT1() > maxT1) {
                return;
            }

            activeVehicles.poll();
        }
    }


    private int calculateNumberOfVehiclesToAdd(double vehicleCount)
    {
        if (vehicleCount < 0) {
            throw new IllegalArgumentException();
        }

        return (int)uniform.floorOrCeil(vehicleCount) - activeVehicles.size();
    }


    private void addVehicles(int count)
    {
        double maxT0 = currentTimePeriod + periodDuration;
        for (int i = 0; i < count; i++) {
            double t0 = uniform.nextDouble(currentTimePeriod, maxT0);
            double workTime = uniform.nextDouble(minWorkTime, maxWorkTime);
            Vehicle veh = vehicleCreator.createVehicle(t0, t0 + workTime);
            
            activeVehicles.add(veh);
            vehicles.add(veh);
        }
    }


    private void removeVehiclesBeforeT1(int count)
    {
        double maxT1 = currentTimePeriod + periodDuration;
        for (int i = 0; i < count; i++) {
            Vehicle veh = activeVehicles.poll();
            veh.setT1(uniform.nextDouble(currentTimePeriod, maxT1));
        }
    }


    public List<Vehicle> getVehicles()
    {
        return vehicles;
    }
}
