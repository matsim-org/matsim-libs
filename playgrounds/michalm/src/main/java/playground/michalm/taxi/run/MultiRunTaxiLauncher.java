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
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.michalm.taxi.optimizer.AbstractTaxiOptimizerParams;
import playground.michalm.taxi.optimizer.AbstractTaxiOptimizerParams.TravelTimeSource;
import playground.michalm.taxi.util.stats.*;


class MultiRunTaxiLauncher
    extends TaxiLauncher
{
    private static final int[] RANDOM_SEEDS = { 463, 467, 479, 487, 491, 499, 503, 509, 521, 523,
            541, 547, 557, 563, 569, 571, 577, 587, 593, 599, 601, 607, 613, 617, 619, 631, 641,
            643, 647, 653 };

    private boolean warmup;
    private PrintWriter pw;


    MultiRunTaxiLauncher(Configuration config)
    {
        super(config);
    }


    void initOutputFiles(String outputFileSuffix)
    {
        try {
            pw = new PrintWriter(launcherParams.multiRunStatsDir + "/stats" + outputFileSuffix);
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


    void run(int runs, Configuration optimConfig)
    {
        if (runs < 0 || runs > RANDOM_SEEDS.length) {
            throw new RuntimeException();
        }

        optimParams = AbstractTaxiOptimizerParams.createParams(optimConfig);

        travelTimeCalculator = null;

        if (optimParams.travelTimeSource == TravelTimeSource.EVENTS) {
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
        String cfgId = optimParams.id;

        multiRunStats.printStats(pw, cfgId, data);
        pw.flush();
    }


    private void produceDetailedStats(int run)
    {
        HourlyTaxiStatsCalculator calculator = new HourlyTaxiStatsCalculator(
                context.getVrpData().getVehicles().values(), STATS_HOURS);
        HourlyTaxiStats.printAllStats(calculator.getStats(),
                launcherParams.detailedTaxiStatsDir + "/hourly_stats_run_" + run);
        HourlyHistograms.printAllHistograms(calculator.getHourlyHistograms(),
                launcherParams.detailedTaxiStatsDir + "/hourly_histograms_run_" + run);
        calculator.getDailyHistograms().printHistograms(
                launcherParams.detailedTaxiStatsDir + "/daily_histograms_run_" + run);
    }


    static void runAll(int runs, Configuration config)
    {
        List<Configuration> optimConfigs = TaxiConfigUtils.getOptimizerConfigs(config);

        MultiRunTaxiLauncher multiLauncher = new MultiRunTaxiLauncher(config);
        multiLauncher.initOutputFiles("");

        for (Configuration optimCfg : optimConfigs) {
            multiLauncher.run(runs, optimCfg);
        }

        multiLauncher.closeOutputFiles();
    }


    public static void main(String[] args)
    {
        int runs = 1;
        MultiRunTaxiLauncher.runAll(runs, TaxiConfigUtils.loadConfig(args[0]));
    }
}
