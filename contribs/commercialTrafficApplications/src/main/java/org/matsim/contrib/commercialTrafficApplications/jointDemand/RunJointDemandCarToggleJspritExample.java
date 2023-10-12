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

package org.matsim.contrib.commercialTrafficApplications.jointDemand;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.matsim.core.config.ConfigUtils.loadConfig;
import static org.matsim.core.scenario.ScenarioUtils.loadScenario;

class RunJointDemandCarToggleJspritExample {

    private static final  String EXAMPLE_CONFIG = "scenarios/grid/jointDemand_config.xml";
    private static final Logger log = LogManager.getLogger(RunJointDemandCarToggleJspritExample.class);

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
        new RunJointDemandCarToggleJspritExample().run(configUrl);
    }

    public void run(URL configUrl){
        Config config = loadConfig(configUrl);
        JointDemandConfigGroup jointDemandConfigGroup = ConfigUtils.addOrGetModule(config, JointDemandConfigGroup.class);
        jointDemandConfigGroup.setFirstLegTraveltimeBufferFactor(1.5);
        jointDemandConfigGroup.setChangeCommercialJobOperatorInterval(2);

        FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
        freightCarriersConfigGroup.setTravelTimeSliceWidth(3600);
        freightCarriersConfigGroup.setCarriersFile("jointDemand_carriers_car.xml");
        freightCarriersConfigGroup.setCarriersVehicleTypesFile("jointDemand_vehicleTypes.xml");

        prepareConfig(config);

        Scenario scenario = loadScenario(config);
        CarriersUtils.loadCarriersAccordingToFreightConfig(scenario); //assumes that input file paths are set in FreightCarriersConfigGroup
        //alternatively, one can read in the input Carriers and CarrierVehicleTypes manually and use
        //CarrierControlerUtils.getCarriers(scenario) and CarrierControlerUtils.getCarrierVehicleTypes(scenario)

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new JointDemandModule() );
        controler.run();
    }

    private static void prepareConfig(Config config) {
        ReplanningConfigGroup.StrategySettings changeExpBeta = new ReplanningConfigGroup.StrategySettings();
        changeExpBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
        changeExpBeta.setWeight(0.5);
        config.replanning().addStrategySettings(changeExpBeta);

        ReplanningConfigGroup.StrategySettings changeJobOperator = new ReplanningConfigGroup.StrategySettings();
        changeJobOperator.setStrategyName(ChangeCommercialJobOperator.SELECTOR_NAME);
        changeJobOperator.setWeight(0.5);
        config.replanning().addStrategySettings(changeJobOperator);

        config.replanning().setFractionOfIterationsToDisableInnovation(.8);
        ScoringConfigGroup.ActivityParams home = new ScoringConfigGroup.ActivityParams("home");
        home.setTypicalDuration(14 * 3600);
        config.scoring().addActivityParams(home);
        ScoringConfigGroup.ActivityParams work = new ScoringConfigGroup.ActivityParams("work");
        work.setTypicalDuration(14 * 3600);
        work.setOpeningTime(8 * 3600);
        work.setClosingTime(8 * 3600);
        config.scoring().addActivityParams(work);
        config.controller().setWriteEventsInterval(5);
        config.controller().setOutputDirectory("output/commercialTrafficApplications/jointDemand/RunJointDemandCarToggleJspritExample");
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controller().setLastIteration(5);
    }
}
