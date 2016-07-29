/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer;

import java.util.*;
import java.util.Map.Entry;

import org.matsim.contrib.taxi.benchmark.RunTaxiMiniBenchmark;
import org.matsim.contrib.taxi.benchmark.RunTaxiMiniBenchmark.MiniBenchmark;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerFactory.OptimizerType;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.*;


public class TaxiOptimizerTests
{
    public static class TaxiConfigVariant
    {
        final boolean destinationKnown;
        final boolean vehicleDiversion;
        final double pickupDuration;
        final double dropoffDuration;
        final double AStarEuclideanOverdoFactor;
        final boolean onlineVehicleTracker;


        private TaxiConfigVariant(boolean destinationKnown, boolean vehicleDiversion,
                double pickupDuration, double dropoffDuration, double AStarEuclideanOverdoFactor,
                boolean onlineVehicleTracker)
        {
            this.destinationKnown = destinationKnown;
            this.vehicleDiversion = vehicleDiversion;
            this.pickupDuration = pickupDuration;
            this.dropoffDuration = dropoffDuration;
            this.AStarEuclideanOverdoFactor = AStarEuclideanOverdoFactor;
            this.onlineVehicleTracker = onlineVehicleTracker;
        }


        void updateTaxiConfig(TaxiConfigGroup taxiCfg)
        {
            taxiCfg.setDestinationKnown(destinationKnown);
            taxiCfg.setVehicleDiversion(vehicleDiversion);
            taxiCfg.setPickupDuration(pickupDuration);
            taxiCfg.setDropoffDuration(dropoffDuration);
            taxiCfg.setAStarEuclideanOverdoFactor(AStarEuclideanOverdoFactor);
            taxiCfg.setOnlineVehicleTracker(onlineVehicleTracker);
        }
    }


    public static List<TaxiConfigVariant> createDefaultTaxiConfigVariants(
            boolean diversionSupported)
    {
        List<TaxiConfigVariant> variants = new ArrayList<>();

        // onlineVehicleTracker == false ==> vehicleDiversion == false
        variants.add(new TaxiConfigVariant(false, false, 120, 60, 1., false));
        variants.add(new TaxiConfigVariant(true, false, 0, 0, 1., false));

        // onlineVehicleTracker == true, vehicleDiversion == false
        variants.add(new TaxiConfigVariant(false, false, 120, 60, 1., true));
        variants.add(new TaxiConfigVariant(true, false, 120, 60, 1., true));

        if (diversionSupported) {
            // onlineVehicleTracker == true, vehicleDiversion == true
            variants.add(new TaxiConfigVariant(false, true, 0, 0, 1., true));
            variants.add(new TaxiConfigVariant(true, true, 120, 60, 1., true));
        }

        return variants;
    }


    public static Map<String, String> createAbstractOptimParams(OptimizerType type)
    {
        Map<String, String> params = new HashMap<>();
        params.put(AbstractTaxiOptimizerParams.ID, type.name());
        params.put(DefaultTaxiOptimizerFactory.TYPE, type.name());
        return params;
    }


    public static class BenchmarkWithConfig
    {
        private final Config config;
        private final MiniBenchmark miniBenchmark;


        public BenchmarkWithConfig(String plansSuffix, String taxisSuffix)
        {
            String dir = "./src/main/resources/mielec_2014_02/";
            String configFile = dir + "mielec_taxi_benchmark_config.xml";

            TaxiConfigGroup taxiCfg = new TaxiConfigGroup();
            config = ConfigUtils.loadConfig(configFile, taxiCfg);

            config.plans().setInputFile(
                    "plans_only_taxi_mini_benchmark_" + plansSuffix + ".xml.gz");
            taxiCfg.setTaxisFile(dir + "taxis_mini_benchmark-" + taxisSuffix + ".xml");

            miniBenchmark = RunTaxiMiniBenchmark.createMiniBenchmark(config);
        }
    }


    public static void runBenchmark(List<TaxiConfigVariant> variants, Map<String, String> params,
            BenchmarkWithConfig benchmark)
    {
        TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(benchmark.config);

        ConfigGroup optimizerCfg = new ConfigGroup(TaxiConfigGroup.OPTIMIZER_PARAMETER_SET);
        for (Entry<String, String> e : params.entrySet()) {
            optimizerCfg.addParam(e.getKey(), e.getValue());
        }
        taxiCfg.setOptimizerConfigGroup(optimizerCfg);

        for (TaxiConfigVariant v : variants) {
            v.updateTaxiConfig(taxiCfg);
            benchmark.miniBenchmark.run(new DefaultTaxiOptimizerFactory());
        }
    }
}
