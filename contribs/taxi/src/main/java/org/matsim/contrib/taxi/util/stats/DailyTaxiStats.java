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

import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.util.DoubleEnumAdder;


public class DailyTaxiStats
    extends TaxiStats
{
    //public final DescriptiveStatistics passengerWaitTime = new DescriptiveStatistics();

    //needs to be double instead of long due to integer overflow
    //20k taxis each with 30-h stay tasks would cause an overflow
    //public final DoubleEnumAdder<TaxiTask.TaxiTaskType> taskTimeSumsByType

    //daily vehicle's empty ratio
    //public final DescriptiveStatistics vehicleEmptyDriveRatio = new DescriptiveStatistics();

    //daily vehicle's empty ratio
    //public final DescriptiveStatistics vehicleStayRatio = new DescriptiveStatistics();

    public DailyTaxiStats()
    {
        super("daily", new DoubleEnumAdder<>(TaxiTask.TaxiTaskType.class));
    }

}
