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

import commercialtraffic.integration.CommercialTrafficConfigGroup;
import commercialtraffic.integration.CommercialTrafficModule;
import commercialtraffic.replanning.ChangeDeliveryServiceOperator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;

import static org.matsim.core.config.ConfigUtils.createConfig;
import static org.matsim.core.scenario.ScenarioUtils.loadScenario;

public class RunCommercialTrafficExample {
    public static void main(String[] args) {

        String inputDir = "D:\\Thiel\\Programme\\WVModell\\01_MatSimInput\\Test\\";

        Config config = createConfig();
        CommercialTrafficConfigGroup commercialTrafficConfigGroup = new CommercialTrafficConfigGroup();
        commercialTrafficConfigGroup.setCarriersFile(inputDir + "carrier_definition.xml");
        commercialTrafficConfigGroup.setCarriersVehicleTypesFile(inputDir + "carrier_vehicletypes.xml");
        commercialTrafficConfigGroup.setFirstLegTraveltimeBufferFactor(1.5);
        config.addModule(commercialTrafficConfigGroup);
        StrategyConfigGroup.StrategySettings changeExpBeta = new StrategyConfigGroup.StrategySettings();
        changeExpBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
        changeExpBeta.setWeight(0.5);
        config.strategy().addStrategySettings(changeExpBeta);

//        StrategyConfigGroup.StrategySettings changeServiceOperator = new StrategyConfigGroup.StrategySettings();
//        changeServiceOperator.setStrategyName(ChangeDeliveryServiceOperator.SELECTOR_NAME);
//        changeServiceOperator.setWeight(0.5);
//        config.strategy().addStrategySettings(changeServiceOperator);

        config.strategy().setFractionOfIterationsToDisableInnovation(.8);
//        PlanCalcScoreConfigGroup.ActivityParams home = new PlanCalcScoreConfigGroup.ActivityParams("home");
//        home.setTypicalDuration(14 * 3600);
//        config.planCalcScore().addActivityParams(home);
//        PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("work");
//        work.setTypicalDuration(14 * 3600);
//        work.setOpeningTime(8 * 3600);
//        work.setClosingTime(8 * 3600);
//        config.planCalcScore().addActivityParams(work);
        config.controler().setLastIteration(10);
        config.controler().setWriteEventsInterval(1);
        config.controler().setOutputDirectory("D:\\Thiel\\Programme\\WVModell\\01_MatSimInput\\Test\\output\\commercialtraffictestrun");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.network().setInputFile(inputDir + "network_editedPt.xml.gz");
        config.plans().setInputFile(inputDir + "populationWithCTdemand.xml.gz");

        config.controler().setLastIteration(5);

        Scenario scenario = loadScenario(config);

        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new CommercialTrafficModule(config, (carrierId -> 20)));

        controler.run();


    }
}
