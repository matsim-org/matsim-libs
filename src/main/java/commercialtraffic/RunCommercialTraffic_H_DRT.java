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

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import commercialtraffic.commercialJob.CommercialTrafficConfigGroup;
import commercialtraffic.commercialJob.CommercialTrafficModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;
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
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

public class RunCommercialTraffic_H_DRT {
	public static void main(String[] args) {
		String runId = "vw280_CT_DRT_Q_0.3_DRT";
		String pct = ".0.1";

		String inputDir = "D:\\Thiel\\Programme\\WVModell\\01_MatSimInput\\CT_DRT\\CT_DRT_0.1_Q_0.3_DRT\\";

		Config config = ConfigUtils.loadConfig(inputDir + "config_0.1_CT.xml", new CommercialTrafficConfigGroup());
		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration);
		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setSimEndtimeInterpretation(QSimConfigGroup.EndtimeInterpretation.onlyUseEndtime);
		config.global().setNumberOfThreads(16);
		config.parallelEventHandling().setNumberOfThreads(16);
		config.qsim().setNumberOfThreads(1);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.75); // Fraction to disable Innovation

		// RECREATE ACTIVITY PARAMS
		{
			config.planCalcScore().getActivityParams().clear();
			// activities:
			for (long ii = 1; ii <= 30; ii += 1) {

				config.planCalcScore()
						.addActivityParams(new ActivityParams("home_" + ii).setTypicalDuration(ii * 3600));

				config.planCalcScore().addActivityParams(new ActivityParams("work_" + ii).setTypicalDuration(ii * 3600)
						.setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));

				config.planCalcScore().addActivityParams(new ActivityParams("leisure_" + ii)
						.setTypicalDuration(ii * 3600).setOpeningTime(9. * 3600.).setClosingTime(27. * 3600.));

				config.planCalcScore().addActivityParams(new ActivityParams("shopping_" + ii)
						.setTypicalDuration(ii * 3600).setOpeningTime(8. * 3600.).setClosingTime(21. * 3600.));

				config.planCalcScore()
						.addActivityParams(new ActivityParams("other_" + ii).setTypicalDuration(ii * 3600));

			}

			config.planCalcScore().addActivityParams(new ActivityParams("home").setTypicalDuration(14 * 3600));
			config.planCalcScore().addActivityParams(new ActivityParams("work").setTypicalDuration(8 * 3600)
					.setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
			config.planCalcScore().addActivityParams(new ActivityParams("leisure").setTypicalDuration(1 * 3600)
					.setOpeningTime(9. * 3600.).setClosingTime(27. * 3600.));
			config.planCalcScore().addActivityParams(new ActivityParams("shopping").setTypicalDuration(1 * 3600)
					.setOpeningTime(8. * 3600.).setClosingTime(21. * 3600.));
			config.planCalcScore().addActivityParams(new ActivityParams("other").setTypicalDuration(1 * 3600));
			config.planCalcScore().addActivityParams(new ActivityParams("education").setTypicalDuration(8 * 3600)
					.setOpeningTime(8. * 3600.).setClosingTime(18. * 3600.));
		}

		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks);
		config.plansCalcRoute().setRoutingRandomness(3.);
		config.controler().setWriteEventsInterval(5);
		config.controler().setOutputDirectory("D:\\Thiel\\Programme\\WVModell\\02_MatSimOutput\\" + runId + pct);
		config.controler()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		// config.qsim().setVehiclesSource(VehiclesSource.defaultVehicle);
		// vsp defaults
		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);
		// config.plansCalcRoute().setInsertingAccessEgressWalk( true );

		config.network().setInputFile(inputDir + "Network\\network_editedPt.xml.gz");
		config.plans().setInputFile(inputDir + "Population\\populationWithCTdemand.xml.gz");
		config.transit().setTransitScheduleFile(inputDir + "Network\\transitSchedule.xml.gz");
		config.transit().setVehiclesFile(inputDir + "Network\\transitVehicles.xml.gz");

		CommercialTrafficConfigGroup commercialTrafficConfigGroup = ConfigUtils.addOrGetModule(config,
				CommercialTrafficConfigGroup.class);
		commercialTrafficConfigGroup.setFirstLegTraveltimeBufferFactor(1.5);

		FreightConfigGroup ctcg = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		ctcg.setCarriersFile(inputDir + "Carrier\\carrier_definition.xml");
		ctcg.setCarriersVehicleTypesFile(inputDir + "Carrier\\carrier_vehicletypes.xml");
		ctcg.setTravelTimeSliceWidth(3600);

		ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config,
				MultiModeDrtConfigGroup.class);
		DrtConfigGroup drtCfg = new DrtConfigGroup();

		String drtTag = "car";
		vwExamples.utils.CreateStopsFromGrid.run(config.network().getInputFile(), 100.0, drtTag);
		drtCfg.setTransitStopFile(inputDir + "\\network\\virtualStops.xml");
		drtCfg.setMaxWalkDistance(300.0);
		drtCfg.setMaxWaitTime(600);
		drtCfg.setMaxTravelTimeAlpha(5);
		drtCfg.setMaxTravelTimeBeta(15 * 60);
		drtCfg.setStopDuration(30);
		drtCfg.setVehiclesFile(inputDir + "drt\\drtVehicles.xml");

		boolean rebalancing = true;
		if (rebalancing == true) {

			// Every x-seconds the simulation calls a re-balancing process.
			// Re-balancing has the task to move vehicles into cells or zones that fits
			// typically with the demand situation
			// The technically used re-balancing strategy is then installed/binded within
			// the initialized controler
			System.out.println("Rebalancing Online");

			MinCostFlowRebalancingParams rebalancingParams = new MinCostFlowRebalancingParams();

			rebalancingParams.setInterval(300);
			rebalancingParams.setCellSize(1000);
			rebalancingParams.setTargetAlpha(0.8);
			rebalancingParams.setTargetBeta(1.5);
			rebalancingParams.setMaxTimeBeforeIdle(300);
			rebalancingParams.setMinServiceTime(3600);
			drtCfg.addParameterSet(rebalancingParams);

		}
		multiModeDrtConfigGroup.addParameterSet(drtCfg);

		// StrategyConfigGroup.StrategySettings changeServiceOperator = new
		// StrategyConfigGroup.StrategySettings();
		// changeServiceOperator.setStrategyName(ChangeDeliveryServiceOperator.SELECTOR_NAME);
		// changeServiceOperator.setWeight(0.5);
		// config.strategy().addStrategySettings(changeServiceOperator);

		// Config for StayHome Act
		PlanCalcScoreConfigGroup.ModeParams scoreParams = new PlanCalcScoreConfigGroup.ModeParams(
				"preventedShoppingTrip");
		config.planCalcScore().addModeParams(scoreParams);

		PlansCalcRouteConfigGroup.ModeRoutingParams params = new PlansCalcRouteConfigGroup.ModeRoutingParams();
		params.setMode("preventedShoppingTrip");
		params.setTeleportedModeFreespeedLimit(100000d);
		params.setTeleportedModeSpeed(100000d);
		params.setBeelineDistanceFactor(1.3);
		config.plansCalcRoute().addModeRoutingParams(params);

		config.planCalcScore().addModeParams(scoreParams);

		config.controler().setLastIteration(5);
		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
		ScenarioUtils.loadScenario(scenario);

		DrtConfigs.adjustMultiModeDrtConfig(MultiModeDrtConfigGroup.get(config), config.planCalcScore());
		FreightUtils.loadCarriersAccordingToFreightConfig(scenario);
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

		controler.addOverridingModule(new CommercialTrafficModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(config)));
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
