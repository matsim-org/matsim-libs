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

package org.matsim.contrib.taxi.util.stats;

import java.util.EnumMap;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;


public class TaxiStats
{
    final DescriptiveStatistics passengerWaitTimes = new DescriptiveStatistics();
    final EnumMap<TaxiTaskType, DescriptiveStatistics> timesByTaskType;


    public TaxiStats()
    {
        timesByTaskType = new EnumMap<>(TaxiTaskType.class);
        for (TaxiTaskType t : TaxiTaskType.values()) {
            timesByTaskType.put(t, new DescriptiveStatistics());
        }
    }


    public void addTask(TaxiTask task)
    {
        double time = task.getEndTime() - task.getBeginTime();
        timesByTaskType.get(task.getTaxiTaskType()).addValue(time);
    }
    
    
    public DescriptiveStatistics getPassengerWaitTimes()
    {
        return passengerWaitTimes;
    }


    public double getEmptyDriveRatio()
    {
        double empty = timesByTaskType.get(TaxiTaskType.EMPTY_DRIVE).getSum();//not mean!
        double occupied = timesByTaskType.get(TaxiTaskType.OCCUPIED_DRIVE).getSum();//not mean!
        return empty / (empty + occupied);
    }


    public double getStayTime()
    {
        return timesByTaskType.get(TaxiTaskType.STAY).getSum();//not mean!
    }


    public DescriptiveStatistics getOccupiedDriveTimes()
    {
        return timesByTaskType.get(TaxiTaskType.OCCUPIED_DRIVE);
    }
}