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

import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;
import org.matsim.core.gbl.MatsimRandom;

import playground.michalm.taxi.scheduler.TaxiDelaySpeedupStats;
import playground.michalm.taxi.util.stats.*;
import playground.michalm.taxi.util.stats.TaxiStatsCalculator.TaxiStats;


class MultiRunTaxiLauncher
{
    private static final int[] RANDOM_SEEDS = { 463, 467, 479, 487, 491, 499, 503, 509, 521, 523,
            541, 547, 557, 563, 569, 571, 577, 587, 593, 599, 601, 607, 613, 617, 619, 631, 641,
            643, 647, 653 };

    private final TaxiLauncherParams params;
    private final TaxiLauncher launcher;

    private final TaxiDelaySpeedupStats delaySpeedupStats;
    private final LeastCostPathCalculatorCacheStats cacheStats;

    private PrintWriter pw;
    private PrintWriter pw2;
    private PrintWriter pw3;


    MultiRunTaxiLauncher(TaxiLauncherParams params)
    {
        this.params = params;
        launcher = new TaxiLauncher(params);

        delaySpeedupStats = new TaxiDelaySpeedupStats();
        launcher.delaySpeedupStats = delaySpeedupStats;

        cacheStats = new LeastCostPathCalculatorCacheStats();
        launcher.cacheStats = cacheStats;
    }


    void initOutputFiles(String outputFileSuffix)
    {
        try {
            pw = new PrintWriter(params.outputDir + "stats" + outputFileSuffix);
            pw.print("cfg\tn\tm\tPW\tPWp95\tPWmax\tPD\tPDp95\tPDmax\tDD\tPS\tDS\tW\tComp\n");

            pw2 = new PrintWriter(params.outputDir + "timeUpdates" + outputFileSuffix);

            pw3 = new PrintWriter(params.outputDir + "cacheStats" + outputFileSuffix);
            pw3.print("cfg\tHits\tMisses\n");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    void closeOutputFiles()
    {
        pw.close();
        pw2.close();
        pw3.close();
    }


    void run(AlgorithmConfig config, int runs, Boolean destinationKnown,
            Boolean onlineVehicleTracker, Boolean advanceRequestSubmission)
    {
        if (runs < 0 || runs > RANDOM_SEEDS.length) {
            throw new RuntimeException();
        }

        params.algorithmConfig = config;
        params.destinationKnown = destinationKnown;
        params.onlineVehicleTracker = onlineVehicleTracker;
        params.advanceRequestSubmission = advanceRequestSubmission;

        launcher.clearVrpPathCalculator();

        //warmup
        if (params.algorithmConfig.ttimeSource == TravelTimeSource.EVENTS && //
                !launcher.scenario.getConfig().network().isTimeVariantNetwork()) {
            for (int i = 0; i < runs; i += 4) {
                MatsimRandom.reset(RANDOM_SEEDS[i]);
                launcher.initVrpPathCalculator();
                launcher.go(true);
            }
        }

        ///========================

        MultiRunStats multipleRunStats = new MultiRunStats();
        launcher.initVrpPathCalculator();//the same for all runs

        for (int i = 0; i < runs; i++) {
            long t0 = System.currentTimeMillis();
            MatsimRandom.reset(RANDOM_SEEDS[i]);
            launcher.go(false);
            TaxiStats evaluation = (TaxiStats)new TaxiStatsCalculator()
                    .calculateStats(launcher.context.getVrpData().getVehicles());
            long t1 = System.currentTimeMillis();

            multipleRunStats.updateStats(evaluation, t1 - t0);
        }

        VrpData data = launcher.context.getVrpData();
        String cfg = params.algorithmConfig.name();

        multipleRunStats.printStats(pw, cfg, data);
        pw.flush();

        delaySpeedupStats.printStats(pw2, params.algorithmConfig.name());
        delaySpeedupStats.clearStats();
        pw2.flush();

        cacheStats.printStats(pw3, params.algorithmConfig.name());
        cacheStats.clearStats();
        pw3.flush();

        //=========== stats & graphs for the last run

        //        JFreeChart chart = TaxiScheduleChartUtils.chartSchedule(data.getVehicles());
        //        chart.setTitle(cfg);
        //        ChartUtils.showFrame(chart);
    }


    void run(AlgorithmConfig config, int runs)
    {
        Boolean destinationKnown = false;
        Boolean onlineVehicleTracker = false;
        Boolean advanceRequestSubmission = false;

        run(config, runs, destinationKnown, onlineVehicleTracker, advanceRequestSubmission);
    }


    void run(EnumSet<AlgorithmConfig> configs, int runs)
    {
        for (AlgorithmConfig cfg : configs) {
            run(cfg, runs);
        }
    }


    static final EnumSet<AlgorithmConfig> NOS_TW_xx = EnumSet.of(//
    //        NOS_TW_TD,//
    //        NOS_TW_FF
    NOS_TW_15M
            );

    static final EnumSet<AlgorithmConfig> NOS_TP_xx = EnumSet.of(//
    //        NOS_TP_TD, //
    //        NOS_TP_FF
    NOS_TP_15M
            );

    static final EnumSet<AlgorithmConfig> NOS_DSE_xx = EnumSet.of(//
    //        NOS_DSE_TD, //
    //        NOS_DSE_FF
    NOS_DSE_15M
            );

    static final EnumSet<AlgorithmConfig> OTS_TW_xx = EnumSet.of(//
            //OTS_TW_FF
            OTS_TW_15M);

    static final EnumSet<AlgorithmConfig> OTS_TP_xx = EnumSet.of(//
            //OTS_TP_FF
            OTS_TP_15M);

    static final EnumSet<AlgorithmConfig> RES_TW_xx = EnumSet.of(//
            //RES_TW_FF
            RES_TW_15M);

    static final EnumSet<AlgorithmConfig> RES_TP_xx = EnumSet.of(//
            //RES_TP_FF
            RES_TP_15M);

    static final EnumSet<AlgorithmConfig> APS_TW_xx = EnumSet.of(//
            //APS_TW_TD,
            //APS_TW_FF
            APS_TW_15M);

    static final EnumSet<AlgorithmConfig> APS_TP_xx = EnumSet.of(//
            //APS_TP_TD,
            //APS_TP_FF
            APS_TP_15M);

    static final EnumSet<AlgorithmConfig> APS_DSE_xx = EnumSet.of(//
            //APS_DSE_TD,
            //APS_DSE_FF
            APS_DSE_15M);


    static void runAll(int runs, TaxiLauncherParams params)
    {
        MultiRunTaxiLauncher multiLauncher = new MultiRunTaxiLauncher(params);
        multiLauncher.initOutputFiles("");

        multiLauncher.run(NOS_TW_xx, runs);
        multiLauncher.run(NOS_TP_xx, runs);
        multiLauncher.run(NOS_DSE_xx, runs);
        //
        //        multiLauncher.run(OTS_TW_xx, runs);
        //        multiLauncher.run(OTS_TP_xx, runs);
        //
        //        multiLauncher.run(RES_TW_xx, runs);
        //        multiLauncher.run(RES_TP_xx, runs);
        //
        //        multiLauncher.run(APS_TW_xx, runs);
        //        multiLauncher.run(APS_TP_xx, runs);
        //        multiLauncher.run(APS_DSE_xx, runs);

        //multiLauncher.run(NOS_DSE_FF, runs, true, true, true);
        //        multiLauncher.run(MIP_TW_FF, runs, true, true, false);

        multiLauncher.closeOutputFiles();
    }


    public static void main(String... args)
    {
        int runs = Integer.valueOf(args[0]);
        String paramFile = args[1];
        TaxiLauncherParams params;

        if (args.length == 4) {
            String inputDir = args[2];
            String outputDir = args[3];
            params = TaxiLauncherParams.readParams(paramFile, inputDir, outputDir);
        }
        else if (args.length == 2) {
            params = TaxiLauncherParams.readParams(paramFile);
        }
        else {
            throw new IllegalArgumentException();
        }

        runAll(runs, params);
    }
}
