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

import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.util.LongEnumAdder;


public class HourlyTaxiStats
    extends TaxiStats
{
    //for submission made during this hour
    //public final DescriptiveStatistics passengerWaitTime = new DescriptiveStatistics();

    //duration of all task types within this hour
    //cannot use DescriptiveStatistics, because some tasks span over two or more hours
    //
    //each vehicle's contribution is between 0 and 3600 s (vehicle may not operate all the time)
    //similar (though slightly less accurate) results can be obtained by averaging time profile
    //values for each hour
    //
    //due to integer overflow, works up to 596,523 taxis
    //public final LongEnumAdder<TaxiTask.TaxiTaskType> taskTimeSumsByType;

    //hourly vehicle's empty ratio
    //for drives that started in this hour; expect high variations:
    //can be 1.0 if a single empty drive started just before the end of this hour;
    //can also be 0.0 if a single occupied drive started just after the beginning of this hour
    //public final DescriptiveStatistics vehicleEmptyDriveRatio = new DescriptiveStatistics();

    //
    //some vehicles may operate for less than 3600 s, which may bias these two statistics
    //if this effect is not desired, consider using taskTypeSums instead
    //public final DescriptiveStatistics vehicleStayRatio = new DescriptiveStatistics();

    public HourlyTaxiStats(int hour)
    {
        super(hour + "", new LongEnumAdder<>(TaxiTask.TaxiTaskType.class));
    }
}