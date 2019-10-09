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
import org.matsim.core.config.Config;
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
        CommercialTrafficConfigGroup commercialTrafficConfigGroup = new CommercialTrafficConfigGroup();
        commercialTrafficConfigGroup.setCarriersFile(inputDir + "test-carriers-car.xml");
        commercialTrafficConfigGroup.setCarriersVehicleTypesFile(inputDir + "vehicleTypes.xml");
        commercialTrafficConfigGroup.setFirstLegTraveltimeBufferFactor(1.5);
        commercialTrafficConfigGroup.setCarriersVehicleTypesFile(inputDir + "carrier_vehicletypes.xml");
        commercialTrafficConfigGroup.setjSpritTimeSliceWidth(3600);
        config.addModule(commercialTrafficConfigGroup);

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

        Scenario scenario = loadScenario(config);

        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new CommercialTrafficModule() );

        controler.run();


    }
}
