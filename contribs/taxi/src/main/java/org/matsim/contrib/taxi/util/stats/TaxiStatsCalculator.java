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

package org.matsim.contrib.taxi.util.stats;

import java.util.*;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.util.LongEnumAdder;

import com.google.common.collect.Maps;


public class TaxiStatsCalculator
{
    private final int hours;
    private final HourlyTaxiStats[] hourlyStats;
    private final DailyTaxiStats dailyStats = new DailyTaxiStats();
    private final Map<String, TaxiStats> taxiStats;


    public TaxiStatsCalculator(Iterable<? extends Vehicle> vehicles)
    {
        hours = calcHours(vehicles);
        hourlyStats = new HourlyTaxiStats[hours];
        for (int h = 0; h < hours; h++) {
            hourlyStats[h] = new HourlyTaxiStats(h);
        }

        Map<String, TaxiStats> allStats = Maps.newLinkedHashMapWithExpectedSize(hours + 1);
        for (HourlyTaxiStats s : hourlyStats) {
            allStats.put(s.id, s);
        }
        allStats.put(dailyStats.id, dailyStats);
        taxiStats = Collections.unmodifiableMap(allStats);

        for (Vehicle v : vehicles) {
            updateStatsForVehicle(v);
        }
    }


    private int calcHours(Iterable<? extends Vehicle> vehicles)
    {
        double maxEndTime = 0;
        for (Vehicle v : vehicles) {
            double endTime = v.getSchedule().getEndTime();
            if (endTime > maxEndTime) {
                maxEndTime = endTime;
            }
        }

        return (int)Math.ceil(maxEndTime / 3600);
    }


    public Map<String, TaxiStats> getTaxiStats()
    {
        return taxiStats;
    }


    public HourlyTaxiStats getHourlyStats(int hour)
    {
        return hourlyStats[hour];
    }


    public DailyTaxiStats getDailyStats()
    {
        return dailyStats;
    }


    private void updateStatsForVehicle(Vehicle vehicle)
    {
        Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(vehicle.getSchedule());
        if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
            return;// do not evaluate - the vehicle is unused
        }

        @SuppressWarnings("unchecked")
        LongEnumAdder<TaxiTask.TaxiTaskType>[] vehicleHourlySums = new LongEnumAdder[hours];
        int hourIdx = hour(schedule.getBeginTime());

        for (TaxiTask t : schedule.getTasks()) {
            double from = t.getBeginTime();

            int toHour = hour(t.getEndTime());
            for (; hourIdx < toHour; hourIdx++) {
                double to = (hourIdx + 1) * 3600;
                includeTaskIntoHourlySums(vehicleHourlySums, hourIdx, t, from, to);
                from = to;
            }
            includeTaskIntoHourlySums(vehicleHourlySums, toHour, t, from, t.getEndTime());

            if (t.getTaxiTaskType() == TaxiTaskType.PICKUP) {
                Request req = ((TaxiPickupTask)t).getRequest();
                double waitTime = Math.max(t.getBeginTime() - req.getT0(), 0);
                int hour = hour(req.getT0());
                hourlyStats[hour].passengerWaitTime.addValue(waitTime);
                dailyStats.passengerWaitTime.addValue(waitTime);
            }
        }

        includeVehicleHourlySumsIntoStats(vehicleHourlySums);
    }


    private int hour(double time)
    {
        return (int) (time / 3600);
    }


    private void includeTaskIntoHourlySums(LongEnumAdder<TaxiTask.TaxiTaskType>[] hourlySums, int hour,
            TaxiTask task, double fromTime, double toTime)
    {
        if (fromTime < toTime) {
            if (hourlySums[hour] == null) {
                hourlySums[hour] = new LongEnumAdder<>(TaxiTask.TaxiTaskType.class);
            }
            hourlySums[hour].add(task.getTaxiTaskType(), (long)(toTime - fromTime));
        }
    }


    private void includeVehicleHourlySumsIntoStats(LongEnumAdder<TaxiTaskType>[] vehicleHourlySums)
    {
        LongEnumAdder<TaxiTaskType> vehicleDailySums = new LongEnumAdder<>(TaxiTaskType.class);

        for (int h = 0; h < hours; h++) {
            LongEnumAdder<TaxiTask.TaxiTaskType> vhs = vehicleHourlySums[h];
            if (vhs != null && vhs.getLongTotal() > 0) {
                updateTaxiStats(hourlyStats[h], vhs);
                vehicleDailySums.addAll(vhs);
            }
        }

        updateTaxiStats(dailyStats, vehicleDailySums);
    }


    private void updateTaxiStats(TaxiStats taxiStats, LongEnumAdder<TaxiTaskType> vehicleSums)
    {
        updateEmptyDriveRatio(taxiStats.vehicleEmptyDriveRatio, vehicleSums);
        updateStayRatio(taxiStats.vehicleStayRatio, vehicleSums);
        taxiStats.taskTimeSumsByType.addAll(vehicleSums);
    }


    private void updateEmptyDriveRatio(DescriptiveStatistics emptyDriveRatioStats,
            LongEnumAdder<TaxiTaskType> durations)
    {
        double empty = durations.getLong(TaxiTaskType.EMPTY_DRIVE);
        double occupied = durations.getLong(TaxiTaskType.OCCUPIED_DRIVE);

        if (empty != 0 || occupied != 0) {
            double emptyRatio = empty / (empty + occupied);
            emptyDriveRatioStats.addValue(emptyRatio);
        }
    }


    private void updateStayRatio(DescriptiveStatistics stayRatioStats,
            LongEnumAdder<TaxiTaskType> durations)
    {
        double total = durations.getLongTotal();
        if (total != 0) {
            double stayRatio = durations.getLong(TaxiTaskType.STAY) / total;
            stayRatioStats.addValue(stayRatio);
        }
    }
}
