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

import java.io.PrintWriter;

import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.util.stats.HourlyTaxiStats.StayRatioLevel;
import org.matsim.core.utils.io.IOUtils;


public class HourlyTaxiStatsWriter
{
    private final HourlyTaxiStats[] hourlyStats;


    public HourlyTaxiStatsWriter(HourlyTaxiStats[] hourlyStats)
    {
        this.hourlyStats = hourlyStats;
    }


    public void write(String file)
    {
        try (PrintWriter pw = new PrintWriter(IOUtils.getBufferedWriter(file))) {
            writePassengerWaitTimeStats(pw);
            writeEmptyDriveRatioStats(pw);
            writeVehicleWaitRatioStats(pw);
            writeTaskTypeSums(pw);
            writeHourlyWaitRatioCounters(pw);
        }
    }


    private void writePassengerWaitTimeStats(PrintWriter pw)
    {
        pw.println("Passenger Wait Time [s]");
        pw.println("hour\tmean\tmin\tpc_2\tpc_5\tpc_25\tpc_50\tpc_75\tpc_95\tpc_98\tmax");

        for (HourlyTaxiStats s : hourlyStats) {
            pw.printf("%d\t%.1f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f", //
                    s.hour, //
                    s.passengerWaitTime.getMean(), //
                    s.passengerWaitTime.getMin(), //
                    s.passengerWaitTime.getPercentile(2), //
                    s.passengerWaitTime.getPercentile(5), //
                    s.passengerWaitTime.getPercentile(25), //
                    s.passengerWaitTime.getPercentile(50), //
                    s.passengerWaitTime.getPercentile(75), //
                    s.passengerWaitTime.getPercentile(95), //
                    s.passengerWaitTime.getPercentile(98), //
                    s.passengerWaitTime.getMax());
            pw.println();
        }
        pw.println();
    }


    private void writeEmptyDriveRatioStats(PrintWriter pw)
    {
        pw.println("Empty Drive Ratio [%]");
        pw.println("hour\tmean\tmin\tpc_2\tpc_5\tpc_25\tpc_50\tpc_75\tpc_95\tpc_98\tmax");

        for (HourlyTaxiStats s : hourlyStats) {
            pw.printf("%d\t%.4f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f", //
                    s.hour, //
                    s.emptyDriveRatio.getMean(), //
                    s.emptyDriveRatio.getMin(), //
                    s.emptyDriveRatio.getPercentile(2), //
                    s.emptyDriveRatio.getPercentile(5), //
                    s.emptyDriveRatio.getPercentile(25), //
                    s.emptyDriveRatio.getPercentile(50), //
                    s.emptyDriveRatio.getPercentile(75), //
                    s.emptyDriveRatio.getPercentile(95), //
                    s.emptyDriveRatio.getPercentile(98), //
                    s.emptyDriveRatio.getMax());
            pw.println();
        }
        pw.println();
    }


    private void writeVehicleWaitRatioStats(PrintWriter pw)
    {
        pw.println("Vehicle Wait Ratio [%]");
        pw.println("hour\tmean\tmin\tpc_2\tpc_5\tpc_25\tpc_50\tpc_75\tpc_95\tpc_98\tmax");

        for (HourlyTaxiStats s : hourlyStats) {
            pw.printf("%d\t%.4f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f", //
                    s.hour, //
                    s.stayRatio.getMean(), //
                    s.stayRatio.getMin(), //
                    s.stayRatio.getPercentile(2), //
                    s.stayRatio.getPercentile(5), //
                    s.stayRatio.getPercentile(25), //
                    s.stayRatio.getPercentile(50), //
                    s.stayRatio.getPercentile(75), //
                    s.stayRatio.getPercentile(95), //
                    s.stayRatio.getPercentile(98), //
                    s.stayRatio.getMax());
            pw.println();
        }
        pw.println();
    }


    private void writeTaskTypeSums(PrintWriter pw)
    { 
        pw.println("Total duration of tasks by type [h]");
        pw.println(TimeProfiles.TAXI_TASK_TYPES_HEADER);

        for (HourlyTaxiStats s : hourlyStats) {
            pw.printf("%d", s.hour);

            for (TaxiTask.TaxiTaskType t : TaxiTask.TaxiTaskType.values()) {
                pw.printf("\t%.2f", (double)s.taskTypeSums.getSum(t) / 3600.);
            }
            pw.println();
        }
        pw.println();
    }


    private void writeHourlyWaitRatioCounters(PrintWriter pw)
    {
        pw.println("Hourly Wait Ratio Counts");
        pw.println("hour\t0+%\t1+%\t25+%\t50+%\t75+%\t100%");

        for (HourlyTaxiStats s : hourlyStats) {
            pw.printf("%d", s.hour);

            for (StayRatioLevel l : StayRatioLevel.values()) {
                pw.printf("\t%d", s.stayRatioLevelCounter.getCount(l));
            }
            pw.println();
        }
        pw.println();
    }
}