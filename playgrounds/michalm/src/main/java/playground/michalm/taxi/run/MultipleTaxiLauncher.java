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

import static playground.michalm.taxi.run.AlgorithmConfig.*;

import java.io.*;
import java.util.EnumSet;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jfree.chart.JFreeChart;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;
import org.matsim.core.gbl.MatsimRandom;

import pl.poznan.put.util.jfreechart.ChartUtils;
import playground.michalm.taxi.chart.TaxiScheduleChartUtils;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.TaxiStatsCalculator.TaxiStats;
import playground.michalm.taxi.scheduler.TaxiDelaySpeedupStats;


/*package*/class MultipleTaxiLauncher
{
    private static final int[] RANDOM_SEEDS = { 463, 467, 479, 487, 491, 499, 503, 509, 521, 523,
            541, 547, 557, 563, 569, 571, 577, 587, 593, 599, 601, 607, 613, 617, 619, 631, 641,
            643, 647, 653 };

    private final TaxiLauncher launcher;
    private final TaxiDelaySpeedupStats delaySpeedupStats;
    private final LeastCostPathCalculatorCacheStats cacheStats;

    private PrintWriter pw;
    private PrintWriter pw2;
    private PrintWriter pw3;


    /*package*/MultipleTaxiLauncher(String paramFile)
    {
        launcher = new TaxiLauncher(paramFile);

        delaySpeedupStats = new TaxiDelaySpeedupStats();
        launcher.delaySpeedupStats = delaySpeedupStats;

        cacheStats = new LeastCostPathCalculatorCacheStats();
        launcher.cacheStats = cacheStats;
    }


    /*package*/void initOutputFiles(String outputFileSuffix)
    {
        try {
            pw = new PrintWriter(launcher.dirName + "stats" + outputFileSuffix);
            pw.print("cfg\tn\tm\tPW\tPWp95\tPWmax\tPD\tPDp95\tPDmax\tDD\tPS\tDS\tW\tComp\n");

            pw2 = new PrintWriter(launcher.dirName + "timeUpdates" + outputFileSuffix);

            pw3 = new PrintWriter(launcher.dirName + "cacheStats" + outputFileSuffix);
            pw3.print("cfg\tHits\tMisses\n");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /*package*/void closeOutputFiles()
    {
        pw.close();
        pw2.close();
        pw3.close();
    }


    /*package*/void run(AlgorithmConfig config, int runs, Boolean destinationKnown,
            Boolean onlineVehicleTracker, Boolean minimizePickupTripTime)
    {
        if (runs < 0 || runs > RANDOM_SEEDS.length) {
            throw new RuntimeException();
        }

        launcher.algorithmConfig = config;
        launcher.destinationKnown = destinationKnown;
        launcher.onlineVehicleTracker = onlineVehicleTracker;
        launcher.minimizePickupTripTime = minimizePickupTripTime;

        SummaryStatistics taxiPickupDriveTime = new SummaryStatistics();
        SummaryStatistics percentile95TaxiPickupDriveTime = new SummaryStatistics();
        SummaryStatistics maxTaxiPickupDriveTime = new SummaryStatistics();
        SummaryStatistics taxiDropoffDriveTime = new SummaryStatistics();
        SummaryStatistics taxiPickupTime = new SummaryStatistics();
        SummaryStatistics taxiDropoffTime = new SummaryStatistics();
        SummaryStatistics taxiCruiseTime = new SummaryStatistics();
        SummaryStatistics taxiWaitTime = new SummaryStatistics();
        SummaryStatistics taxiOverTime = new SummaryStatistics();
        SummaryStatistics passengerWaitTime = new SummaryStatistics();
        SummaryStatistics maxPassengerWaitTime = new SummaryStatistics();
        SummaryStatistics percentile95PassengerWaitTime = new SummaryStatistics();
        SummaryStatistics computationTime = new SummaryStatistics();

        launcher.clearVrpPathCalculator();

        //warmup
        if (launcher.algorithmConfig.ttimeSource != TravelTimeSource.FREE_FLOW_SPEED) {
            for (int i = 0; i < runs; i += 4) {
                MatsimRandom.reset(RANDOM_SEEDS[i]);
                launcher.initVrpPathCalculator();
                launcher.go(true);
            }
        }

        ///========================

        launcher.initVrpPathCalculator();//the same for all runs

        for (int i = 0; i < runs; i++) {
            long t0 = System.currentTimeMillis();
            MatsimRandom.reset(RANDOM_SEEDS[i]);
            launcher.go(false);
            TaxiStats evaluation = (TaxiStats)new TaxiStatsCalculator()
                    .calculateStats(launcher.context.getVrpData());
            long t1 = System.currentTimeMillis();

            taxiPickupDriveTime.addValue(evaluation.getTaxiPickupDriveTime());
            percentile95TaxiPickupDriveTime.addValue(evaluation.getTaxiPickupDriveTimeStats()
                    .getPercentile(95));
            maxTaxiPickupDriveTime.addValue(evaluation.getMaxTaxiPickupDriveTime());
            taxiDropoffDriveTime.addValue(evaluation.getTaxiDropoffDriveTime());
            taxiPickupTime.addValue(evaluation.getTaxiPickupTime());
            taxiDropoffTime.addValue(evaluation.getTaxiDropoffTime());
            taxiCruiseTime.addValue(evaluation.getTaxiCruiseTime());
            taxiWaitTime.addValue(evaluation.getTaxiWaitTime());
            taxiOverTime.addValue(evaluation.getTaxiOverTime());
            passengerWaitTime.addValue(evaluation.getPassengerWaitTime());
            percentile95PassengerWaitTime.addValue(evaluation.getPassengerWaitTimeStats()
                    .getPercentile(95));
            maxPassengerWaitTime.addValue(evaluation.getMaxPassengerWaitTime());
            computationTime.addValue(0.001 * (t1 - t0));
        }

        VrpData data = launcher.context.getVrpData();
        String cfg = launcher.algorithmConfig.name();
        if (minimizePickupTripTime != null) {
            cfg += minimizePickupTripTime ? "_TP" : "_TW";
        }

        pw.printf(
                "%20s\t%d\t%d\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n",//
                cfg,//
                data.getRequests().size(),//
                data.getVehicles().size(),//
                passengerWaitTime.getMean(),//
                percentile95PassengerWaitTime.getMean(), //
                maxPassengerWaitTime.getMean(),//
                taxiPickupDriveTime.getMean(),//
                percentile95TaxiPickupDriveTime.getMean(), //
                maxTaxiPickupDriveTime.getMean(),//
                taxiDropoffDriveTime.getMean(),//
                taxiPickupTime.getMean(),//
                taxiDropoffTime.getMean(),//
                taxiWaitTime.getMean(),//
                computationTime.getMean());

        delaySpeedupStats.printStats(pw2, launcher.algorithmConfig.name());
        delaySpeedupStats.clearStats();

        cacheStats.printStats(pw3, launcher.algorithmConfig.name());
        cacheStats.clearStats();

        JFreeChart chart = TaxiScheduleChartUtils.chartSchedule(launcher.context.getVrpData()
                .getVehicles());
        chart.setTitle(cfg);
        ChartUtils.showFrame(chart);
    }


    /*package*/void run(AlgorithmConfig config, int runs, Boolean minimizePickupTripTime)
    {
        Boolean destinationKnown = false;
        Boolean onlineVehicleTracker = false;

        run(config, runs, destinationKnown, onlineVehicleTracker, minimizePickupTripTime);

    }


    /*package*/void run(EnumSet<AlgorithmConfig> configs, int runs, Boolean minimizePickupTripTime)
    {
        for (AlgorithmConfig cfg : configs) {
            run(cfg, runs, minimizePickupTripTime);
        }
    }


    /*package*/static final EnumSet<AlgorithmConfig> selectedNos = EnumSet.of(//
            //NOS_SL,
            //NOS_TD,
            NOS_FF
            //NOS_24H,
            //NOS_15M
            );

    /*package*/static final EnumSet<AlgorithmConfig> selectedNosDse = EnumSet.of(//
            //NOS_DSE_SL,
            //NOS_DSE_TD,
            NOS_DSE_FF
            //NOS_DSE_24H,
            //NOS_DSE_15M
            );

    /*package*/static final EnumSet<AlgorithmConfig> selectedNonNos = EnumSet.of(//
            //OTS_FF,
            //OTS_24H,
            //OTS_15M,
            //========================================
            //RES_FF,
            //RES_24H,
            //RES_15M,
            //========================================
            APS_FF
            //APS_24H,
            //APS_15M
            );

    /*package*/static final EnumSet<AlgorithmConfig> selectedNonNosDse = EnumSet.of(//
            APS_DSE_FF
            //APS_DSE_24H,
            //APS_DSE_15M
            );


    /*package*/static void runAll(int runs, String paramFile)
    {
        MultipleTaxiLauncher multiLauncher = new MultipleTaxiLauncher(paramFile);
        multiLauncher.initOutputFiles("");

        //multiLauncher.run(NOS_SL, runs, false);

        //                multiLauncher.run(selectedNos, runs, false);
        //                multiLauncher.run(selectedNos, runs, true);
        //                multiLauncher.run(selectedNosDse, runs, null);

        multiLauncher.run(selectedNonNos, runs, false);
        multiLauncher.run(selectedNonNos, runs, true);
        multiLauncher.run(selectedNonNosDse, runs, null);

        multiLauncher.closeOutputFiles();
    }


    // args: configIdx runs
    public static void main(String... args)
    {
        int runs = Integer.valueOf(args[0]);
        String paramFile = args[1];
        runAll(runs, paramFile);
    }
}
