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

package org.matsim.contrib.commercialTrafficApplications.jointDemand.examples;/*
 * created by jbischoff, 03.05.2019
 */

import static org.matsim.core.config.ConfigUtils.loadConfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.commercialTrafficApplications.jointDemand.ChangeCommercialJobOperator;
import org.matsim.contrib.commercialTrafficApplications.jointDemand.JointDemandConfigGroup;
import org.matsim.contrib.commercialTrafficApplications.jointDemand.JointDemandModule;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.optimizer.constraints.DefaultDrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.ConfigGroup;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

class RunJointDemandDRTExample {

    private static final  String EXAMPLE_CONFIG = "scenarios/grid/jointDemand_config.xml";
    private static final Logger log = LogManager.getLogger(RunJointDemandDRTExample.class);

    public static void main(String[] args) throws IOException {
        final URL configUrl;
        if (args.length > 0) {
            log.info("Starting simulation run with the following arguments:");
            configUrl = new URL(args[0]);
            log.info("config URL: " + configUrl);
        } else {
            File localConfigFile = new File(EXAMPLE_CONFIG);
            if (localConfigFile.exists()) {
                log.info("Starting simulation run with the local example config file");
                configUrl = localConfigFile.toURI().toURL();
            } else {
                log.info("Starting simulation run with the example config file from GitHub repository");
                configUrl = new URL("https://raw.githubusercontent.com/matsim-org/matsim/master/contribs/commercialTrafficApplications/"
                        + EXAMPLE_CONFIG);
            }
        }
        new RunJointDemandDRTExample().run(configUrl);
    }

    public void run(URL configURL){
        Config config = loadConfig(configURL);
        prepareConfig(config);
        DrtConfigs.adjustMultiModeDrtConfig(MultiModeDrtConfigGroup.get(config), config.scoring(),
                config.routing());

        Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
        ScenarioUtils.loadScenario(scenario);
        CarriersUtils.loadCarriersAccordingToFreightConfig(scenario); //assumes that input file paths are set in FreightCarriersConfigGroup
        //alternatively, one can read in the input Carriers and CarrierVehicleTypes manually and use
        //CarrierControlerUtils.getCarriers(scenario) and CarrierControlerUtils.getCarrierVehicleTypes(scenario)

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new JointDemandModule());

        controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(config)));
        controler.run();

    }

    private static void prepareConfig(Config config) {
        loadConfigGroups(config);


        config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
        ReplanningConfigGroup.StrategySettings changeExpBeta = new ReplanningConfigGroup.StrategySettings();
        changeExpBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
        changeExpBeta.setWeight(0.5);
        config.replanning().addStrategySettings(changeExpBeta);
        ReplanningConfigGroup.StrategySettings changeServiceOperator = new ReplanningConfigGroup.StrategySettings();
        changeServiceOperator.setStrategyName(ChangeCommercialJobOperator.SELECTOR_NAME);
        changeServiceOperator.setWeight(0.5);
        config.replanning().addStrategySettings(changeServiceOperator);

        config.replanning().setFractionOfIterationsToDisableInnovation(.8);
        ScoringConfigGroup.ActivityParams home = new ScoringConfigGroup.ActivityParams("home");
        home.setTypicalDuration(14 * 3600);
        config.scoring().addActivityParams(home);
        ScoringConfigGroup.ActivityParams work = new ScoringConfigGroup.ActivityParams("work");
        work.setTypicalDuration(14 * 3600);
        work.setOpeningTime(8 * 3600);
        work.setClosingTime(8 * 3600);
        config.scoring().addActivityParams(work);
        config.controller().setWriteEventsInterval(1);
        config.controller().setOutputDirectory("output/commercialTrafficApplications/jointDemand/RunJointDemandUsingDRTExample");
        config.controller()
                .setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        config.qsim().setEndTime(26 * 3600);
        config.qsim().setSimEndtimeInterpretation(QSimConfigGroup.EndtimeInterpretation.onlyUseEndtime);

        config.controller().setLastIteration(5);
    }

    private static void loadConfigGroups(Config config) {
		DvrpConfigGroup dvrpConfigGroup = ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);
		ConfigGroup zoneParams = dvrpConfigGroup.getTravelTimeMatrixParams().createParameterSet(SquareGridZoneSystemParams.SET_NAME);
		dvrpConfigGroup.getTravelTimeMatrixParams().addParameterSet(zoneParams);

		MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);

        DrtConfigGroup drtCfg = new DrtConfigGroup();
        DefaultDrtOptimizationConstraintsSet defaultConstraintsSet =
                (DefaultDrtOptimizationConstraintsSet) drtCfg.addOrGetDrtOptimizationConstraintsParams()
                        .addOrGetDefaultDrtOptimizationConstraintsSet();
        defaultConstraintsSet.maxWaitTime = 2 * 3600;
		defaultConstraintsSet.maxTravelTimeAlpha = 5;
		defaultConstraintsSet.maxTravelTimeBeta = 15 * 60;
        drtCfg.stopDuration = 60;
        drtCfg.vehiclesFile = "jointDemand_vehicles.xml";
        multiModeDrtConfigGroup.addParameterSet(drtCfg);
        drtCfg.addDrtInsertionSearchParams(new ExtensiveInsertionSearchParams() {});

        JointDemandConfigGroup jointDemandConfigGroup = ConfigUtils.addOrGetModule(config, JointDemandConfigGroup.class);
        jointDemandConfigGroup.setFirstLegTraveltimeBufferFactor(1.5);

        FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
        freightCarriersConfigGroup.setCarriersFile("jointDemand_carriers_drt.xml");
        freightCarriersConfigGroup.setCarriersVehicleTypesFile("jointDemand_vehicleTypes.xml");
    }
}
