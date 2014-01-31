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

package playground.michalm.taxi.optimizer;

import java.io.PrintWriter;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Task.TaskType;

import playground.michalm.taxi.schedule.TaxiTask;


public class TaxiDelaySpeedupStats
{
    private final SummaryStatistics pickupDriveDelayStats = new SummaryStatistics();
    private final SummaryStatistics pickupDriveSpeedupStats = new SummaryStatistics();
    private final SummaryStatistics dropoffDriveDelayStats = new SummaryStatistics();
    private final SummaryStatistics dropoffDriveSpeedupStats = new SummaryStatistics();
    private final SummaryStatistics cruiseDelayStats = new SummaryStatistics();
    private final SummaryStatistics cruiseSpeedupStats = new SummaryStatistics();
    private final SummaryStatistics waitDelayStats = new SummaryStatistics();
    private final SummaryStatistics waitSpeedupStats = new SummaryStatistics();
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
            case PICKUP_DRIVE:
                updateStats(delay, pickupDriveDelayStats, pickupDriveSpeedupStats);
                break;

            case DROPOFF_DRIVE:
                updateStats(delay, dropoffDriveDelayStats, dropoffDriveSpeedupStats);
                break;

            case CRUISE_DRIVE:
                updateStats(delay, cruiseDelayStats, cruiseSpeedupStats);
                break;

            case WAIT_STAY:
                updateStats(delay, waitDelayStats, waitSpeedupStats);
                break;

            case PICKUP_STAY:
                updateStats(delay, pickupDelayStats, pickupSpeedupStats);
                break;

            case DROPOFF_STAY:
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

        printSingleStats(pw, pickupDriveDelayStats, "pickup drive delay");
        printSingleStats(pw, pickupDriveSpeedupStats, "pickup drive speedup");
        printSingleStats(pw, dropoffDriveDelayStats, "delivery drive delay");
        printSingleStats(pw, dropoffDriveSpeedupStats, "delivery drive speedup");
        printSingleStats(pw, cruiseDelayStats, "cruise delay");
        printSingleStats(pw, cruiseSpeedupStats, "cruise speedup");
        printSingleStats(pw, waitDelayStats, "wait delay");
        printSingleStats(pw, waitSpeedupStats, "wait speedup");
        printSingleStats(pw, pickupDelayStats, "pickup delay");
        printSingleStats(pw, pickupSpeedupStats, "pickup speedup");
        printSingleStats(pw, dropoffDelayStats, "dropoff delay");
        printSingleStats(pw, dropoffSpeedupStats, "dropoff speedup");

        pw.println();
    }


    public void clearStats()
    {
        pickupDriveDelayStats.clear();
        pickupDriveSpeedupStats.clear();
        dropoffDriveDelayStats.clear();
        dropoffDriveSpeedupStats.clear();
        cruiseDelayStats.clear();
        cruiseSpeedupStats.clear();
        waitDelayStats.clear();
        waitSpeedupStats.clear();
        pickupDelayStats.clear();
        pickupSpeedupStats.clear();
        dropoffDelayStats.clear();
        dropoffSpeedupStats.clear();
    }
}
