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
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.core.utils.io.IOUtils;


public class TaxiStatsWriter
{
    private final Map<String, TaxiStats> taxiStats;


    public TaxiStatsWriter(Map<String, TaxiStats> taxiStats)
    {
        this.taxiStats = taxiStats;
    }


    public void write(String file)
    {
        try (PrintWriter pw = new PrintWriter(IOUtils.getBufferedWriter(file))) {
            writePassengerWaitTimeStats(pw);
            writeVehicleEmptyDriveRatioStats(pw);
            writeVehicleWaitRatioStats(pw);
            writeTaskTypeSums(pw);
        }
    }


    private void writePassengerWaitTimeStats(PrintWriter pw)
    {
        pw.println("Passenger Wait Time [s]");
        pw.println("hour\tn\t" + DETAILED_STATS_SUBHEADER);

        for (TaxiStats s : taxiStats.values()) {
            String prefix = String.format("%s\t%d", s.id, s.passengerWaitTime.getN());
            printfDetailedStats(pw,
                    prefix + "\t%.1f\t%.1f\t|\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f",
                    s.passengerWaitTime);
        }
        pw.println();
    }


    private void writeVehicleEmptyDriveRatioStats(PrintWriter pw)
    {
        pw.println("Vehicle Empty Drive Ratio");
        pw.println("hour\tfleet\t" + DETAILED_STATS_SUBHEADER);

        for (TaxiStats s : taxiStats.values()) {
            String prefix = String.format("%s\t%.4f", s.id, s.getFleetEmptyDriveRatio());
            printfDetailedStats(pw,
                    prefix + "\t%.4f\t%.4f\t|\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f",
                    s.vehicleEmptyDriveRatio);
        }
        pw.println();
    }


    private void writeVehicleWaitRatioStats(PrintWriter pw)
    {
        pw.println("Vehicle Wait Ratio");
        pw.println("hour\tfleet\t" + DETAILED_STATS_SUBHEADER);

        for (TaxiStats s : taxiStats.values()) {
            String prefix = String.format("%s\t%.4f", s.id, s.getFleetStayRatio());
            printfDetailedStats(pw,
                    prefix + "\t%.4f\t%.4f\t|\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f",
                    s.vehicleStayRatio);
        }
        pw.println();
    }


    private static final String DETAILED_STATS_SUBHEADER = "mean\tsd\t|\t"
            + "min\tpc_2\tpc_5\tpc_25\tpc_50\tpc_75\tpc_95\tpc_98\tmax";


    private void printfDetailedStats(PrintWriter pw, String format, DescriptiveStatistics stats)
    {
        pw.printf(format, //
                stats.getMean(), //
                stats.getStandardDeviation(), //
                //
                stats.getMin(), //
                stats.getPercentile(2), //
                stats.getPercentile(5), //
                stats.getPercentile(25), //
                stats.getPercentile(50), //
                stats.getPercentile(75), //
                stats.getPercentile(95), //
                stats.getPercentile(98), //
                stats.getMax());
        pw.println();
    }


    private void writeTaskTypeSums(PrintWriter pw)
    {
        pw.println("Total duration of tasks by type [h]");
        pw.println(TimeProfiles.TAXI_TASK_TYPES_HEADER + "\tall");

        for (TaxiStats s : taxiStats.values()) {
            pw.print(s.id);
            for (TaxiTask.TaxiTaskType t : TaxiTask.TaxiTaskType.values()) {
                pw.printf("\t%.2f", s.taskTimeSumsByType.get(t).doubleValue() / 3600);
            }
            pw.printf("\t%.2f", s.taskTimeSumsByType.getTotal().doubleValue() / 3600);
            pw.println();
        }
        pw.println();
    }
}