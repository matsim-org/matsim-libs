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

package playground.michalm.taxi.scheduler;

import java.io.PrintWriter;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Task.TaskType;

import playground.michalm.taxi.schedule.TaxiTask;


public class TaxiDelaySpeedupStats
{
    private final SummaryStatistics driveDelayStats = new SummaryStatistics();
    private final SummaryStatistics driveSpeedupStats = new SummaryStatistics();
    private final SummaryStatistics driveWithPassengerDelayStats = new SummaryStatistics();
    private final SummaryStatistics driveWithPassengerSpeedupStats = new SummaryStatistics();
    private final SummaryStatistics stayDelayStats = new SummaryStatistics();
    private final SummaryStatistics staySpeedupStats = new SummaryStatistics();
    private final SummaryStatistics pickupDelayStats = new SummaryStatistics();
    private final SummaryStatistics pickupSpeedupStats = new SummaryStatistics();
    private final SummaryStatistics dropoffDelayStats = new SummaryStatistics();
    private final SummaryStatistics dropoffSpeedupStats = new SummaryStatistics();


    public void updateStats(TaxiTask currentTask, double endTime)
    {
        double plannedEndTime;

        if (currentTask.getType() == TaskType.DRIVE) {
            plannedEndTime = ((DriveTask)currentTask).getTaskTracker().getPlannedEndTime();
        }
        else {
            plannedEndTime = currentTask.getEndTime();
        }

        double delay = endTime - plannedEndTime;

        if (delay == 0) {
            return;
        }

        switch (currentTask.getTaxiTaskType()) {
            case DRIVE:
                updateStats(delay, driveDelayStats, driveSpeedupStats);
                break;

            case DRIVE_WITH_PASSENGER:
                updateStats(delay, driveWithPassengerDelayStats, driveWithPassengerSpeedupStats);
                break;

            case STAY:
                updateStats(delay, stayDelayStats, staySpeedupStats);
                break;

            case PICKUP:
                updateStats(delay, pickupDelayStats, pickupSpeedupStats);
                break;

            case DROPOFF:
                updateStats(delay, dropoffDelayStats, dropoffSpeedupStats);
        }
    }


    private void updateStats(double delay, SummaryStatistics delayStats,
            SummaryStatistics speedupStats)
    {
        if (delay > 0) {
            delayStats.addValue(delay);
        }
        else {
            speedupStats.addValue(-delay);
        }
    }


    private void printSingleStats(PrintWriter pw, SummaryStatistics stats, String name)
    {
        pw.printf("%20s\t%d\t%f\t%f\t%f", name, stats.getN(), stats.getMean(), stats.getMax(),
                stats.getStandardDeviation());
        pw.println();
    }


    public void printStats(PrintWriter pw, String id)
    {
        pw.println(id + " ==============================");

        printSingleStats(pw, driveDelayStats, "drive delay");
        printSingleStats(pw, driveSpeedupStats, "drive speedup");
        printSingleStats(pw, driveWithPassengerDelayStats, "drive with passenger delay");
        printSingleStats(pw, driveWithPassengerSpeedupStats, "drive with passenger speedup");
        printSingleStats(pw, stayDelayStats, "stay delay");
        printSingleStats(pw, staySpeedupStats, "stay speedup");
        printSingleStats(pw, pickupDelayStats, "pickup delay");
        printSingleStats(pw, pickupSpeedupStats, "pickup speedup");
        printSingleStats(pw, dropoffDelayStats, "dropoff delay");
        printSingleStats(pw, dropoffSpeedupStats, "dropoff speedup");

        pw.println();
    }


    public void clearStats()
    {
        driveDelayStats.clear();
        driveSpeedupStats.clear();
        driveWithPassengerDelayStats.clear();
        driveWithPassengerSpeedupStats.clear();
        stayDelayStats.clear();
        staySpeedupStats.clear();
        pickupDelayStats.clear();
        pickupSpeedupStats.clear();
        dropoffDelayStats.clear();
        dropoffSpeedupStats.clear();
    }
}
