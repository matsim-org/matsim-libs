
/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package vwExamples.Zim;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.edrt.optimizer.EDrtVehicleDataEntryFactory.EDrtVehicleDataEntryFactoryProvider;
import org.matsim.contrib.ev.charging.ChargeUpToMaxSocStrategy;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.contrib.ev.charging.FastThenSlowCharging;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.temperature.TemperatureChangeModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import electric.edrt.energyconsumption.VwAVAuxEnergyConsumptionWithTemperatures;
import electric.edrt.energyconsumption.VwDrtDriveEnergyConsumption;
import vwExamples.utils.CreateEDRTVehiclesAndChargers;
import vwExamples.utils.customEV.BatteryReplacementCharging;
import vwExamples.utils.customEdrtModule.CustomEDrtControlerCreator;

/**
 * @author axer
 */

public class Sim02_DrtCommuter {

	public static final double MAX_RELATIVE_SOC = 0.8;// charge up to 80% SOC
	public static final double MIN_RELATIVE_SOC = 0.1;// send to chargers vehicles below 20% SOC
	public static final double CHARGING_SPEED_FACTOR = 1.0;
	public static final double BATTERYREPLACETIME = 180.0;

	static boolean BatteryReplace = false;

	static int[] fleetRange = { 300 };
	// static int[] fleetRange = {50,60,70};

	public static void main(String[] args) throws IOException {
		// int count = 7;
		int n_iterations = 1;
		for (int it = 0; it < n_iterations; it++) {
			for (int fleet : fleetRange) {

				run(fleet, it);
			}

		}

	}

	public static void run(int fleet, int iterationIdx) throws IOException {

		// Enable or Disable rebalancing
		String runId = "VW243_CityCommuterDRTAmpel2.0_10pct" + fleet + "_veh_idx" + iterationIdx;
		boolean rebalancing = true;

		String inbase = "D:\\Matsim\\Axer\\Hannover\\ZIM\\";

		// With EV
		//		final Config config = ConfigUtils.loadConfig(inbase + "\\input\\Sim02_CommuterDRT.xml", new MultiModeDrtConfigGroup(),
//				new DvrpConfigGroup(), new OTFVisConfigGroup(), new EvConfigGroup(),
//				new TemperatureChangeConfigGroup());
		//
		// Without EV
		 final Config config = ConfigUtils.loadConfig(inbase + "\\input\\Sim02_CommuterDRT.xml",
				 new MultiModeDrtConfigGroup(),
		 new DvrpConfigGroup(), new OTFVisConfigGroup());

		// With EV
//		TemperatureChangeConfigGroup tcg = (TemperatureChangeConfigGroup) config.getModules()
//				.get(TemperatureChangeConfigGroup.GROUP_NAME);
		//		tcg.setTemperatureChangeFile(inbase + "\\input\\temp\\temperatures_0.csv");

		config.travelTimeCalculator().setTraveltimeBinSize(900);
		Set<String> modes = new HashSet<String>();
		modes.add("car");
		modes.add("drt");
		config.travelTimeCalculator().setAnalyzedModes(modes);

		// config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		// Overwrite existing configuration parameters
		config.plans().setInputFile(
				inbase + "\\input\\plans\\w243_inOutWithDRT_selected.xml.gz");
		config.controler().setLastIteration(6); // Number of simulation iterations
		config.controler().setWriteEventsInterval(2); // Write Events file every x-Iterations
		config.controler().setWritePlansInterval(2); // Write Plan file every x-Iterations
		config.qsim().setStartTime(0);
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		 config.qsim().setFlowCapFactor(0.1);
		 config.qsim().setStorageCapFactor(0.11);

		String networkFilePath = inbase + "\\input\\network\\network_intersectionLinks_1.28_.xml.gz";
		String shapeFilePath = inbase + "\\input\\shp\\Real_Region_Hannover.shp";
		String shapeFeature = "NO"; // shapeFeature is used to read the shapeFilePath. All zones in shapeFile are
									// used to generate a drt service area
		String drtTag = "drt"; // drtTag is assigned to roads that should be used by the drt service
		// Adding drtTag to the network in order to define a service area
		vwExamples.utils.serviceAreaShapeToNetwork.run(networkFilePath, shapeFilePath, shapeFeature, drtTag);

		config.network().setInputFile(inbase + "\\input\\network\\drtServiceAreaNetwork.xml.gz");

		// This part allows to change dynamically DRT config parameters
		DrtConfigGroup drt = DrtConfigGroup.getSingleModeDrtConfig(config);

		drt.setPrintDetailedWarnings(false);
		// Parameters to setup the DRT service
		drt.setMaxTravelTimeBeta(900.0);
		drt.setMaxTravelTimeAlpha(1.4);
		drt.setMaxWaitTime(900.0);
		drt.setStopDuration(105);
		drt.setRejectRequestIfMaxWaitOrTravelTimeViolated(true);

		// Create the virtual stops for the drt service
		// VirtualStops are dynamically generated
		vwExamples.utils.CreateStopsFromGrid.run(config.network().getInputFile(), 400.0, drtTag);
		drt.setTransitStopFile(inbase + "\\input\\network\\virtualStops.xml");
		drt.setMaxWalkDistance(800.0);

		config.controler().setRunId(runId);

		config.controler().setOutputDirectory(inbase + "\\output\\" + runId); // Define dynamically the the
		// output path

		// For each demand scenario we are using a predefined drt vehicle fleet size
		// int fleetSize = 50;
		// CreatePeopleMoverVehicles.run(inbase+"/network/drtServiceAreaNetwork.xml.gz",
		// fleetSize, drtTag);

		// Define infrastructure for eDRT (vehicles, depots and chargers)
		CreateEDRTVehiclesAndChargers vehiclesAndChargers = new CreateEDRTVehiclesAndChargers();
		Map<Id<Link>, Integer> depotsAndVehicles = new HashMap<>();
		// depotsAndVehicles.put(Id.createLinkId(314700), vehiclePerDepot); // H HBF
		// depotsAndVehicles.put(Id.createLinkId(108636), vehiclePerDepot); // VWN
		// depotsAndVehicles.put(Id.createLinkId(162930), vehiclePerDepot); // Linden
		// Süd
		// depotsAndVehicles.put(Id.createLinkId(123095), vehiclePerDepot); // Wülferode

		// Periphery Hubs
		depotsAndVehicles.put(Id.createLinkId(96245), (int) (fleet * 0.108328079325959)); // 1
		depotsAndVehicles.put(Id.createLinkId(105093), (int) (fleet * 0.0393763970883246)); // 2
		depotsAndVehicles.put(Id.createLinkId(315038), (int) (fleet * 0.160887258554479)); // 3
		depotsAndVehicles.put(Id.createLinkId(18783), (int) (fleet * 0.0829942110391471)); // 4
		depotsAndVehicles.put(Id.createLinkId(63049), (int) (fleet * 0.0884965896715768)); // 5
		depotsAndVehicles.put(Id.createLinkId(164000), (int) (fleet * 0.0262509313922164)); // 6
		depotsAndVehicles.put(Id.createLinkId(167625), (int) (fleet * 0.0632773542729409)); // 7
		depotsAndVehicles.put(Id.createLinkId(171996), (int) (fleet * 0.0556542672092623)); // 8
		depotsAndVehicles.put(Id.createLinkId(19414), (int) (fleet * 0.0195449074339428)); // 9
		depotsAndVehicles.put(Id.createLinkId(353160), (int) (fleet * 0.0592652031867943)); // 10
		depotsAndVehicles.put(Id.createLinkId(293914), (int) (fleet * 0.0717601879979366)); // 11
		depotsAndVehicles.put(Id.createLinkId(336279), (int) (fleet * 0.0166790852295524)); // 12
		depotsAndVehicles.put(Id.createLinkId(66854), (int) (fleet * 0.0597810511835846)); // 13
		depotsAndVehicles.put(Id.createLinkId(191018), (int) (fleet * 0.0523872298962572)); // 14
		depotsAndVehicles.put(Id.createLinkId(133782), (int) (fleet * 0.0406946753023442)); // 15
		depotsAndVehicles.put(Id.createLinkId(261435), (int) (fleet * 0.02458875451367)); // 16
		depotsAndVehicles.put(Id.createLinkId(325814), (int) (fleet * 0.0300338167020118)); // 17

//		// City Hubs (these hubs are empty at the beginning of the simulation)
//		depotsAndVehicles.put(Id.createLinkId(93695), 1); // 1
		int cityFleet = 100;
		depotsAndVehicles.put(Id.createLinkId(181441), (int) (cityFleet*0.20)); // 2
		depotsAndVehicles.put(Id.createLinkId(108498), (int) (cityFleet*0.20)); // 3
		depotsAndVehicles.put(Id.createLinkId(279990), (int) (cityFleet*0.20)); // 4
//		depotsAndVehicles.put(Id.createLinkId(150245), 1); // 5
//		depotsAndVehicles.put(Id.createLinkId(25519), 1); // 6
//		depotsAndVehicles.put(Id.createLinkId(95881), 1); // 7
//		depotsAndVehicles.put(Id.createLinkId(254323), 1); // 8
		depotsAndVehicles.put(Id.createLinkId(167788), (int) (cityFleet*0.2)); // 9
//		depotsAndVehicles.put(Id.createLinkId(335414), 1); // 10
//		depotsAndVehicles.put(Id.createLinkId(105449), 1); // 11
//		depotsAndVehicles.put(Id.createLinkId(317396), 1); // 12
//		// 13 --> central hub deleted in order to avoid traffic problems due to traffic
//		// concentration
//		depotsAndVehicles.put(Id.createLinkId(337846), 1); // 14
//		depotsAndVehicles.put(Id.createLinkId(319495), 1); // 15
//		depotsAndVehicles.put(Id.createLinkId(93365), 1); // 16
		depotsAndVehicles.put(Id.createLinkId(137655), (int) (cityFleet*0.20)); // 17
//		depotsAndVehicles.put(Id.createLinkId(55525), 1); // 18
//		depotsAndVehicles.put(Id.createLinkId(166126), 1); // 19

		// depotsAndVehicles.put(Id.createLinkId(90785), vehiclePerDepot); //
		// depotsAndVehicles.put(Id.createLinkId(119060), vehiclePerDepot); //
		// depotsAndVehicles.put(Id.createLinkId(111476), vehiclePerDepot); //
		// depotsAndVehicles.put(Id.createLinkId(202515), vehiclePerDepot); //
		// depotsAndVehicles.put(Id.createLinkId(303512), vehiclePerDepot); //
		// depotsAndVehicles.put(Id.createLinkId(239977), vehiclePerDepot); //
		// depotsAndVehicles.put(Id.createLinkId(361079), vehiclePerDepot); //

		vehiclesAndChargers.CHARGER_FILE = inbase + "\\input\\chargers\\chargers.xml.gz";
		vehiclesAndChargers.NETWORKFILE = inbase + "\\input\\network\\drtServiceAreaNetwork.xml.gz";
		vehiclesAndChargers.DRT_VEHICLE_FILE = inbase + "\\input\\fleets\\fleet.xml.gz";
		vehiclesAndChargers.E_VEHICLE_FILE = inbase + "\\input\\fleets\\eFleet.xml.gz";
		vehiclesAndChargers.drtTag = drtTag;
		vehiclesAndChargers.SEATS = 6;
		vehiclesAndChargers.MAX_START_CAPACITY_KWH = 78;
		vehiclesAndChargers.MIN_START_CAPACITY_KWH = 78;
		vehiclesAndChargers.BATTERY_CAPACITY_KWH = 78;
		vehiclesAndChargers.CHARGINGPOWER_KW = (int) (100);
		vehiclesAndChargers.FRACTION_OF_CHARGERS_PER_DEPOT = 0.2;
		vehiclesAndChargers.CHAGERSPERDEPOT = 35;
		vehiclesAndChargers.run(depotsAndVehicles);

		drt.setVehiclesFile(inbase + "\\input\\fleets\\fleet.xml.gz");
		drt.setIdleVehiclesReturnToDepots(false);
		drt.setOperationalScheme(DrtConfigGroup.OperationalScheme.stopbased);
		drt.setPlotDetailedCustomerStats(true);

//		 EvConfigGroup eDrt = (EvConfigGroup)
//		 config.getModules().get(EvConfigGroup.GROUP_NAME);
//		 eDrt.setChargersFile(inbase + "\\input\\chargers\\chargers.xml.gz");
//		 eDrt.setVehiclesFile(inbase + "\\input\\fleets\\eFleet.xml.gz");
//		 eDrt.setAuxDischargeTimeStep(10);
//		 eDrt.setAuxDischargingSimulation(EvConfigGroup.AuxDischargingSimulation.seperateAuxDischargingHandler);
//		 eDrt.setTimeProfiles(true);

		// config.addModule(new ParkingRouterConfigGroup());
		// ParkingRouterConfigGroup prc = ParkingRouterConfigGroup.get(config);
		// String shapeFile = inbase + "shp\\parking-zones.shp";
		// prc.setShapeFile(shapeFile);
		// prc.setCapacityCalculationMethod("useFromNetwork");
		// prc.setShape_key("NO");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// Scale PT Network Capacities
		adjustPtNetworkCapacity(scenario.getNetwork(), config.qsim().getFlowCapFactor());

		// Filter Links with higher speeds than x km/h
		// setXY2Links(scenario, 80 / 3.6);

		// Define the MATSim Controler
		// Based on the prepared configuration this part creates a controller that runs
		Controler controler = createDRTControler(config, false);

//		 Controler controler = createControler(config);

		if (rebalancing == true) {

			// Every x-seconds the simulation calls a re-balancing process.
			// Re-balancing has the task to move vehicles into cells or zones that fits
			// typically with the demand situation
			// The technically used re-balancing strategy is then installed/binded within
			// the initialized controler
			System.out.println("Rebalancing Online");

			MinCostFlowRebalancingParams rebalancingParams = new MinCostFlowRebalancingParams();

			// rebalancingParams.setInterval(300);
			// rebalancingParams.setCellSize(1000);
			// rebalancingParams.setTargetAlpha(0.3);
			// rebalancingParams.setTargetBeta(0.3);
			// rebalancingParams.setMaxTimeBeforeIdle(500);
			// rebalancingParams.setMinServiceTime(3600);
			// drt.addParameterSet(rebalancingParams);

			rebalancingParams.setInterval(1800);
			rebalancingParams.setCellSize(2000);
			rebalancingParams.setTargetAlpha(0.20);
			rebalancingParams.setTargetBeta(0.3);
			rebalancingParams.setMaxTimeBeforeIdle(900);
			rebalancingParams.setMinServiceTime(3600);
			drt.addParameterSet(rebalancingParams);

		}

		// Change the routing module in this way, that agents are forced to go to their
		// closest bus stop.
		// If we would remove this part, agents are searching a bus stop which lies in
		// the direction of their destination but is maybe far away.
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// addRoutingModuleBinding(DvrpConfigGroup.get(config).getMode())
				// .to(ClosestStopBasedDrtRoutingModule.class);
				// Link travel times are iterativly updated between iteration
				// tt[i] = alpha * experiencedTT + (1 - alpha) * oldEstimatedTT;
				// Remark: Small alpha leads to more smoothing and longer lags in reaction.
				// Default alpha is 0.05. Which means i.e. 0.3 is not smooth in comparison to
				// 0.05
				DvrpConfigGroup.get(config).setTravelTimeEstimationAlpha(0.05);
				DvrpConfigGroup.get(config).setTravelTimeEstimationBeta(900);
				// DvrpConfigGroup.get(config).setTravelTimeEstimationAlpha(0.05);
				// DvrpConfigGroup.get(config).setTravelTimeEstimationBeta(3600*24);
				// bind(RelocationWriter.class).asEagerSingleton();
				// addControlerListenerBinding().to(RelocationWriter.class);

			}
		});

		// controler.addOverridingModule(new ParkingRouterModule());
		controler.addOverridingModule(new SwissRailRaptorModule());
		// controler.addOverridingModule(new MyDrtTrajectoryAnalysisModule(drt));

		// We finally run the controller to start MATSim

		boolean deleteRoutes = false;

		if (deleteRoutes) {
			controler.getScenario().getPopulation().getPersons().values().stream().flatMap(p -> p.getPlans().stream())
					.flatMap(pl -> pl.getPlanElements().stream()).filter(Leg.class::isInstance)
					.forEach(pe -> ((Leg) pe).setRoute(null));
		}

		controler.run();

		// }
	}

	public static void adjustPtNetworkCapacity(Network network, double flowCapacityFactor) {
		if (flowCapacityFactor < 1.0) {
			for (Link l : network.getLinks().values()) {
				if (l.getAllowedModes().contains(TransportMode.pt)) {
					l.setCapacity(l.getCapacity() / flowCapacityFactor);
				}
			}
		}
	}

	public static void setXY2Links(Scenario scenario, double maxspeed) {
		Network network = NetworkUtils.createNetwork();
		NetworkFilterManager networkFilterManager = new NetworkFilterManager(scenario.getNetwork());
		networkFilterManager.addLinkFilter(new NetworkLinkFilter() {
			@Override
			public boolean judgeLink(Link l) {
				if (l.getFreespeed() > maxspeed && l.getNumberOfLanes() > 1) {
					return false;
				} else
					return true;
			}
		});
		network = networkFilterManager.applyFilters();
		XY2Links xy2Links = new XY2Links(network, null);
		for (Person p : scenario.getPopulation().getPersons().values()) {
			xy2Links.run(p);
		}

	}

	// public static Controler createControler(Config config) {
	// Controler controler = CustomEDrtControlerCreator.createControler(config,
	// false);
	// controler.addOverridingModule(new TemperatureChangeModule());
	//
	//
	// controler.addOverridingModule(createEvDvrpIntegrationModule(DrtConfigGroup.get(config)));
	// controler.addOverridingModule(new AbstractModule() {
	// @Override
	// public void install() {
	// bind(EDrtVehicleDataEntryFactoryProvider.class)
	// .toInstance(new EDrtVehicleDataEntryFactoryProvider(MIN_RELATIVE_SOC));
	// bind(DriveEnergyConsumption.Factory.class)
	// .toInstance(evconsumption -> new VwDrtDriveEnergyConsumption());
	// bind(AuxEnergyConsumption.Factory.class)
	// .to(VwAVAuxEnergyConsumptionWithTemperatures.VwAuxFactory.class);
	//
	// if (BatteryReplace) {
	// bind(ChargingLogic.Factory.class)
	// .toInstance(charger -> new ChargingWithQueueingAndAssignmentLogic(charger,
	// new BatteryReplacementCharge(BATTERYREPLACETIME)));
	// bind(VehicleAtChargerLinkTracker.class).asEagerSingleton();
	// } else {
	// bind(ChargingLogic.Factory.class)
	// .toInstance(charger -> new ChargingWithQueueingAndAssignmentLogic(charger,
	// new CustomFastThenSlowCharging(charger.getPower(), MAX_RELATIVE_SOC)));
	// bind(VehicleAtChargerLinkTracker.class).asEagerSingleton();
	// }
	// }
	// });
	//
	// return controler;
	// }

	public static Controler createDRTControler(Config config, boolean otfvis) {
		return DrtControlerCreator.createControlerWithSingleModeDrt(config, otfvis);
	}

	public static Controler createControler(Config config) {
		Controler controler = CustomEDrtControlerCreator.createControler(config, false);
		controler.addOverridingModule(new TemperatureChangeModule());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(EDrtVehicleDataEntryFactoryProvider.class).toInstance(
						new EDrtVehicleDataEntryFactoryProvider(MIN_RELATIVE_SOC));
				bind(DriveEnergyConsumption.Factory.class).toInstance(
						evconsumption -> new VwDrtDriveEnergyConsumption());
				bind(AuxEnergyConsumption.Factory.class).to(
						VwAVAuxEnergyConsumptionWithTemperatures.VwAuxFactory.class);

				if (BatteryReplace) {
					bind(ChargingLogic.Factory.class).toProvider(
							new ChargingWithQueueingAndAssignmentLogic.FactoryProvider(
									charger -> new BatteryReplacementCharging.Strategy(charger,
											new ChargeUpToMaxSocStrategy(charger, MAX_RELATIVE_SOC))));
					bind(ChargingPower.Factory.class).toInstance(
							ev -> new BatteryReplacementCharging(ev, BATTERYREPLACETIME));
				} else {
					bind(ChargingLogic.Factory.class).toProvider(
							new ChargingWithQueueingAndAssignmentLogic.FactoryProvider(
									charger -> new ChargeUpToMaxSocStrategy(charger, MAX_RELATIVE_SOC)));
					bind(ChargingPower.Factory.class).toInstance(FastThenSlowCharging::new);
				}

				//				bind(ChargingLogic.Factory.class).toInstance(charger -> new ChargingWithQueueingAndAssignmentLogic(charger, new FastThenSlowCharging(charger.getPower())));
				//				//bind(ChargingLogic.Factory.class).toInstance(charger -> new ChargingWithQueueingAndAssignmentLogic(charger, new BatteryReplacementCharging(240.0)));
			}
		});

		return controler;
	}

	// public static EvDvrpIntegrationModule
	// createEvDvrpIntegrationModule(DrtConfigGroup drtCfg) {
	// return new EvDvrpIntegrationModule(drtCfg.getMode())
	// .setTurnedOnPredicate(RunDrtScenarioBatchH_eDRT_KGERAK::isTurnedOn);
	// }

}
