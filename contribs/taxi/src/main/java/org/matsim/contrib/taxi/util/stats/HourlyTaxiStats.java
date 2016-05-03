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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.util.*;


public class HourlyTaxiStats
{
    public enum StayRatioLevel
    {
        // [from; to)
        L0_1, //== approx. "always busy"; 0pct is impossible due to 1-sec stay tasks
        L1_25, L25_50, // both busy for most of the time
        L50_75, L75_100, //both idle for most of the time 
        L100; //== always idle

        public static StayRatioLevel getLevel(double ratio)
        {
            if (ratio < 0.01) {
                return L0_1;
            }

            return values()[(int) (ratio * 4) + 1];//projection: double(0..1) to int(0..4)
        }
    }


    public final int hour;

    //for submission made during this hour
    public final DescriptiveStatistics passengerWaitTime = new DescriptiveStatistics();

    //duration of all task types;
    //each vehicle's contribution is between 0 and 3600 s (vehicle may not operate all the time)
    //similar (though slightly less accurate) results can be obtained by averaging time profile
    //values for each hour
    //due to integer overflow, works up to almost 600k taxis
    public final EnumAdder<TaxiTask.TaxiTaskType> taskTypeSums = new EnumAdder<>(
            TaxiTask.TaxiTaskType.class);

    //for drives that started in this hour; expect high variations:
    //can be 1.0 if a single empty drive started just before the end of this hour;
    //can also be 0.0 if a single occupied drive started just after the beginning of this hour
    public final DescriptiveStatistics emptyDriveRatio = new DescriptiveStatistics();

    //some vehicles may operate for less than 3600 s, which may bias these two statistics
    //consider using taskTypeSums instead
    public final DescriptiveStatistics stayRatio = new DescriptiveStatistics();
    public final EnumCounter<StayRatioLevel> stayRatioLevelCounter = new EnumCounter<>(
            StayRatioLevel.class);


    public HourlyTaxiStats(int hour)
    {
        this.hour = hour;
    }
    
    
    void incrementStayRatioLevelCounter(double stayRatio)
    {
        stayRatioLevelCounter.increment(StayRatioLevel.getLevel(stayRatio));
    }
}