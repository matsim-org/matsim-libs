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
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

import static org.matsim.core.config.ConfigUtils.createConfig;
import static org.matsim.core.scenario.ScenarioUtils.loadScenario;

public class RunCommercialTraffic_H {
	public static void main(String[] args) {
		String runId = "vw280_0.1_CT_KEP";
		String pct = ".0.1";

		String inputDir = "D:\\Thiel\\Programme\\WVModell\\01_MatSimInput\\vw280_0.1_CT_0.1\\";

		Config config = ConfigUtils.loadConfig(inputDir + "config_0.1_CT.xml", new CommercialTrafficConfigGroup());
		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration);
		config.global().setNumberOfThreads(16);
		config.parallelEventHandling().setNumberOfThreads(16);
		config.qsim().setNumberOfThreads(16);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.75); //Fraction to disable Innovation
		
		//RECREATE ACTIVITY PARAMS 
		{
		config.planCalcScore().getActivityParams().clear();
		// activities:
		for ( long ii = 1 ; ii <= 30; ii+=1 ) {

					config.planCalcScore().addActivityParams( new ActivityParams( "home_" + ii ).setTypicalDuration( ii*3600 ) );

					config.planCalcScore().addActivityParams( new ActivityParams( "work_" + ii ).setTypicalDuration( ii*3600 ).setOpeningTime(6. * 3600. ).setClosingTime(20. * 3600. ) );

					config.planCalcScore().addActivityParams( new ActivityParams( "leisure_" + ii ).setTypicalDuration( ii*3600 ).setOpeningTime(9. * 3600. ).setClosingTime(27. * 3600. ) );

					config.planCalcScore().addActivityParams( new ActivityParams( "shopping_" + ii).setTypicalDuration( ii*3600 ).setOpeningTime(8. * 3600. ).setClosingTime(21. * 3600. ) );

					config.planCalcScore().addActivityParams( new ActivityParams( "other_" + ii ).setTypicalDuration( ii*3600 ) );

		}
		
		config.planCalcScore().addActivityParams( new ActivityParams( "home" ).setTypicalDuration( 14*3600 ) );
		config.planCalcScore().addActivityParams( new ActivityParams( "work").setTypicalDuration( 8*3600 ).setOpeningTime(6. * 3600. ).setClosingTime(20. * 3600. ) );
		config.planCalcScore().addActivityParams( new ActivityParams( "leisure" ).setTypicalDuration( 1*3600 ).setOpeningTime(9. * 3600. ).setClosingTime(27. * 3600. ) );
		config.planCalcScore().addActivityParams( new ActivityParams( "shopping").setTypicalDuration( 1*3600 ).setOpeningTime(8. * 3600. ).setClosingTime(21. * 3600. ) );
		config.planCalcScore().addActivityParams( new ActivityParams( "other" ).setTypicalDuration( 1*3600 ) );
		config.planCalcScore().addActivityParams( new ActivityParams( "education").setTypicalDuration(8*3600 ).setOpeningTime(8. * 3600. ).setClosingTime(18. * 3600. ) );
		}

		StrategyConfigGroup.StrategySettings changeExpBeta = new StrategyConfigGroup.StrategySettings();
		changeExpBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
		changeExpBeta.setWeight(0.5);
		config.strategy().addStrategySettings(changeExpBeta);

		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks );
		config.plansCalcRoute().setRoutingRandomness( 3. );
		config.controler().setWriteEventsInterval(5);
		config.controler().setOutputDirectory("D:\\Thiel\\Programme\\WVModell\\02_MatSimOutput\\" + runId + pct);
		config.controler()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		// config.qsim().setVehiclesSource(VehiclesSource.defaultVehicle);
		// vsp defaults
		config.qsim().setUsingTravelTimeCheckInTeleportation( true );
		config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );
//		config.plansCalcRoute().setInsertingAccessEgressWalk( true );
		

		config.network().setInputFile(inputDir + "Network\\network_editedPt.xml.gz");
		config.plans().setInputFile(inputDir + "Population\\populationWithCTdemand.xml.gz");
		config.transit().setTransitScheduleFile(inputDir + "Network\\transitSchedule.xml.gz");
		config.transit().setVehiclesFile(inputDir + "Network\\transitVehicles.xml.gz");
		
		CommercialTrafficConfigGroup ctcg = (CommercialTrafficConfigGroup) config.getModules().get(CommercialTrafficConfigGroup.GROUP_NAME);
		ctcg.setCarriersFile(inputDir+"Carrier\\carrier_definition.xml");
		ctcg.setCarriersVehicleTypesFile(inputDir+"Carrier\\carrier_vehicletypes.xml");
		ctcg.setjSpritTimeSliceWidth(3600);
		
//        StrategyConfigGroup.StrategySettings changeServiceOperator = new StrategyConfigGroup.StrategySettings();
//        changeServiceOperator.setStrategyName(ChangeDeliveryServiceOperator.SELECTOR_NAME);
//        changeServiceOperator.setWeight(0.5);
//        config.strategy().addStrategySettings(changeServiceOperator);
		
		
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
		
		
		config.controler().setLastIteration(5);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.00); // Fraction to disable Innovation
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
            if(carrierId.toString().startsWith("KEP")) return 100;
            return 3;
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
