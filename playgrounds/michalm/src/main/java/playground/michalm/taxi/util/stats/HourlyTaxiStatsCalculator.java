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

package playground.michalm.taxi.util.stats;

import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;

import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;


public class HourlyTaxiStatsCalculator
{
    private final TaxiStats[] stats;


    public HourlyTaxiStatsCalculator(Iterable<? extends Vehicle> vehicles, int hours)
    {
        stats = new TaxiStats[hours];
        for (int i = 0; i < hours; i++) {
            stats[i] = new TaxiStats();
        }

        for (Vehicle v : vehicles) {
            calculateStatsImpl(v);
        }
    }


    public TaxiStats[] getStats()
    {
        return stats;
    }


    private void calculateStatsImpl(Vehicle vehicle)
    {
        Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(vehicle.getSchedule());

        if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
            return;// do not evaluate - the vehicle is unused
        }

        for (TaxiTask t : schedule.getTasks()) {
            stats[hour(t.getBeginTime())].addTask(t);

            if (t.getTaxiTaskType() == TaxiTaskType.PICKUP) {
                Request req = ((TaxiPickupTask)t).getRequest();
                double waitTime = Math.max(t.getBeginTime() - req.getT0(), 0);
                stats[hour(req.getT0())].passengerWaitTimes.addValue(waitTime);
            }
        }
    }
    
    
    private int hour(double time)
    {
        return (int)(time / 3600);
    }
}
