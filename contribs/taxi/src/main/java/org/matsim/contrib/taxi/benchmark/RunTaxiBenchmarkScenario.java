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
import org.matsim.core.config.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scenario.ScenarioUtils.ScenarioBuilder;


/**
 * For a fair and consistent benchmarking of taxi dispatching algorithms we assume that we use
 * time-variant network with variable speeds on links in each time interval (usually 15 minutes).
 * There is no other traffic and the flow capacity factor is increased at least by 100 (to prevent
 * time delays at nodes if many taxis are moving over one)
 */
public class RunTaxiBenchmarkScenario
{
    public static void run(String configFile, int runs)
    {
        final TaxiConfigGroup taxiCfg = new TaxiConfigGroup();
        Config config = ConfigUtils.loadConfig(configFile, taxiCfg);
        config.addConfigConsistencyChecker(new TaxiBenchmarkConfigConsistencyChecker());
        config.checkConsistency();

        config.controler().setLastIteration(runs);

        Scenario scenario = loadScenarioWithFixedIntervalTimeVariantLinks(config, 15 * 60,
                30 * 3600);
        final TaxiData taxiData = new TaxiData();
        new VehicleReader(scenario.getNetwork(), taxiData).parse(taxiCfg.getTaxisFile());

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new TaxiModule(taxiData));
        controler.addOverridingModule(VrpTravelTimeModules.createFreespeedTravelTimeModule());
        controler.addOverridingModule(new DynQSimModule<>(TaxiQSimProvider.class));

        controler.addControlerListener(new TaxiSimulationConsistencyChecker(taxiData));

        String id = taxiCfg.getOptimizerConfigGroup().getValue(AbstractTaxiOptimizerParams.ID);
        controler.addControlerListener(
                new MultiRunStats(taxiData, config.controler().getOutputDirectory(), id));

        controler.run();

    }


    private static Scenario loadScenarioWithFixedIntervalTimeVariantLinks(Config config,
            int interval, int maxTime)
    {
        Scenario scenario = new ScenarioBuilder(config).build();
        ((NetworkImpl)scenario.getNetwork()).getFactory()
                .setLinkFactory(new FixedIntervalTimeVariantLinkFactory(interval, maxTime));
        ScenarioUtils.loadScenario(scenario);
        return scenario;
    }


    public static void main(String[] args)
    {
        run("d:/eclipse/matsim-all/contribs/taxi/src/main/resources/one_taxi/one_taxi_config.xml",
                20);
    }
}
