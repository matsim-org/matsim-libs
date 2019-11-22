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

package commercialtraffic;/*
 * created by jbischoff, 03.05.2019
 */

import commercialtraffic.commercialJob.ChangeCommercialJobOperator;
import commercialtraffic.commercialJob.CommercialTrafficConfigGroup;
import commercialtraffic.commercialJob.CommercialTrafficModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;

import static org.matsim.core.config.ConfigUtils.createConfig;
import static org.matsim.core.scenario.ScenarioUtils.loadScenario;

class RunCommercialTrafficExample {
    public static void main(String[] args) {


        String inputDir = "input/commercialtrafficIt/";

        Config config = createConfig();
        CommercialTrafficConfigGroup commercialTrafficConfigGroup = ConfigUtils.addOrGetModule(config, CommercialTrafficConfigGroup.class);
        commercialTrafficConfigGroup.setFirstLegTraveltimeBufferFactor(1.5);

        FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
        freightConfigGroup.setTravelTimeSliceWidth(3600);
        freightConfigGroup.setCarriersFile(inputDir + "test-carriers-car.xml");
        freightConfigGroup.setCarriersVehicleTypesFile(inputDir + "vehicleTypes.xml");

        prepareConfig(config, inputDir);

        Scenario scenario = loadScenario(config);
        FreightUtils.loadCarriersAccordingToFreightConfig(scenario); //assumes that input file paths are set in FreightConfigGroup
        //alternatively, one can read in the input Carriers and CarrierVehicleTypes manually and use
        //FreightUtils.getCarriers(scenario) and FreightUtils.getCarrierVehicleTypes(scenario)

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new CommercialTrafficModule() );
        controler.run();
    }

    private static void prepareConfig(Config config, String inputDir) {
        StrategyConfigGroup.StrategySettings changeExpBeta = new StrategyConfigGroup.StrategySettings();
        changeExpBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
        changeExpBeta.setWeight(0.5);
        config.strategy().addStrategySettings(changeExpBeta);

        StrategyConfigGroup.StrategySettings changeJobOperator = new StrategyConfigGroup.StrategySettings();
        changeJobOperator.setStrategyName(ChangeCommercialJobOperator.SELECTOR_NAME);
        changeJobOperator.setWeight(0.5);
        config.strategy().addStrategySettings(changeJobOperator);

        config.strategy().setFractionOfIterationsToDisableInnovation(.8);
        PlanCalcScoreConfigGroup.ActivityParams home = new PlanCalcScoreConfigGroup.ActivityParams("home");
        home.setTypicalDuration(14 * 3600);
        config.planCalcScore().addActivityParams(home);
        PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("work");
        work.setTypicalDuration(14 * 3600);
        work.setOpeningTime(8 * 3600);
        work.setClosingTime(8 * 3600);
        config.planCalcScore().addActivityParams(work);
        config.controler().setLastIteration(100);
        config.controler().setWriteEventsInterval(5);
        config.controler().setOutputDirectory("output/commercialtraffictestrunWithCar");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.network().setInputFile(inputDir + "grid_network.xml");
        config.plans().setInputFile(inputDir + "testpop.xml");
        config.controler().setLastIteration(5);
    }
}
