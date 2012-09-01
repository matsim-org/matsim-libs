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

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.matsim.core.gbl.MatsimRandom;

import pl.poznan.put.vrp.dynamic.optimizer.taxi.*;


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


    public void run(int configIdx, int runs)
        throws IOException
    {
        launcher.algorithmConfig = AlgorithmConfig.ALL[configIdx];

        SummaryStatistics travelTime = new SummaryStatistics();
        SummaryStatistics travelTimeOccupied = new SummaryStatistics();
        SummaryStatistics travelTimeIdle = new SummaryStatistics();
        SummaryStatistics waitTime = new SummaryStatistics();
        SummaryStatistics totalTime = new SummaryStatistics();

        for (int i = 0; i < runs; i++) {
            MatsimRandom.reset(RANDOM_SEEDS[i]);
            launcher.go();
            TaxiEvaluation evaluation = (TaxiEvaluation)new TaxiEvaluator()
                    .evaluateVrp(launcher.data.getVrpData());

            travelTime.addValue(evaluation.getTravelCost());
            travelTimeOccupied.addValue(evaluation.getTravelCostWithPassenger());
            travelTimeIdle.addValue(evaluation.getTravelCostWithoutPassenger());
            waitTime.addValue(evaluation.getReqTimeViolations());
            totalTime.addValue(evaluation.getValue());
        }

        PrintWriter pw = new PrintWriter(launcher.dirName + "stats_" + configIdx + "_" + runs
                + ".out");
        pw.println("---\tTOTAL-TT\tOCC-TT\tIDLE-TT\tWAIT-T\tTOTAL-T");

        pw.printf("Mean\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n", travelTime.getMean(),
                travelTimeOccupied.getMean(), travelTimeIdle.getMean(), waitTime.getMean(),
                totalTime.getMean());
        pw.printf("Min\t%d\t%d\t%d\t%d\t%d\n", (int)travelTime.getMin(),
                (int)travelTimeOccupied.getMin(), (int)travelTimeIdle.getMin(),
                (int)waitTime.getMin(), (int)totalTime.getMin());
        pw.printf("Max\t%d\t%d\t%d\t%d\t%d\n", (int)travelTime.getMax(),
                (int)travelTimeOccupied.getMax(), (int)travelTimeIdle.getMax(),
                (int)waitTime.getMax(), (int)totalTime.getMax());
        pw.printf("StdDev\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n", travelTime.getStandardDeviation(),
                travelTimeOccupied.getStandardDeviation(), travelTimeIdle.getStandardDeviation(),
                waitTime.getStandardDeviation(), totalTime.getStandardDeviation());

        pw.close();
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

        if (configIdx == -1) {
            for (int i = 0; i < AlgorithmConfig.ALL.length; i++) {
                multiLauncher.run(i, runs);
            }
        }
        else {
            multiLauncher.run(configIdx, runs);
        }
    }
}
