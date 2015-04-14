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

package org.matsim.contrib.dvrp.schedule;

import java.util.List;

import org.matsim.contrib.dvrp.data.Vehicle;


public interface Schedule<T extends Task>
{
    public enum ScheduleStatus
    {
        UNPLANNED, PLANNED, STARTED, COMPLETED;
    };


    Vehicle getVehicle();


    List<T> getTasks();// unmodifiableList


    int getTaskCount();


    T getCurrentTask();


    ScheduleStatus getStatus();


    double getBeginTime();


    double getEndTime();


    // schedule modification functionality:

    void addTask(T task);


    void addTask(int taskIdx, T task);


    void removeLastTask();


    void removeTask(T task);


    T nextTask();//this one seems synchronous (will be executed when switching between DynActions)
}
