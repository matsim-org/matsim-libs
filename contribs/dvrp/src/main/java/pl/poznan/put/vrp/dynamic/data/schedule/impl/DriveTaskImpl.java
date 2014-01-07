/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package pl.poznan.put.vrp.dynamic.data.schedule.impl;

import org.matsim.contrib.dvrp.data.network.shortestpath.ShortestPath;

import pl.poznan.put.vrp.dynamic.data.online.*;
import pl.poznan.put.vrp.dynamic.data.schedule.DriveTask;


public class DriveTaskImpl
    extends AbstractTask
    implements DriveTask
{
    private final ShortestPath shortestPath;
    private VehicleTracker vehicleTracker;


    public DriveTaskImpl(ShortestPath shortestPath)
    {
        super(shortestPath.departureTime, shortestPath.getArrivalTime());
        this.shortestPath = shortestPath;

        vehicleTracker = new OfflineVehicleTracker(this);//by default; can be changed later
    }


    @Override
    public TaskType getType()
    {
        return TaskType.DRIVE;
    }


    @Override
    public ShortestPath getShortestPath()
    {
        return shortestPath;
    }


    @Override
    public VehicleTracker getVehicleTracker()
    {
        return vehicleTracker;
    }


    @Override
    public void setVehicleTracker(VehicleTracker vehicleTracker)
    {
        this.vehicleTracker = vehicleTracker;
    }


    @Override
    public String toString()
    {
        return "D(@" + shortestPath.getFromLink().getId() + "->@"
                + shortestPath.getToLink().getId() + ")" + commonToString();
    }
}