/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.util.EnumAdder;


public class TaxiStats
{
    public final String id;

    public final DescriptiveStatistics passengerWaitTime = new DescriptiveStatistics();

    public final EnumAdder<TaxiTask.TaxiTaskType, ?> taskTimeSumsByType;

    public final DescriptiveStatistics vehicleEmptyDriveRatio = new DescriptiveStatistics();

    public final DescriptiveStatistics vehicleStayRatio = new DescriptiveStatistics();


    public TaxiStats(String id, EnumAdder<TaxiTask.TaxiTaskType, ?> taskTimeSumsByType)
    {
        this.id = id;
        this.taskTimeSumsByType = taskTimeSumsByType;
    }


    public double getFleetEmptyDriveRatio()
    {
        double empty = taskTimeSumsByType.get(TaxiTask.TaxiTaskType.EMPTY_DRIVE).doubleValue();
        double occupied = taskTimeSumsByType.get(TaxiTask.TaxiTaskType.OCCUPIED_DRIVE).doubleValue();
        return empty / (empty + occupied);
    }


    public double getFleetStayRatio()
    {
        double stay = taskTimeSumsByType.get(TaxiTask.TaxiTaskType.STAY).doubleValue();
        double total = taskTimeSumsByType.getTotal().doubleValue();
        return stay / total;
    }


    public double getOccupiedDriveRatio()
    {
        double occupied = taskTimeSumsByType.get(TaxiTaskType.OCCUPIED_DRIVE).doubleValue();
        double total = taskTimeSumsByType.getTotal().doubleValue();
        return occupied / total;
    }
}