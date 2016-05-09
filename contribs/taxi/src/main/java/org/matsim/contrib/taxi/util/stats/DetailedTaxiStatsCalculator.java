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

import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.util.EnumAdder;


public class DetailedTaxiStatsCalculator
{
    private final int hours;
    private final HourlyTaxiStats[] hourlyStats;
    private final HourlyHistograms[] hourlyHistograms;
    private final DailyHistograms dailyHistograms;


    public DetailedTaxiStatsCalculator(Iterable<? extends Vehicle> vehicles)
    {
        hours = calcHours(vehicles);

        hourlyStats = new HourlyTaxiStats[hours];
        hourlyHistograms = new HourlyHistograms[hours];

        for (int h = 0; h < hours; h++) {
            hourlyStats[h] = new HourlyTaxiStats(h);
            hourlyHistograms[h] = new HourlyHistograms(h);
        }

        dailyHistograms = new DailyHistograms();

        for (Vehicle v : vehicles) {
            updateHourlyStatsForVehicle(v);
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


    public HourlyTaxiStats[] getHourlyStats()
    {
        return hourlyStats;
    }


    public HourlyHistograms[] getHourlyHistograms()
    {
        return hourlyHistograms;
    }


    public DailyHistograms getDailyHistograms()
    {
        return dailyHistograms;
    }


    private void updateHourlyStatsForVehicle(Vehicle vehicle)
    {
        Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(vehicle.getSchedule());
        if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
            return;// do not evaluate - the vehicle is unused
        }

        @SuppressWarnings("unchecked")
        EnumAdder<TaxiTask.TaxiTaskType>[] hourlySums = new EnumAdder[hours];

        int hourIdx = hour(schedule.getBeginTime());

        for (TaxiTask t : schedule.getTasks()) {
            double from = t.getBeginTime();

            int toHour = hour(t.getEndTime());
            for (; hourIdx < toHour; hourIdx++) {
                double to = (hourIdx + 1) * 3600;
                includeTaskIntoHourlySums(hourlySums, hourIdx, t, (int)(to - from));
                from = to;
            }

            includeTaskIntoHourlySums(hourlySums, toHour, t, (int)(t.getEndTime() - from));

            switch (t.getTaxiTaskType()) {
                case PICKUP:
                    Request req = ((TaxiPickupTask)t).getRequest();
                    double waitTime = Math.max(t.getBeginTime() - req.getT0(), 0);
                    int hour = hour(req.getT0());
                    hourlyStats[hour].passengerWaitTime.addValue(waitTime);
                    hourlyHistograms[hour].passengerWaitTime.addValue(waitTime);
                    break;

                case EMPTY_DRIVE:
                    double driveTime = t.getEndTime() - t.getBeginTime();
                    hour = hour(t.getBeginTime());
                    //                    hourlyStats[hour].emptyDriveTime.addValue(driveTime);
                    hourlyHistograms[hour].emptyDriveTime
                            .addValue(t.getEndTime() - t.getBeginTime());
                    break;

                case OCCUPIED_DRIVE:
                    driveTime = t.getEndTime() - t.getBeginTime();
                    hour = hour(t.getBeginTime());
                    //                    hourlyStats[hour].occupiedDriveTime.addValue(driveTime);
                    hourlyHistograms[hour].occupiedDriveTime.addValue(driveTime);

                default:
            }
        }

        updateHourlyStats(hourlySums);
    }


    private int hour(double time)
    {
        return (int) (time / 3600);
    }


    private void includeTaskIntoHourlySums(EnumAdder<TaxiTask.TaxiTaskType>[] hourlySums, int hour,
            TaxiTask task, int durationWithinHour)
    {
        if (durationWithinHour == 0) {
            return;
        }

        if (hourlySums[hour] == null) {
            hourlySums[hour] = new EnumAdder<>(TaxiTask.TaxiTaskType.class);
        }

        hourlySums[hour].add(task.getTaxiTaskType(), durationWithinHour);
    }


    private void updateHourlyStats(EnumAdder<TaxiTask.TaxiTaskType>[] hourlySums)
    {
        double dailyEmpty = 0;
        double dailyOccupied = 0;
        double dailyStay = 0;
        double dailyTotal = 0;

        for (int h = 0; h < hours; h++) {
            EnumAdder<TaxiTask.TaxiTaskType> vhs = hourlySums[h];
            if (vhs == null) {
                continue;
            }

            double empty = vhs.getSum(TaxiTaskType.EMPTY_DRIVE);
            double occupied = vhs.getSum(TaxiTaskType.OCCUPIED_DRIVE);
            double stay = vhs.getSum(TaxiTaskType.STAY);
            double total = vhs.getTotalSum();

            double emptyRatio = empty / (empty + occupied);
            double stayRatio = stay / total;

            dailyEmpty += empty;
            dailyOccupied += occupied;
            dailyStay += stay;
            dailyTotal += total;

            HourlyTaxiStats hs = hourlyStats[h];
            HourlyHistograms hh = hourlyHistograms[h];

            hs.taskTypeSums.addAll(vhs);

            if (!Double.isNaN(emptyRatio)) {
                hs.emptyDriveRatio.addValue(emptyRatio);
                hh.emptyDriveRatio.addValue(emptyRatio);
            }

            hs.stayRatio.addValue(stayRatio);
            hh.stayRatio.addValue(stayRatio);

            hs.incrementStayRatioLevelCounter(stayRatio);
        }

        double dailyEmptyRatio = dailyEmpty / (dailyEmpty + dailyOccupied);
        double dailyStayRatio = dailyStay / dailyTotal;

        dailyHistograms.emptyDriveRatio.addValue(dailyEmptyRatio);
        dailyHistograms.stayRatio.addValue(dailyStayRatio);
    }
}
