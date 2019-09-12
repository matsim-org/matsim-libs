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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

import static org.matsim.core.config.ConfigUtils.createConfig;
import static org.matsim.core.scenario.ScenarioUtils.loadScenario;

public class RunCommercialTraffic_H {
	public static void main(String[] args) {
		String runId = "vw243_0.1_EGrocery0.1_shops_changeCar";
		String pct = ".0.1";

		String inputDir = "D:\\Thiel\\Programme\\WVModell\\01_MatSimInput\\vw243_0.1_EGrocery0.1_shops_changeCar\\";

		Config config = ConfigUtils.loadConfig(inputDir + "config_0.1.xml", new CommercialTrafficConfigGroup());

		StrategyConfigGroup.StrategySettings changeExpBeta = new StrategyConfigGroup.StrategySettings();
		changeExpBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
		changeExpBeta.setWeight(0.5);
		config.strategy().addStrategySettings(changeExpBeta);
		config.controler().setWriteEventsInterval(5);
		config.controler().setOutputDirectory("D:\\Thiel\\Programme\\WVModell\\02_MatSimOutput\\" + runId + pct);
		config.controler()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		// config.qsim().setVehiclesSource(VehiclesSource.defaultVehicle);

		config.network().setInputFile(inputDir + "Network\\network_editedPt.xml.gz");
		config.plans().setInputFile(inputDir + "Population\\populationWithCTdemand.xml.gz");
		
		CommercialTrafficConfigGroup ctcg = (CommercialTrafficConfigGroup) config.getModules().get(CommercialTrafficConfigGroup.GROUP_NAME);
		ctcg.setCarriersFile(inputDir+"Carrier\\carrier_definition.xml");
		ctcg.setCarriersVehicleTypesFile(inputDir+"Carrier\\carrier_vehicletypes.xml");
		ctcg.setjSpritTimeSliceWidth(3600);
		
        StrategyConfigGroup.StrategySettings changeServiceOperator = new StrategyConfigGroup.StrategySettings();
        changeServiceOperator.setStrategyName(ChangeDeliveryServiceOperator.SELECTOR_NAME);
        changeServiceOperator.setWeight(0.5);
        config.strategy().addStrategySettings(changeServiceOperator);
		
		
		//Config for StayHome Act
		PlanCalcScoreConfigGroup.ModeParams scoreParams =  new PlanCalcScoreConfigGroup.ModeParams("preventedShoppingTrip");
		config.planCalcScore().addModeParams(scoreParams);
		
		PlansCalcRouteConfigGroup.ModeRoutingParams params = new PlansCalcRouteConfigGroup.ModeRoutingParams();
		params.setMode("preventedShoppingTrip");
		params.setTeleportedModeFreespeedLimit(100000d);
		params.setTeleportedModeSpeed(100000d);
		params.setBeelineDistanceFactor(1.3);
		config.plansCalcRoute().addModeRoutingParams(params);

		config.planCalcScore().addModeParams(scoreParams);
		
		
		config.controler().setLastIteration(50);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.80); // Fraction to disable Innovation
		Scenario scenario = loadScenario(config);
		adjustPtNetworkCapacity(scenario.getNetwork(), config.qsim().getFlowCapFactor());

		Controler controler = new Controler(scenario);
		config.controler().setRunId(runId + pct);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
			}
		});

		controler.addOverridingModule(new SwissRailRaptorModule());

		controler.addOverridingModule(new CommercialTrafficModule(config, carrierId -> {
            if(carrierId.toString().startsWith("H1")) return 15;
            return 1;
        }));

		controler.run();

	}

	private static void adjustPtNetworkCapacity(Network network, double flowCapacityFactor) {
		if (flowCapacityFactor < 1.0) {
			for (Link l : network.getLinks().values()) {
				if (l.getAllowedModes().contains(TransportMode.pt)) {
					l.setCapacity(l.getCapacity() / flowCapacityFactor);
				}
			}
		}
	}
}
