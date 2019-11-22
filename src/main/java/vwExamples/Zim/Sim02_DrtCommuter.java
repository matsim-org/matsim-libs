
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
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import vwExamples.utils.CreateEDRTVehiclesAndChargers;

/**
 * @author axer
 */

public class Sim02_DrtCommuter {

	public static void main(String[] args) throws IOException {

		String runId = args[0];
		String base = args[1];
		String configFileName = args[2];
		String networkWithCapacities = args[3];
		String inputPlans = args[4];
		int fleet = Integer.parseInt(args[5]);
		int qsimcores =Integer.parseInt(args[6]);
		int hdlcores =Integer.parseInt(args[7]);
		new Sim02_DrtCommuter().run(runId,base,configFileName,networkWithCapacities,inputPlans,fleet,qsimcores,hdlcores);
	}

	public void run(String runId,String base, String configFilename, String networkWithCapacities, String inputPlans,int fleet, int qsimcores, int hdlcores) throws IOException {

		
		// Enable or Disable rebalancing
		// String runId = "vw280_CityCommuterDRTcarOnly_20pct_1.0_" + fleet + "_veh";
		boolean rebalancing = true;

		String inbase = base;
		
		String input = inbase + "input//";

		// Create empty multiModeDrtConfigGroup
		DrtConfigGroup drtCfg = new DrtConfigGroup();
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup();
		multiModeDrtConfigGroup.addParameterSet(drtCfg);

		final Config config = ConfigUtils.loadConfig(inbase + "//input//"+configFilename, multiModeDrtConfigGroup,
				new DvrpConfigGroup(), new CadytsConfigGroup());

		
		//Disable any innovation from the beginning of the simulation
		config.strategy().clearStrategySettings();
		StrategySettings strat = new StrategySettings();

		strat.setStrategyName(DefaultSelector.KeepLastSelected.toString());
		strat.setWeight(1.0);
		config.strategy().addStrategySettings(strat);
		config.strategy().setFractionOfIterationsToDisableInnovation(0);
		
		PlanCalcScoreConfigGroup.ModeParams scoreParams =  new PlanCalcScoreConfigGroup.ModeParams("drt");
		config.planCalcScore().addModeParams(scoreParams);
		PlanCalcScoreConfigGroup.ModeParams scoreParams2 =  new PlanCalcScoreConfigGroup.ModeParams("drt_walk");
		config.planCalcScore().addModeParams(scoreParams2);
		
		config.travelTimeCalculator().setTraveltimeBinSize(900);
		Set<String> modes = new HashSet<String>();
		modes.add("car");
		modes.add("drt");
		config.travelTimeCalculator().setAnalyzedModes(modes);
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.plans().setInputFile(inbase + "//input//plans//"+inputPlans);

		config.controler().setLastIteration(6); // Number of simulation iterations
		
		config.transit().setTransitScheduleFile(input + "transit//vw280_0.1.output_transitSchedule.xml.gz");
		config.transit().setVehiclesFile(input + "transit//vw280_0.1.output_transitVehicles.xml.gz");

		config.qsim().setStartTime(0);
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		// config.qsim().setFlowCapFactor(0.1);
		// config.qsim().setStorageCapFactor(0.11);

		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks);
		config.plansCalcRoute().setRoutingRandomness(3.);

		// vsp defaults
		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);
		config.qsim().setNumberOfThreads(1);

		String networkFilePath = inbase + "//input//network//"+networkWithCapacities;
		String shapeFilePath = inbase + "//input//shp//Real_Region_Hannover.shp";
		String shapeFeature = "NO"; // shapeFeature is used to read the shapeFilePath. All zones in shapeFile are
									// used to generate a drt service area
		String drtTag = "drt"; // drtTag is assigned to roads that should be used by the drt service
		// Adding drtTag to the network in order to define a service area
		vwExamples.utils.serviceAreaShapeToNetwork.run(networkFilePath, shapeFilePath, shapeFeature, drtTag);

		config.network().setInputFile(inbase + "//input//network//drtServiceAreaNetwork.xml.gz");

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
		drt.setTransitStopFile(inbase + "//input//network//virtualStops.xml");
		drt.setMaxWalkDistance(800.0);

		config.controler().setRunId(runId);

		config.controler().setOutputDirectory(inbase + "//output//" + runId); // Define dynamically the the
		// output path

		// For each demand scenario we are using a predefined drt vehicle fleet size
		// int fleetSize = 50;
		// CreatePeopleMoverVehicles.run(inbase+"/network/drtServiceAreaNetwork.xml.gz",
		// fleetSize, drtTag);

		// Define infrastructure for eDRT (vehicles, depots and chargers)
		CreateEDRTVehiclesAndChargers vehiclesAndChargers = new CreateEDRTVehiclesAndChargers();
		Map<Id<Link>, Integer> depotsAndVehicles = new HashMap<>();


		// Periphery Hubs
		int peripheralFleet = (int) (fleet * 0.8);
		depotsAndVehicles.put(Id.createLinkId(96245), (int) (peripheralFleet * 0.108328079325959)); // 1
		depotsAndVehicles.put(Id.createLinkId(105093), (int) (peripheralFleet * 0.0393763970883246)); // 2
		depotsAndVehicles.put(Id.createLinkId(315038), (int) (peripheralFleet * 0.160887258554479)); // 3
		depotsAndVehicles.put(Id.createLinkId(18783), (int) (peripheralFleet * 0.0829942110391471)); // 4
		depotsAndVehicles.put(Id.createLinkId(63049), (int) (peripheralFleet * 0.0884965896715768)); // 5
		depotsAndVehicles.put(Id.createLinkId(164000), (int) (peripheralFleet * 0.0262509313922164)); // 6
		depotsAndVehicles.put(Id.createLinkId(167625), (int) (peripheralFleet * 0.0632773542729409)); // 7
		depotsAndVehicles.put(Id.createLinkId(171996), (int) (peripheralFleet * 0.0556542672092623)); // 8
		depotsAndVehicles.put(Id.createLinkId(19414), (int) (peripheralFleet * 0.0195449074339428)); // 9
		depotsAndVehicles.put(Id.createLinkId(353160), (int) (peripheralFleet * 0.0592652031867943)); // 10
		depotsAndVehicles.put(Id.createLinkId(293914), (int) (peripheralFleet * 0.0717601879979366)); // 11
		depotsAndVehicles.put(Id.createLinkId(336279), (int) (peripheralFleet * 0.0166790852295524)); // 12
		depotsAndVehicles.put(Id.createLinkId(66854), (int) (peripheralFleet * 0.0597810511835846)); // 13
		depotsAndVehicles.put(Id.createLinkId(191018), (int) (peripheralFleet * 0.0523872298962572)); // 14
		depotsAndVehicles.put(Id.createLinkId(133782), (int) (peripheralFleet * 0.0406946753023442)); // 15
		depotsAndVehicles.put(Id.createLinkId(261435), (int) (peripheralFleet * 0.02458875451367)); // 16
		depotsAndVehicles.put(Id.createLinkId(325814), (int) (peripheralFleet * 0.0300338167020118)); // 17

		// // City Hubs (these hubs are empty at the beginning of the simulation)

		int cityFleet = (int) (fleet * 0.2);
		depotsAndVehicles.put(Id.createLinkId(181441), (int) (cityFleet * 0.20)); // 2
		depotsAndVehicles.put(Id.createLinkId(108498), (int) (cityFleet * 0.20)); // 3
		depotsAndVehicles.put(Id.createLinkId(279990), (int) (cityFleet * 0.20)); // 4
		depotsAndVehicles.put(Id.createLinkId(167788), (int) (cityFleet * 0.20)); // 9
		depotsAndVehicles.put(Id.createLinkId(137655), (int) (cityFleet * 0.20)); // 17

		vehiclesAndChargers.CHARGER_FILE = inbase + "//input//chargers//chargers.xml.gz";
		vehiclesAndChargers.NETWORKFILE = inbase + "//input//network//drtServiceAreaNetwork.xml.gz";
		vehiclesAndChargers.DRT_VEHICLE_FILE = inbase + "//input//fleets//fleet.xml.gz";
		vehiclesAndChargers.E_VEHICLE_FILE = inbase + "//input//fleets//eFleet.xml.gz";
		vehiclesAndChargers.drtTag = drtTag;
		vehiclesAndChargers.SEATS = 6;
		vehiclesAndChargers.MAX_START_CAPACITY_KWH = 78;
		vehiclesAndChargers.MIN_START_CAPACITY_KWH = 78;
		vehiclesAndChargers.BATTERY_CAPACITY_KWH = 78;
		vehiclesAndChargers.CHARGINGPOWER_KW = (int) (100);
		vehiclesAndChargers.FRACTION_OF_CHARGERS_PER_DEPOT = 0.2;
		vehiclesAndChargers.CHAGERSPERDEPOT = 35;
		vehiclesAndChargers.run(depotsAndVehicles);

		drt.setVehiclesFile(inbase + "//input//fleets//fleet.xml.gz");
		drt.setIdleVehiclesReturnToDepots(false);
		drt.setOperationalScheme(DrtConfigGroup.OperationalScheme.stopbased);

		drt.setPlotDetailedCustomerStats(true);

		// Define the MATSim Controler
		// Based on the prepared configuration this part creates a controller that runs
		Controler controler = createDRTControler(config, false);

		// Controler controler = createControler(config);

		if (rebalancing == true) {

			// Every x-seconds the simulation calls a re-balancing process.
			// Re-balancing has the task to move vehicles into cells or zones that fits
			// typically with the demand situation
			// The technically used re-balancing strategy is then installed/binded within
			// the initialized controler
			System.out.println("Rebalancing Online");

			MinCostFlowRebalancingParams rebalancingParams = new MinCostFlowRebalancingParams();

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

				DvrpConfigGroup.get(config).setTravelTimeEstimationAlpha(0.05);
				DvrpConfigGroup.get(config).setTravelTimeEstimationBeta(900);


			}
		});

		// We finally run the controller to start MATSim

		boolean deleteRoutes = false;

		if (deleteRoutes) {
			controler.getScenario().getPopulation().getPersons().values().stream().flatMap(p -> p.getPlans().stream())
					.flatMap(pl -> pl.getPlanElements().stream()).filter(Leg.class::isInstance)
					.forEach(pe -> ((Leg) pe).setRoute(null));
		}

		adjustPtNetworkCapacity(controler.getScenario().getNetwork(), config.qsim().getFlowCapFactor());
		controler.addOverridingModule(new SwissRailRaptorModule());
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

	public static Controler createDRTControler(Config config, boolean otfvis) {
		return DrtControlerCreator.createControlerWithSingleModeDrt(config, otfvis);
	}

}
