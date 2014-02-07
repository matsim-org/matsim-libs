/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.run;

import java.io.*;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;
import org.matsim.core.gbl.MatsimRandom;

import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.TaxiStatsCalculator.TaxiStats;


/*package*/class MultipleSingleIterTaxiLauncher
{
    private static final int[] RANDOM_SEEDS = { 463, 467, 479, 487, 491, 499, 503, 509, 521, 523,
            541, 547, 557, 563, 569, 571, 577, 587, 593, 599, 601, 607, 613, 617, 619, 631, 641,
            643, 647, 653 };

    private final SingleIterTaxiLauncher launcher;
    private final TaxiDelaySpeedupStats delaySpeedupStats;


    /*package*/MultipleSingleIterTaxiLauncher(String paramFile)
        throws IOException
    {
        launcher = new SingleIterTaxiLauncher(paramFile);
        delaySpeedupStats = new TaxiDelaySpeedupStats();
        launcher.delaySpeedupStats = delaySpeedupStats;
    }


    /*package*/void run(int configIdx, int runs, boolean destinationKnown,
            boolean onlineVehicleTracker, boolean minimizePickupTripTime, PrintWriter pw,
            PrintWriter pw2)
    {
        launcher.algorithmConfig = AlgorithmConfig.ALL[configIdx];
        launcher.destinationKnown = destinationKnown;
        launcher.onlineVehicleTracker = onlineVehicleTracker;
        launcher.minimizePickupTripTime = minimizePickupTripTime;

        // taxiPickupDriveTime
        // taxiDeliveryDriveTime
        // taxiServiceTime
        // taxiWaitTime
        // taxiOverTime
        // passengerWaitTime

        SummaryStatistics taxiPickupDriveTime = new SummaryStatistics();
        SummaryStatistics taxiDeliveryDriveTime = new SummaryStatistics();
        SummaryStatistics taxiServiceTime = new SummaryStatistics();
        SummaryStatistics taxiCruiseTime = new SummaryStatistics();
        SummaryStatistics taxiWaitTime = new SummaryStatistics();
        SummaryStatistics taxiOverTime = new SummaryStatistics();
        SummaryStatistics passengerWaitTime = new SummaryStatistics();
        SummaryStatistics maxPassengerWaitTime = new SummaryStatistics();
        SummaryStatistics computationTime = new SummaryStatistics();

        boolean warmup;

        switch (configIdx) {
            case 0:
            case 1:
            case 3:
            case 4:
            case 5:
            case 6:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
                // run as many times as requested
                
                // additionally, run warmup iterations if necessary
                warmup = launcher.algorithmConfig.ttimeSource != TravelTimeSource.FREE_FLOW_SPEED;

                break;

            default:
                // do not run at all
                runs = 0;
                warmup = false;
        }

        if (warmup) {
            for (int i = 0; i < runs; i += 4) {
                MatsimRandom.reset(RANDOM_SEEDS[i]);
                launcher.go(true);
            }
        }

        for (int i = 0; i < runs; i++) {
            long t0 = System.currentTimeMillis();
            MatsimRandom.reset(RANDOM_SEEDS[i]);
            launcher.go(false);
            TaxiStats evaluation = (TaxiStats)new TaxiStatsCalculator()
                    .calculateStats(launcher.context.getVrpData());
            long t1 = System.currentTimeMillis();

            taxiPickupDriveTime.addValue(evaluation.getTaxiPickupDriveTime());
            taxiDeliveryDriveTime.addValue(evaluation.getTaxiDropoffDriveTime());
            taxiServiceTime.addValue(evaluation.getTaxiPickupTime());
            taxiCruiseTime.addValue(evaluation.getTaxiCruiseTime());
            taxiWaitTime.addValue(evaluation.getTaxiWaitTime());
            taxiOverTime.addValue(evaluation.getTaxiOverTime());
            passengerWaitTime.addValue(evaluation.getPassengerWaitTime());
            maxPassengerWaitTime.addValue(evaluation.getMaxPassengerWaitTime());
            computationTime.addValue(t1 - t0);
        }

        pw.println(launcher.algorithmConfig.name);
        pw.println(configIdx + "\t" + TaxiStats.HEADER + "\tcomputationT");

        pw.printf("Mean\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n",//
                taxiPickupDriveTime.getMean(),//
                taxiDeliveryDriveTime.getMean(),//
                taxiServiceTime.getMean(),//
                taxiCruiseTime.getMean(),//
                taxiWaitTime.getMean(),//
                taxiOverTime.getMean(),//
                passengerWaitTime.getMean(),//
                maxPassengerWaitTime.getMean(),//
                computationTime.getMean());
        pw.printf("Min\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n",//
                taxiPickupDriveTime.getMin(),//
                taxiDeliveryDriveTime.getMin(),//
                taxiServiceTime.getMin(),//
                taxiCruiseTime.getMin(),//
                taxiWaitTime.getMin(),//
                taxiOverTime.getMin(),//
                passengerWaitTime.getMin(),//
                maxPassengerWaitTime.getMin(),//
                computationTime.getMin());
        pw.printf("Max\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n",//
                taxiPickupDriveTime.getMax(),//
                taxiDeliveryDriveTime.getMax(),//
                taxiServiceTime.getMax(),//
                taxiCruiseTime.getMax(),//
                taxiWaitTime.getMax(),//
                taxiOverTime.getMax(),//
                passengerWaitTime.getMax(),//
                maxPassengerWaitTime.getMax(),//
                computationTime.getMax());
        pw.printf("StdDev\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n",
                taxiPickupDriveTime.getStandardDeviation(),//
                taxiDeliveryDriveTime.getStandardDeviation(),//
                taxiServiceTime.getStandardDeviation(),//
                taxiCruiseTime.getStandardDeviation(),//
                taxiWaitTime.getStandardDeviation(),//
                taxiOverTime.getStandardDeviation(),//
                passengerWaitTime.getStandardDeviation(),//
                maxPassengerWaitTime.getStandardDeviation(),//
                computationTime.getStandardDeviation());

        // the endTime of the simulation??? --- time of last served request

        pw.println();

        if (runs > 0) {
            delaySpeedupStats.printStats(pw2, configIdx + "");
            delaySpeedupStats.clearStats();
        }

        launcher.travelTimeCalculator = null;
    }


    private static void run(int configIdx, int runs, String paramFile, boolean destinationKnown,
            boolean onlineVehicleTracker, boolean minimizePickupTripTime)
        throws IOException
    {
        MultipleSingleIterTaxiLauncher multiLauncher = new MultipleSingleIterTaxiLauncher(paramFile);

        String txt = "DK_" + destinationKnown + "_VT_" + onlineVehicleTracker + "_TP_"
                + minimizePickupTripTime;

        PrintWriter pw = new PrintWriter(multiLauncher.launcher.dirName + "stats_" + txt + ".out");
        PrintWriter pw2 = new PrintWriter(multiLauncher.launcher.dirName + "timeUpdates_" + txt
                + ".out");

        if (configIdx == -1) {
            for (int i = 0; i < AlgorithmConfig.ALL.length; i++) {
                multiLauncher.run(i, runs, destinationKnown, onlineVehicleTracker,
                        minimizePickupTripTime, pw, pw2);
            }
        }
        else {
            multiLauncher.run(configIdx, runs, destinationKnown, onlineVehicleTracker,
                    minimizePickupTripTime, pw, pw2);
        }

        pw.close();
        pw2.close();

    }


    // args: configIdx runs
    public static void main(String... args)
        throws IOException
    {
        String paramFile;
        if (args.length == 2) {
            paramFile = null;
        }
        else if (args.length == 3) {
            paramFile = args[2];
        }
        else {
            throw new RuntimeException();
        }

        int configIdx = Integer.valueOf(args[0]);
        if (configIdx < -1 || configIdx >= AlgorithmConfig.ALL.length) {
            throw new RuntimeException();
        }

        int runs = Integer.valueOf(args[1]);
        if (runs < 1 || runs > RANDOM_SEEDS.length) {
            throw new RuntimeException();
        }

        run(configIdx, runs, paramFile, false, false, false);
        run(configIdx, runs, paramFile, false, true, false);
        run(configIdx, runs, paramFile, true, false, false);
        run(configIdx, runs, paramFile, true, true, false);

        run(configIdx, runs, paramFile, false, false, true);
        run(configIdx, runs, paramFile, false, true, true);
        run(configIdx, runs, paramFile, true, false, true);
        run(configIdx, runs, paramFile, true, true, true);
    }
}
