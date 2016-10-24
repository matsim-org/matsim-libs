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

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.*;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vehicles.*;


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
public class RunTaxiMiniBenchmark
{
    public interface MiniBenchmark
    {
        public void run(TaxiOptimizerFactory optimizerFactory);
    }


    public static void run(String configFile, TaxiOptimizerFactory optimizerFactory, int runs)
    {
        final TaxiConfigGroup taxiCfg = new TaxiConfigGroup();
        Config config = ConfigUtils.loadConfig(configFile, taxiCfg);

        for (int r = 0; r < runs; r++) {
            createMiniBenchmark(config).run(optimizerFactory);
        }
    }


    public static MiniBenchmark createMiniBenchmark(Config config)
    {
        TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
        config.addConfigConsistencyChecker(new TaxiBenchmarkConfigConsistencyChecker());
        config.checkConsistency();

        final Scenario scenario = RunTaxiBenchmark.loadBenchmarkScenario(config, 15 * 60,
                30 * 3600);
        final TaxiData taxiData = new TaxiData();
        new VehicleReader(scenario.getNetwork(), taxiData).readFile(taxiCfg.getTaxisFile());

        final EventsManager events = EventsUtils.createEventsManager();
        final Collection<AbstractQSimPlugin> plugins = DynQSimModule.createQSimPlugins(config);
        final TravelTime estimatedTravelTime = new FreeSpeedTravelTime();
        final VehicleType vehicleType = VehicleUtils.getDefaultVehicleType();

        return new MiniBenchmark() {
            public void run(TaxiOptimizerFactory optimizerFactory)
            {
                new TaxiQSimProvider(events, plugins, scenario, taxiData, estimatedTravelTime,
                        vehicleType, optimizerFactory).get().run();
            }
        };
    }


    public static void main(String[] args)
    {
        run("./src/main/resources/one_taxi_benchmark/one_taxi_benchmark_config.xml",
                new DefaultTaxiOptimizerFactory(), 20);
    }
}
