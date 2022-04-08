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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.commercialTrafficApplications.jointDemand.ChangeCommercialJobOperator;
import org.matsim.contrib.commercialTrafficApplications.jointDemand.JointDemandConfigGroup;
import org.matsim.contrib.commercialTrafficApplications.jointDemand.JointDemandModule;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

class RunJointDemandDRTExample {

    private static final  String EXAMPLE_CONFIG = "scenarios/grid/jointDemand_config.xml";
    private static final Logger log = Logger.getLogger(RunJointDemandDRTExample.class);

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
        DrtConfigs.adjustMultiModeDrtConfig(MultiModeDrtConfigGroup.get(config), config.planCalcScore(),
                config.plansCalcRoute());

        Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
        ScenarioUtils.loadScenario(scenario);
        FreightUtils.loadCarriersAccordingToFreightConfig(scenario); //assumes that input file paths are set in FreightConfigGroup
        //alternatively, one can read in the input Carriers and CarrierVehicleTypes manually and use
        //FreightUtils.getCarriers(scenario) and FreightUtils.getCarrierVehicleTypes(scenario)

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new JointDemandModule());

        controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(config)));
        controler.run();

    }

    private static void prepareConfig(Config config) {
        loadConfigGroups(config);


        config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
        StrategyConfigGroup.StrategySettings changeExpBeta = new StrategyConfigGroup.StrategySettings();
        changeExpBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
        changeExpBeta.setWeight(0.5);
        config.strategy().addStrategySettings(changeExpBeta);
        StrategyConfigGroup.StrategySettings changeServiceOperator = new StrategyConfigGroup.StrategySettings();
        changeServiceOperator.setStrategyName(ChangeCommercialJobOperator.SELECTOR_NAME);
        changeServiceOperator.setWeight(0.5);
        config.strategy().addStrategySettings(changeServiceOperator);

        config.strategy().setFractionOfIterationsToDisableInnovation(.8);
        PlanCalcScoreConfigGroup.ActivityParams home = new PlanCalcScoreConfigGroup.ActivityParams("home");
        home.setTypicalDuration(14 * 3600);
        config.planCalcScore().addActivityParams(home);
        PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("work");
        work.setTypicalDuration(14 * 3600);
        work.setOpeningTime(8 * 3600);
        work.setClosingTime(8 * 3600);
        config.planCalcScore().addActivityParams(work);
        config.controler().setWriteEventsInterval(1);
        config.controler().setOutputDirectory("output/commercialTrafficApplications/jointDemand/RunJointDemandUsingDRTExample");
        config.controler()
                .setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        config.qsim().setEndTime(26 * 3600);
        config.qsim().setSimEndtimeInterpretation(QSimConfigGroup.EndtimeInterpretation.onlyUseEndtime);

        config.controler().setLastIteration(5);
    }

    private static void loadConfigGroups(Config config) {
        ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);
        MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);

        DrtConfigGroup drtCfg = new DrtConfigGroup();
        drtCfg.setMaxWaitTime(2 * 3600);
        drtCfg.setMaxTravelTimeAlpha(5);
        drtCfg.setMaxTravelTimeBeta(15 * 60);
        drtCfg.setStopDuration(60);
        drtCfg.setVehiclesFile("jointDemand_vehicles.xml");
        multiModeDrtConfigGroup.addParameterSet(drtCfg);
        drtCfg.addDrtInsertionSearchParams(new ExtensiveInsertionSearchParams() {});

        JointDemandConfigGroup jointDemandConfigGroup = ConfigUtils.addOrGetModule(config, JointDemandConfigGroup.class);
        jointDemandConfigGroup.setFirstLegTraveltimeBufferFactor(1.5);

        FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
        freightConfigGroup.setCarriersFile("jointDemand_carriers_drt.xml");
        freightConfigGroup.setCarriersVehicleTypesFile("jointDemand_vehicleTypes.xml");
    }
}
