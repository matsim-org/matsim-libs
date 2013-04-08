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

package playground.michalm.vrp.run.online;

import java.io.*;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.core.gbl.MatsimRandom;

import pl.poznan.put.vrp.dynamic.optimizer.taxi.*;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiEvaluator.TaxiEvaluation;


public class MultipleSingleIterOnlineDvrpLauncher
{
    private static final int[] RANDOM_SEEDS = { 463, 467, 479, 487, 491, 499, 503, 509, 521, 523,
            541, 547, 557, 563, 569, 571, 577, 587, 593, 599, 601, 607, 613, 617, 619, 631, 641,
            643, 647, 653 };

    private SingleIterOnlineDvrpLauncher launcher;


    public MultipleSingleIterOnlineDvrpLauncher(String paramFile)
        throws IOException
    {
        launcher = new SingleIterOnlineDvrpLauncher();
        launcher.readArgs(paramFile);
        launcher.prepareMatsimData();
    }


    public void run(int configIdx, int runs, PrintWriter pw, PrintWriter pw2)
        throws IOException
    {
        launcher.algorithmConfig = AlgorithmConfig.ALL[configIdx];

        // taxiPickupDriveTime
        // taxiDeliveryDriveTime
        // taxiServiceTime
        // taxiWaitTime
        // taxiOverTime
        // passengerWaitTime

        SummaryStatistics taxiPickupDriveTime = new SummaryStatistics();
        SummaryStatistics taxiDeliveryDriveTime = new SummaryStatistics();
        SummaryStatistics taxiServiceTime = new SummaryStatistics();
        SummaryStatistics taxiWaitTime = new SummaryStatistics();
        SummaryStatistics taxiOverTime = new SummaryStatistics();
        SummaryStatistics passengerWaitTime = new SummaryStatistics();
        SummaryStatistics maxPassengerWaitTime = new SummaryStatistics();

        switch (configIdx) {
            case 0:
            case 1:
            case 3:
            case 4:
            case 9:
            case 10:
            case 15:
            case 16:
                // run as many times as requested
                break;

            default:
                // do not run
                runs = 0;
        }

        for (int i = 0; i < runs; i++) {
            MatsimRandom.reset(RANDOM_SEEDS[i]);
            launcher.go();
            TaxiEvaluation evaluation = (TaxiEvaluation)new TaxiEvaluator()
                    .evaluateVrp(launcher.data.getVrpData());

            taxiPickupDriveTime.addValue(evaluation.getTaxiPickupDriveTime());
            taxiDeliveryDriveTime.addValue(evaluation.getTaxiDeliveryDriveTime());
            taxiServiceTime.addValue(evaluation.getTaxiServiceTime());
            taxiWaitTime.addValue(evaluation.getTaxiWaitTime());
            taxiOverTime.addValue(evaluation.getTaxiOverTime());
            passengerWaitTime.addValue(evaluation.getPassengerWaitTime());
            maxPassengerWaitTime.addValue(evaluation.getMaxPassengerWaitTime());
        }

        pw.println(configIdx + "\t" + TaxiEvaluation.HEADER);

        pw.printf("Mean\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n",//
                taxiPickupDriveTime.getMean(),//
                taxiDeliveryDriveTime.getMean(),//
                taxiServiceTime.getMean(),//
                taxiWaitTime.getMean(),//
                taxiOverTime.getMean(),//
                passengerWaitTime.getMean(),//
                maxPassengerWaitTime.getMean());
        pw.printf("Min\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n",//
                (int)taxiPickupDriveTime.getMin(),//
                (int)taxiDeliveryDriveTime.getMin(),//
                (int)taxiServiceTime.getMin(),//
                (int)taxiWaitTime.getMin(),//
                (int)taxiOverTime.getMin(),//
                (int)passengerWaitTime.getMin(),//
                (int)passengerWaitTime.getMin());
        pw.printf("Max\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n",//
                (int)taxiPickupDriveTime.getMax(),//
                (int)taxiDeliveryDriveTime.getMax(),//
                (int)taxiServiceTime.getMax(),//
                (int)taxiWaitTime.getMax(),//
                (int)taxiOverTime.getMax(),//
                (int)passengerWaitTime.getMax(),//
                (int)passengerWaitTime.getMax());
        pw.printf("StdDev\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n",
                taxiPickupDriveTime.getStandardDeviation(),//
                taxiDeliveryDriveTime.getStandardDeviation(),//
                taxiServiceTime.getStandardDeviation(),//
                taxiWaitTime.getStandardDeviation(),//
                taxiOverTime.getStandardDeviation(),//
                passengerWaitTime.getStandardDeviation(),//
                passengerWaitTime.getStandardDeviation());

        // the endTime of the simulation??? --- time of last served request

        pw.println();

        if (runs > 0) {
            pw2.println(configIdx + " ==============================");

            pw2.println("pickupDelayStats");
            pw2.println(TaxiOptimizer.pickupDelayStats);
            pw2.println("pickupSpeedUpStats");
            pw2.println(TaxiOptimizer.pickupSpeedupStats);
            pw2.println("deliveryDelayStats");
            pw2.println(TaxiOptimizer.deliveryDelayStats);
            pw2.println("deliverySpeedUpStats");
            pw2.println(TaxiOptimizer.deliverySpeedupStats);
            pw2.println("waitDelayStats");
            pw2.println(TaxiOptimizer.waitDelayStats);
            pw2.println("waitSpeedUpStats");
            pw2.println(TaxiOptimizer.waitSpeedupStats);
            pw2.println("serveDelayStats");
            pw2.println(TaxiOptimizer.serveDelayStats);
            pw2.println("serveSpeedUpStats");
            pw2.println(TaxiOptimizer.serveSpeedupStats);

            pw2.println();

            TaxiOptimizer.pickupDelayStats.clear();
            TaxiOptimizer.pickupSpeedupStats.clear();
            TaxiOptimizer.deliveryDelayStats.clear();
            TaxiOptimizer.deliverySpeedupStats.clear();
            TaxiOptimizer.waitDelayStats.clear();
            TaxiOptimizer.waitSpeedupStats.clear();
            TaxiOptimizer.serveDelayStats.clear();
            TaxiOptimizer.serveSpeedupStats.clear();
        }
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

        MultipleSingleIterOnlineDvrpLauncher multiLauncher = new MultipleSingleIterOnlineDvrpLauncher(
                paramFile);

        PrintWriter pw = new PrintWriter(multiLauncher.launcher.dirName + "stats.out");
        PrintWriter pw2 = new PrintWriter(multiLauncher.launcher.dirName + "timeUpdates.out");

        if (configIdx == -1) {
            for (int i = 0; i < AlgorithmConfig.ALL.length; i++) {
                multiLauncher.run(i, runs, pw, pw2);
            }
        }
        else {
            multiLauncher.run(configIdx, runs, pw, pw2);
        }

        pw.close();
        pw2.close();
    }
}
