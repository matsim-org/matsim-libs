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

package org.matsim.contrib.dvrp.extensions.electric;

import java.util.*;


public class ChargingScheduleImpl<T extends ChargeTask>
    implements ChargingSchedule<T>
{
    private final Charger chargingStation;

    private final List<T> tasks;
    private final List<T> unmodifiableTasks;

    private T currentTask;


    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ChargingScheduleImpl(Charger chargingStation)
    {
        this.chargingStation = chargingStation;

        tasks = new ArrayList<>();
        unmodifiableTasks = (List)Collections.unmodifiableList(tasks);

        currentTask = null;
    }


    @Override
    public Charger getCharger()
    {
        return chargingStation;
    }


    @Override
    public List<T> getTasks()
    {
        return unmodifiableTasks;
    }


    @Override
    public void addTask(T task)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void removeTask(T task)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public T getCurrentTask()
    {
        return currentTask;
    }
}
