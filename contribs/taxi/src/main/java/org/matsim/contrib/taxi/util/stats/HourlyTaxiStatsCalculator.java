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


public class HourlyTaxiStatsCalculator
{
    private static class HourlyVehicleStats
    {
        private double empty = 0;
        private double pickup = 0;
        private double occupied = 0;
        private double dropoff = 0;
        private double stay = 0;


        private double total()
        {
            return empty + pickup + occupied + dropoff + stay;
        }
    }


    private final int hours;
    private final HourlyTaxiStats[] hourlyStats;
    private final HourlyHistograms[] hourlyHistograms;
    private final DailyHistograms dailyHistograms;


    public HourlyTaxiStatsCalculator(Iterable<? extends Vehicle> vehicles, int hours)
    {
        this.hours = hours;

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


    public HourlyTaxiStats[] getStats()
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


    public int getHours()
    {
        return hours;
    }


    private void updateHourlyStatsForVehicle(Vehicle vehicle)
    {
        Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(vehicle.getSchedule());
        if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
            return;// do not evaluate - the vehicle is unused
        }

        HourlyVehicleStats[] stats = new HourlyVehicleStats[hours];

        int hourIdx = hour(schedule.getBeginTime());

        for (TaxiTask t : schedule.getTasks()) {
            double from = t.getBeginTime();

            int toHour = hour(t.getEndTime());
            for (; hourIdx < toHour; hourIdx++) {
                double to = (hourIdx + 1) * 3600;
                updateHourlyVehicleStats(stats, hourIdx, t, to - from);

                from = to;
            }

            updateHourlyVehicleStats(stats, toHour, t, t.getEndTime() - from);

            switch (t.getTaxiTaskType()) {
                case PICKUP:
                    Request req = ((TaxiPickupTask)t).getRequest();
                    double waitTime = Math.max(t.getBeginTime() - req.getT0(), 0);
                    int hour = hour(req.getT0());
                    hourlyStats[hour].passengerWaitTime.addValue(waitTime);
                    hourlyHistograms[hour].passengerWaitTime.addValue(waitTime);
                    break;

                case DRIVE_EMPTY:
                    hour = hour(t.getBeginTime());
                    hourlyHistograms[hour].emptyDriveTime
                            .addValue(t.getEndTime() - t.getBeginTime());
                    break;

                case DRIVE_OCCUPIED:
                    hour = hour(t.getBeginTime());
                    hourlyHistograms[hour].occupiedDriveTime
                            .addValue(t.getEndTime() - t.getBeginTime());

                default:
            }
        }

        updateHourlyStats(stats);
    }


    private int hour(double time)
    {
        return (int) (time / 3600);
    }


    private void updateHourlyVehicleStats(HourlyVehicleStats[] stats, int hour, TaxiTask task,
            double durationWithinHour)
    {
        if (durationWithinHour == 0) {
            return;
        }

        if (stats[hour] == null) {
            stats[hour] = new HourlyVehicleStats();
        }

        switch (task.getTaxiTaskType()) {
            case DRIVE_EMPTY:
                stats[hour].empty += durationWithinHour;
                return;

            case PICKUP:
                stats[hour].pickup += durationWithinHour;
                return;

            case DRIVE_OCCUPIED:
                stats[hour].occupied += durationWithinHour;
                return;

            case DROPOFF:
                stats[hour].dropoff += durationWithinHour;
                return;

            case STAY:
                stats[hour].stay += durationWithinHour;
                return;

            default:
                throw new RuntimeException();
        }
    }


    private void updateHourlyStats(HourlyVehicleStats[] vehStats)
    {
        double dailyEmpty = 0;
        double dailyOccupied = 0;
        double dailyStay = 0;
        double dailyTotal = 0;

        for (int h = 0; h < hours; h++) {
            HourlyVehicleStats vhs = vehStats[h];
            if (vhs == null) {
                continue;
            }

            double emptyRatio = vhs.empty / (vhs.empty + vhs.occupied);
            double stayRatio = vhs.stay / vhs.total();

            dailyEmpty += vhs.empty;
            dailyOccupied += vhs.occupied;
            dailyStay += vhs.stay;
            dailyTotal += vhs.total();

            HourlyTaxiStats hs = hourlyStats[h];
            if (!Double.isNaN(emptyRatio)) {
                hs.emptyDriveRatio.addValue(emptyRatio);
            }
            hs.stayRatio.addValue(stayRatio);

            hs.allCount++;

            if (stayRatio < 1.0) {
                hs.stayLt100PctCount++;

                if (stayRatio < 0.75) {
                    hs.stayLt75PctCount++;

                    if (stayRatio < 0.5) {
                        hs.stayLt50PctCount++;

                        if (stayRatio < 0.25) {
                            hs.stayLt25PctCount++;

                            if (stayRatio < 0.01) {
                                hs.stayLt1PctCount++;
                            }
                        }
                    }
                }
            }

            HourlyHistograms hh = hourlyHistograms[h];
            if (!Double.isNaN(emptyRatio)) {
                hh.emptyDriveRatio.addValue(emptyRatio);
            }

            hh.stayRatio.addValue(stayRatio);
        }

        double dailyEmptyRatio = dailyEmpty / (dailyEmpty + dailyOccupied);
        double dailyStayRatio = dailyStay / dailyTotal;

        dailyHistograms.emptyDriveRatio.addValue(dailyEmptyRatio);
        dailyHistograms.stayRatio.addValue(dailyStayRatio);
    }
}
