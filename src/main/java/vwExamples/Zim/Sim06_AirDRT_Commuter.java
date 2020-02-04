/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package vwExamples.Zim;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;
import org.matsim.contrib.drt.routing.MultiModeDrtMainModeIdentifier;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigGroup.OperationalScheme;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.router.DvrpRoutingModuleProvider.Stage;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

import uam.scenario.UamNetworkFleetCreatorFromCSV;

/**
 * @author Steffen Axer, based on Michal Maciejewski (michalm)
 */
public class Sim06_AirDRT_Commuter {
	public void run(String runId, String base, String configFilename, String network, String inputPlans, int fleet,
			int qsimcores, int hdlcores) throws IOException {

		// Logger.getRootLogger().setLevel(Level.DEBUG);

		String inbase = base;
		String input = inbase + "input//";
		final Config config = ConfigUtils.loadConfig(inbase + "//input//" + configFilename,
				new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup(), new CadytsConfigGroup());

		UamNetworkFleetCreatorFromCSV fleetAndNetworkCreator= new UamNetworkFleetCreatorFromCSV(input + "uam//ports.csv", input + "//network//" + network, input + "//uam");
		fleetAndNetworkCreator.weightIn=0.8;
		fleetAndNetworkCreator.weightOut=0.2;
		fleetAndNetworkCreator.HUB_LINK_NUM_LANES=200;
		fleetAndNetworkCreator.updateAirNetworkParams();
		fleetAndNetworkCreator.run(fleet);
		
		DvrpConfigGroup.get(config).setNetworkModesAsString("uam");
		MultiModeDrtConfigGroup multiModeDrtCfg = MultiModeDrtConfigGroup.get(config);

		DrtConfigGroup uamCfg = new DrtConfigGroup();
		uamCfg.setMode("uam");
		uamCfg.setMaxTravelTimeBeta(900.0);
		uamCfg.setMaxTravelTimeAlpha(1.0);
		uamCfg.setMaxWaitTime(900.0);
		uamCfg.setStopDuration(105);
		uamCfg.setRejectRequestIfMaxWaitOrTravelTimeViolated(false);
		uamCfg.setTransitStopFile(inbase + "//input//uam//uam_stops.xml");
		uamCfg.setMaxWalkDistance(20000.0);
		uamCfg.setVehiclesFile(inbase + "//input//uam//uam_fleet.xml");
		uamCfg.setUseModeFilteredSubnetwork(true);
		uamCfg.setOperationalScheme(OperationalScheme.stopbased);
		multiModeDrtCfg.addParameterSet(uamCfg);

		Map<String, DrtConfigGroup> drtCfgMap = multiModeDrtCfg.getModalElements().stream()
				.collect(Collectors.toMap(DrtConfigGroup::getMode, cfg -> cfg));

		drtCfgMap.values()
				.forEach(cfg -> DrtConfigs.adjustDrtConfig(cfg, config.planCalcScore(), config.plansCalcRoute()));

		config.plansCalcRoute().setInsertingAccessEgressWalk(true);

		config.plans().setInputFile(inbase + "//input//plans//" + inputPlans);

		config.controler().setLastIteration(1); // Number of simulation iterations

		config.transit().setTransitScheduleFile(input + "transit//vw280_0.1.output_transitSchedule.xml.gz");
		config.transit().setVehiclesFile(input + "transit//vw280_0.1.output_transitVehicles.xml.gz");

		config.qsim().setStartTime(0);
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
		// config.qsim().setFlowCapFactor(0.1);
		// config.qsim().setStorageCapFactor(0.11);

		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks);
		config.plansCalcRoute().setRoutingRandomness(3.);

		// vsp defaults
		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);
		config.qsim().setNumberOfThreads(1);

		// Network with UAM
		config.network().setInputFile(inbase + "//input//uam/network_with_uam.xml.gz");

		config.controler().setRunId(runId);

		config.controler().setOutputDirectory(inbase + "//output//" + runId); // Define dynamically the the

		if (true) {

			// Every x-seconds the simulation calls a re-balancing process.
			// Re-balancing has the task to move vehicles into cells or zones that fits
			// typically with the demand situation
			// The technically used re-balancing strategy is then installed/binded within
			// the initialized controler
			System.out.println("Rebalancing Online");

			MinCostFlowRebalancingParams rebalancingParams = new MinCostFlowRebalancingParams();

			rebalancingParams.setInterval(1800);
			rebalancingParams.setCellSize(500);
			rebalancingParams.setTargetAlpha(0.25);
			rebalancingParams.setTargetBeta(0.3);
			rebalancingParams.setMaxTimeBeforeIdle(900);
			rebalancingParams.setMinServiceTime(3600);
			uamCfg.addParameterSet(rebalancingParams);

		}

		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
		ScenarioUtils.loadScenario(scenario);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateModes(uamCfg.getMode()));

		controler.addOverridingModule(new AbstractDvrpModeModule(uamCfg.getMode()) {
			@Inject
			private MultiModeDrtConfigGroup multiModeDrtCfg;

			@Override
			public void install() {
				MapBinder<Stage, RoutingModule> mapBinder = modalMapBinder(Stage.class, RoutingModule.class);
				// DRT as access mode (fixed)
				mapBinder.addBinding(Stage.ACCESS).to(Key.get(RoutingModule.class, Names.named("car")));
				// more flexible approach
				mapBinder.addBinding(Stage.EGRESS).to(Key.get(RoutingModule.class, Names.named("car")));

				bind(MainModeIdentifier.class).toInstance(new UamMainModeIdentifier(multiModeDrtCfg));
			}
		});

		// run simulation
		controler.run();
	}

	private static class UamAccessEgressRoutingModuleProvider implements Provider<RoutingModule> {
		@Inject
		private Injector injector;

		@Override
		public RoutingModule get() {
			String mode = "car";// or a more less random choice here
			return (fromFacility, toFacility, departureTime, person) -> injector.getInstance(TripRouter.class)
					.calcRoute(mode, fromFacility, toFacility, departureTime, person);
		}
	}

	private static class UamMainModeIdentifier implements MainModeIdentifier {
		private final String mode = "uam";
		private final String drtStageActivityType = PlanCalcScoreConfigGroup.createStageActivityType(mode);
		private final MultiModeDrtMainModeIdentifier delegate;

		@Inject
		public UamMainModeIdentifier(MultiModeDrtConfigGroup drtCfg) {
			delegate = new MultiModeDrtMainModeIdentifier(drtCfg);
		}

		@Override
		public String identifyMainMode(List<? extends PlanElement> tripElements) {
			for (PlanElement pe : tripElements) {
				if (pe instanceof Activity) {
					if (((Activity)pe).getType().equals(drtStageActivityType))
						return mode;
				} else if (pe instanceof Leg) {
					if (TripRouter.isFallbackMode(((Leg)pe).getMode())) {
						return mode;
					}
				}
			}

			return delegate.identifyMainMode(tripElements);
		}
	}

	public static void main(String[] args) throws IOException {

		String runId = args[0];
		String base = args[1];
		String configFileName = args[2];
		String networkWithCapacities = args[3];
		String inputPlans = args[4];
		int fleet = Integer.parseInt(args[5]);
		int qsimcores = Integer.parseInt(args[6]);
		int hdlcores = Integer.parseInt(args[7]);
		new Sim06_AirDRT_Commuter().run(runId, base, configFileName, networkWithCapacities, inputPlans, fleet,
				qsimcores, hdlcores);
	}

	// public static void main(String[] args) {
	// Config config =
	// ConfigUtils.loadConfig("input/uam/uam_drt_config_epsilon_2000.xml",
	// new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new
	// OTFVisConfigGroup());
	// run(config, false, 0);
	// }
}
