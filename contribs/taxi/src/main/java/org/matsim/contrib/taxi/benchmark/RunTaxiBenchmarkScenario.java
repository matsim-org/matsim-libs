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
import org.matsim.contrib.dvrp.run.VrpQSimConfigConsistencyChecker;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.run.*;
import org.matsim.contrib.taxi.util.TaxiSimulationConsistencyChecker;
import org.matsim.core.config.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;


public class RunTaxiBenchmarkScenario
{
    public static void run(String configFile)
    {
        final TaxiConfigGroup taxiCfg = new TaxiConfigGroup();
        Config config = ConfigUtils.loadConfig(configFile, taxiCfg);
        config.addConfigConsistencyChecker(new VrpQSimConfigConsistencyChecker());
        config.checkConsistency();

        if (!config.network().isTimeVariantNetwork()
                && config.network().getChangeEventsInputFile() == null) {
            throw new IllegalStateException("only timevariant network");
        }

        if (config.qsim().getFlowCapFactor() < 100) {
            System.err.println("FlowCapFactor should be large enough (e.g. 100) to obtain "
                    + "free flow");
        }

        // network.getFactory().setLinkFactory(new FixedIntervalTimeVariantLinkFactory(interval, intervalCount));
        
        //TODO
        //MultiRunStats

        Scenario scenario = ScenarioUtils.loadScenario(config);
        TaxiData taxiData = new TaxiData();
        new VehicleReader(scenario.getNetwork(), taxiData).parse(taxiCfg.getTaxisFile());

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new TaxiModule(taxiData));
        controler.addOverridingModule(VrpTravelTimeModules.createFreespeedTravelTimeModule());
        controler.addOverridingModule(new DynQSimModule<>(TaxiQSimProvider.class));

        controler.addControlerListener(new TaxiSimulationConsistencyChecker(taxiData));

        controler.run();
    }


    public static void main(String[] args)
    {
        run("d:/eclipse/matsim-all/contribs/taxi/src/main/resources/one_taxi/one_taxi_config.xml");
    }
}
