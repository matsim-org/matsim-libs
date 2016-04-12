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

package org.matsim.contrib.taxi.benchmark;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;
import org.matsim.contrib.taxi.run.*;
import org.matsim.contrib.taxi.util.TaxiSimulationConsistencyChecker;
import org.matsim.contrib.taxi.util.stats.*;
import org.matsim.core.config.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scenario.ScenarioUtils.ScenarioBuilder;


/**
 * For a fair and consistent benchmarking of taxi dispatching algorithms we assume that link travel
 * times are deterministic. To simulate this property, we remove (1) all other traffic, and (2) link
 * capacity constraints (e.g. by increasing the capacities by 100+ times), as a result all vehicles
 * move with the free-flow speed (which is the effective speed).
 * <p/>
 * To model the impact of traffic, we can use a time-variant network, where we specify different
 * free-flow speeds for each link over time. The default approach is to specify free-flow speeds in
 * each time interval (usually 15 minutes).
 */
public class RunTaxiBenchmark
{
    public static void run(String configFile, int runs)
    {
        final TaxiConfigGroup taxiCfg = new TaxiConfigGroup();
        Config config = ConfigUtils.loadConfig(configFile, taxiCfg);
        config.addConfigConsistencyChecker(new TaxiBenchmarkConfigConsistencyChecker());
        config.checkConsistency();

        config.controler().setLastIteration(runs);

        Scenario scenario = loadBenchmarkScenario(config, 15 * 60, 30 * 3600);
        final TaxiData taxiData = new TaxiData();
        new VehicleReader(scenario.getNetwork(), taxiData).parse(taxiCfg.getTaxisFile());

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new TaxiModule(taxiData));
        controler.addOverridingModule(VrpTravelTimeModules.createFreespeedTravelTimeModule(false));
        controler.addOverridingModule(new DynQSimModule<>(TaxiQSimProvider.class));

        controler.addControlerListener(new TaxiSimulationConsistencyChecker(taxiData));

        String outputDir = config.controler().getOutputDirectory();
        controler.addControlerListener(new TaxiStatsDumper(taxiData, outputDir));

        if (taxiCfg.getDetailedStats()) {
            controler.addControlerListener(new DetailedTaxiStatsDumper(taxiData, controler, 30));
        }

        String id = taxiCfg.getOptimizerConfigGroup().getValue(AbstractTaxiOptimizerParams.ID);
        controler.addControlerListener(new TaxiBenchmarkStats(taxiData, outputDir, id));

        controler.run();
    }


    private static Scenario loadBenchmarkScenario(Config config, int interval, int maxTime)
    {
        Scenario scenario = new ScenarioBuilder(config).build();

        if (config.network().isTimeVariantNetwork()) {
            ((NetworkImpl)scenario.getNetwork()).getFactory()
                    .setLinkFactory(new FixedIntervalTimeVariantLinkFactory(interval, maxTime));
        }

        ScenarioUtils.loadScenario(scenario);
        return scenario;
    }


    public static void main(String[] args)
    {
        run("./src/main/resources/one_taxi_benchmark/one_taxi_benchmark_config.xml", 20);
    }
}
