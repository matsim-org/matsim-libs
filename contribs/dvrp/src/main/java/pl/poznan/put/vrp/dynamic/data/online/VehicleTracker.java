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

package pl.poznan.put.vrp.dynamic.data.online;

import pl.poznan.put.vrp.dynamic.data.network.*;
import pl.poznan.put.vrp.dynamic.data.schedule.DriveTask;


public interface VehicleTracker
{
    DriveTask getDriveTask();


    Vertex getLastPosition();


    int getLastPositionTime();


    Vertex predictNextPosition(int currentTime);


    int predictNextPositionTime(int currentTime);


    int predictEndTime(int currentTime);


    int getInitialEndTime();


    /**
     * Delay relative to the initial driveTask.getEndTime() (the end time my be updated
     * periodically, thus driveTask.getEndTime() may return different results over time)
     */
    int calculateCurrentDelay(int currentTime);
}
