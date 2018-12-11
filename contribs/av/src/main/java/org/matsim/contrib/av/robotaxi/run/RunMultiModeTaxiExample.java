/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.av.robotaxi.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFareModule;
import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFaresConfigGroup;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityProvider;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.util.ArrayList;
import java.util.List;

public class RunMultiModeTaxiExample {
    private static final String CONFIG_FILE = "multi_mode_one_taxi/multi_mode_one_taxi_config.xml";

    public static void run(boolean otfvis, int lastIteration) {
        // load config
        Config config = ConfigUtils.loadConfig(CONFIG_FILE, new MultiModeTaxiConfigGroup(), new DvrpConfigGroup(),
                new OTFVisConfigGroup(), new TaxiFaresConfigGroup());
        config.controler().setLastIteration(lastIteration);
        config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
        config.checkConsistency();

        // load scenario
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // setup controler
        Controler controler = new Controler(scenario);

        MultiModeTaxiConfigGroup multiModeTaxiCfg = MultiModeTaxiConfigGroup.get(config);
        List<DvrpModeQSimModule> dvrpModeQSimModules = new ArrayList<>();
        for (TaxiConfigGroup taxiCfg : multiModeTaxiCfg.getTaxiConfigGroups()) {
            dvrpModeQSimModules.add(new DvrpModeQSimModule.Builder(taxiCfg.getMode()).build());
            controler.addQSimModule(new MultiModeTaxiQSimModule(taxiCfg));
            controler.addOverridingModule(new MultiModeTaxiModule(taxiCfg));
        }

        controler.addOverridingModule(new DvrpModule(dvrpModeQSimModules.stream().toArray(DvrpModeQSimModule[]::new)));

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(TravelDisutilityFactory.class).annotatedWith(Taxi.class)
                        .toInstance(travelTime -> new TimeAsTravelDisutility(travelTime));
            }
        });

        controler.addQSimModule(new AbstractQSimModule() {
            @Override
            protected void configureQSim() {
                bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();
                DvrpTravelDisutilityProvider.bindTravelDisutilityForOptimizer(binder(), Taxi.class);
            }
        });
        controler.addOverridingModule(new TaxiFareModule());
        if (otfvis) {
            controler.addOverridingModule(new OTFVisLiveModule()); // OTFVis visualisation
        }

        // run simulation
        controler.run();
    }

    public static void main(String[] args) {
        run(false, 0); // switch to 'true' to turn on visualisation
    }
}
