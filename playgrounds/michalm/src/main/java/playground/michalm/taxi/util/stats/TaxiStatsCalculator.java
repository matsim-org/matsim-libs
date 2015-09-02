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

import java.util.EnumMap;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;

import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;


public class TaxiStatsCalculator
{
    private final TaxiStats stats = new TaxiStats();


    public TaxiStatsCalculator(Iterable<? extends Vehicle> vehicles)
    {
        for (Vehicle v : vehicles) {
            calculateStatsImpl(v);
        }
    }


    public TaxiStats getStats()
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
            stats.addTime(t);

            if (t.getTaxiTaskType() == TaxiTaskType.PICKUP) {
                Request req = ((TaxiPickupTask)t).getRequest();
                double waitTime = Math.max(t.getBeginTime() - req.getT0(), 0);
                stats.passengerWaitTimes.addValue(waitTime);
            }
        }
    }


    public static class TaxiStats
    {
        public final DescriptiveStatistics passengerWaitTimes = new DescriptiveStatistics();
        public final EnumMap<TaxiTaskType, DescriptiveStatistics> timesByTaskType;


        public TaxiStats()
        {
            timesByTaskType = new EnumMap<>(TaxiTaskType.class);
            for (TaxiTaskType t : TaxiTaskType.values()) {
                timesByTaskType.put(t, new DescriptiveStatistics());
            }
        }


        public void addTime(TaxiTask task)
        {
            double time = task.getEndTime() - task.getBeginTime();
            timesByTaskType.get(task.getTaxiTaskType()).addValue(time);
        }


        public double getDriveEmptyRatio()
        {
            double empty = timesByTaskType.get(TaxiTaskType.DRIVE_EMPTY).getSum();//not mean!
            double withPassenger = timesByTaskType.get(TaxiTaskType.DRIVE_WITH_PASSENGER).getSum();//not mean!
            return empty / (empty + withPassenger);
        }


        public DescriptiveStatistics getDriveWithPassengerTimes()
        {
            return timesByTaskType.get(TaxiTaskType.DRIVE_WITH_PASSENGER);
        }


        public static final String HEADER = "WaitT\t" //
                + "MaxWaitT"//
                + "WithPassengerT"//
                + "%EmptyDrive\t";


        @Override
        public String toString()
        {
            return new StringBuilder()//
                    .append(passengerWaitTimes.getMean()).append('\t') //
                    .append(passengerWaitTimes.getMax()).append('\t') //
                    .append(getDriveWithPassengerTimes().getMean()).append('\t') //
                    .append(getDriveEmptyRatio()).append('\t') //
                    .toString();
        }
    }
}
