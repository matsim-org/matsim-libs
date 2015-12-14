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
import java.util.EnumSet;

import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.michalm.taxi.util.stats.*;


class MultiRunTaxiLauncher
    extends TaxiLauncher
{
    private static final int[] RANDOM_SEEDS = { 463, 467, 479, 487, 491, 499, 503, 509, 521, 523,
            541, 547, 557, 563, 569, 571, 577, 587, 593, 599, 601, 607, 613, 617, 619, 631, 641,
            643, 647, 653 };

    private boolean warmup;
    private PrintWriter pw;


    MultiRunTaxiLauncher(TaxiLauncherParams params)
    {
        super(params);
    }


    void initOutputFiles(String outputFileSuffix)
    {
        try {
            pw = new PrintWriter(params.outputDir + "stats" + outputFileSuffix);
            pw.println(MultiRunStats.HEADER);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void beforeQSim(QSim qSim)
    {
        if (warmup) {
            travelTimeCalculator = TravelTimeCalculator.create(scenario.getNetwork(),
                    scenario.getConfig().travelTimeCalculator());
            qSim.getEventsManager().addHandler(travelTimeCalculator);
        }
    }


    void closeOutputFiles()
    {
        pw.close();
    }


    void runWarmupConditionally(int runs)
    {
        warmup = true;

        for (int i = 0; i < runs; i += 4) {
            MatsimRandom.reset(RANDOM_SEEDS[i]);
            initTravelTimeAndDisutility();
            simulateIteration("warmup" + i);
        }

        warmup = false;
    }


    private static final int STATS_HOURS = 25;


    void run(int runs)
    {
        if (runs < 0 || runs > RANDOM_SEEDS.length) {
            throw new RuntimeException();
        }

        travelTimeCalculator = null;

        if (params.algorithmConfig.ttimeSource == TravelTimeSource.EVENTS) {
            runWarmupConditionally(runs);
        }

        MultiRunStats multiRunStats = new MultiRunStats();
        initTravelTimeAndDisutility();//the same for all runs

        for (int i = 0; i < runs; i++) {
            long t0 = System.currentTimeMillis();
            MatsimRandom.reset(RANDOM_SEEDS[i]);
            simulateIteration(i + "");

            TaxiStats stats = new TaxiStatsCalculator(context.getVrpData().getVehicles().values())
                    .getStats();
            long t1 = System.currentTimeMillis();
            multiRunStats.updateStats(stats, t1 - t0);
            
            produceDetailedStats(i);
        }

        VrpData data = context.getVrpData();
        String cfg = params.algorithmConfig.name();

        multiRunStats.printStats(pw, cfg, data);
        pw.flush();
    }


    private void produceDetailedStats(int run)
    {
        HourlyTaxiStatsCalculator calculator = new HourlyTaxiStatsCalculator(
                context.getVrpData().getVehicles().values(), STATS_HOURS);

        HourlyTaxiStats[] hourlyStats = calculator.getStats();
        try (PrintWriter hourlyStatsWriter = new PrintWriter(
                params.outputDir + "hourly_stats_run_" + run)) {
            hourlyStatsWriter.println(HourlyTaxiStats.MAIN_HEADER);
            hourlyStatsWriter.println(HourlyTaxiStats.SUB_HEADER);

            for (int h = 0; h < STATS_HOURS; h++) {
                hourlyStats[h].printStats(hourlyStatsWriter);
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        HourlyHistograms[] hourlyHistograms = calculator.getHourlyHistograms();
        try (PrintWriter hourlyHistogramsWriter = new PrintWriter(
                params.outputDir + "hourly_histograms_run_" + run)) {
            hourlyHistogramsWriter.println(HourlyHistograms.MAIN_HEADER);
            hourlyHistograms[0].printSubHeaders(hourlyHistogramsWriter);

            for (int h = 0; h < STATS_HOURS; h++) {
                hourlyHistograms[h].printStats(hourlyHistogramsWriter);
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        DailyHistograms dailyHistograms = calculator.getDailyHistograms();
        try (PrintWriter dailyHistogramsWriter = new PrintWriter(
                params.outputDir + "daily_histograms_run_" + run)) {
            dailyHistogramsWriter.println(DailyHistograms.MAIN_HEADER);
            dailyHistograms.printSubHeaders(dailyHistogramsWriter);
            dailyHistograms.printStats(dailyHistogramsWriter);
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    void runConfig(AlgorithmConfig config, int runs)
    {
        params.algorithmConfig = config;
        params.validate();
        run(runs);
    }


    static void run(int runs, TaxiLauncherParams params)
    {
        MultiRunTaxiLauncher multiLauncher = new MultiRunTaxiLauncher(params);
        multiLauncher.initOutputFiles("");
        multiLauncher.run(runs);
        multiLauncher.closeOutputFiles();
    }


    /**
     * One may call this method with the following params overridden: params.destinationKnown =
     * false; params.onlineVehicleTracker = false; params.advanceRequestSubmission = false;
     */
    @SafeVarargs
    static void runAll(int runs, TaxiLauncherParams params, EnumSet<AlgorithmConfig>... configSets)
    {
        MultiRunTaxiLauncher multiLauncher = new MultiRunTaxiLauncher(params);
        multiLauncher.initOutputFiles("");

        for (EnumSet<AlgorithmConfig> cfgSet : configSets) {
            for (AlgorithmConfig cfg : cfgSet) {
                multiLauncher.runConfig(cfg, runs);
            }
        }

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
            String dir = new File(paramFile).getParent() + '/';
            params = TaxiLauncherParams.readParams(paramFile, dir, dir);
        }
        else {
            throw new IllegalArgumentException();
        }

        run(runs, params);
    }
}
