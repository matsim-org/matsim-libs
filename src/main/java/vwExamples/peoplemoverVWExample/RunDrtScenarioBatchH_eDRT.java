
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

package vwExamples.peoplemoverVWExample;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.temperature.TemperatureChangeConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import vwExamples.utils.CreateEDRTVehiclesAndChargers;

//import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

/** * @author axer */

public class RunDrtScenarioBatchH_eDRT {

	public static Controler createControler(Config config, boolean otfvis) {
		return DrtControlerCreator.createControlerWithSingleModeDrt(config, otfvis);
	}

	public static void main(String[] args) throws IOException {

		boolean enableCadyts = false;

		// Enable or Disable rebalancing
		String runId = "Moia_2";
		boolean rebalancing = true;

		String inbase = "D:\\Thiel\\Programme\\MatSim\\03_HannoverDRT\\input\\";
		String outbase = "D:\\Thiel\\Programme\\MatSim\\03_HannoverDRT\\output\\";

		final Config config = ConfigUtils.loadConfig(inbase + "config_H_DRT_1.0_0.25DRT.xml", new DrtConfigGroup(),
				new DvrpConfigGroup(), new OTFVisConfigGroup(), new EvConfigGroup(), new TemperatureChangeConfigGroup(),
				new CadytsConfigGroup());

		TemperatureChangeConfigGroup tcg = (TemperatureChangeConfigGroup) config.getModules()
				.get(TemperatureChangeConfigGroup.GROUP_NAME);
		tcg.setTemperatureChangeFile(inbase + "\\temp\\temperatures.csv");

		// config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		// Overwrite existing configuration parameters
		config.plans().setInputFile(inbase + "\\plans\\Moia_2.20.plans.xml.gz");
		config.controler().setLastIteration(140); // Number of simulation iterations
		config.controler().setWriteEventsInterval(20); // Write Events file every x-Iterations
		config.controler().setWritePlansInterval(20); // Write Plan file every x-Iterations
		config.qsim().setStartTime(0);

		String networkFilePath = inbase + "network\\network.xml.gz";
		String shapeFilePath = inbase + "\\shp\\Hannover_Service_Area.shp";
		String shapeFeature = "NO"; // shapeFeature is used to read the shapeFilePath. All zones in shapeFile are
									// used to generate a drt service area
		String drtTag = "drt"; // drtTag is assigned to roads that should be used by the drt service
		// Adding drtTag to the network in order to define a service area
		vwExamples.utils.serviceAreaShapeToNetwork.run(networkFilePath, shapeFilePath, shapeFeature, drtTag);
		//
		// CreateParkingNetwork createParkingNetwork = new CreateParkingNetwork();
		// createParkingNetwork.customCapacityLinks.put("vw11", 5100.0);
		// createParkingNetwork.customCapacityLinks.put("vw14", 3000.0);
		// createParkingNetwork.customCapacityLinks.put("vwno", 3600.0);
		// createParkingNetwork.customCapacityLinks.put("vw7", 4400.0);
		// createParkingNetwork.customCapacityLinks.put("vw24", 3800.0);
		// createParkingNetwork.customCapacityLinks.put("vw222", 3800.0);
		// createParkingNetwork.customCapacityLinks.put("vw2", 3200.0);
		//
		// createParkingNetwork.run(inbase+"\\shp\\parkinglocations.csv",
		// inbase+"\\network\\drtServiceAreaNetwork.xml.gz",
		// inbase+"\\network\\drtServiceAreaNetwork_withPark.xml.gz");

		config.network().setInputFile(inbase + "\\network\\drtServiceAreaNetwork.xml.gz");
		
		// This part allows to change dynamically DRT config parameters
		DrtConfigGroup drt = (DrtConfigGroup) config.getModules().get(DrtConfigGroup.GROUP_NAME);

		drt.setPrintDetailedWarnings(false);
		// Parameters to setup the DRT service
		drt.setMaxTravelTimeBeta(300.0);
		drt.setMaxTravelTimeAlpha(1.3);
		drt.setMaxWaitTime(300.0);
		drt.setStopDuration(30.0);

		// Create the virtual stops for the drt service
		// VirtualStops are dynamically generated
		vwExamples.utils.CreateStopsFromGrid.run(config.network().getInputFile(), 400.0, drtTag);
		drt.setTransitStopFile(inbase + "\\network\\virtualStops.xml");
		drt.setMaxWalkDistance(800.0);

		config.controler().setRunId(runId);
		// 10 percent Scenario
		// config.qsim().setFlowCapFactor(0.12);
		// config.qsim().setStorageCapFactor(0.24);

		// config.qsim().setFlowCapFactor(0.85);
		// config.qsim().setStorageCapFactor(1.00);

		config.controler().setOutputDirectory(outbase + runId); // Define dynamically the the
																				// output path

		// For each demand scenario we are using a predefined drt vehicle fleet size
		// int fleetSize = 50;
		// CreatePeopleMoverVehicles.run(inbase+"/network/drtServiceAreaNetwork.xml.gz",
		// fleetSize, drtTag);

		// Define infrastructure for eDRT (vehicles, depots and chargers)
		CreateEDRTVehiclesAndChargers vehiclesAndChargers = new CreateEDRTVehiclesAndChargers();
		Map<Id<Link>, Integer> depotsAndVehicles = new HashMap<>();
//		depotsAndVehicles.put(Id.createLinkId(314700), 35); // H HBF
//		depotsAndVehicles.put(Id.createLinkId(108636), 40); // VWN
//		depotsAndVehicles.put(Id.createLinkId(162930), 35); // Linden Süd
//		depotsAndVehicles.put(Id.createLinkId(123095), 40); // Wülferode
		
		depotsAndVehicles.put(Id.createLinkId(186867), 75); // Podbielskistraße 306
		depotsAndVehicles.put(Id.createLinkId(99346), 150); //  Am Mittelfelde 25


		vehiclesAndChargers.CHARGER_FILE = inbase + "\\chargers\\chargers.xml.gz";
		vehiclesAndChargers.NETWORKFILE = inbase + "\\network\\drtServiceAreaNetwork.xml.gz";
		vehiclesAndChargers.DRT_VEHICLE_FILE = inbase + "\\fleets\\fleet.xml.gz";
		vehiclesAndChargers.E_VEHICLE_FILE = inbase + "\\fleets\\eFleet.xml.gz";
		vehiclesAndChargers.drtTag = drtTag;
		vehiclesAndChargers.SEATS = 6;
		vehiclesAndChargers.MAX_START_CAPACITY_KWH = 78;
		vehiclesAndChargers.MIN_START_CAPACITY_KWH = 78;
		vehiclesAndChargers.BATTERY_CAPACITY_KWH = 78;
		vehiclesAndChargers.CHARGINGPOWER_KW = (int) (125*0.85);
		vehiclesAndChargers.FRACTION_OF_CHARGERS_PER_DEPOT = 1.0;
		vehiclesAndChargers.run(depotsAndVehicles);

		drt.setVehiclesFile(inbase + "\\fleets\\fleet.xml.gz");
		drt.setIdleVehiclesReturnToDepots(true);
		drt.setOperationalScheme(DrtConfigGroup.OperationalScheme.stopbased);

		EvConfigGroup eDrt = (EvConfigGroup) config.getModules().get(EvConfigGroup.GROUP_NAME);
		eDrt.setChargersFile(inbase + "\\chargers\\chargers.xml.gz");
		eDrt.setVehiclesFile(inbase + "\\fleets\\eFleet.xml.gz");
		eDrt.setAuxDischargeTimeStep(10);
		eDrt.setTimeProfiles(true);

		// config.addModule(new ParkingRouterConfigGroup());
		// ParkingRouterConfigGroup prc = ParkingRouterConfigGroup.get(config);
		// String shapeFile = inbase+"shp\\parking-zones.shp";
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
		Controler controler = createControler(config, false);

		//Controler controler = RunVWEDrtScenario.createControler(config);

		if (enableCadyts)
		{
			controler.addOverridingModule(new CadytsCarModule());
		}
		
		
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
			rebalancingParams.setTargetBeta(0.8);
			rebalancingParams.setMaxTimeBeforeIdle(300);
			rebalancingParams.setMinServiceTime(3600);
			drt.addParameterSet(rebalancingParams);

		}

		if (enableCadyts)

		{
			// include cadyts into the plan scoring (this will add the cadyts corrections to
			// the scores):
			controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
				private final ScoringParametersForPerson parameters = new SubpopulationScoringParameters(scenario);
				@Inject
				CadytsContext cContext;

				@Override
				public ScoringFunction createNewScoringFunction(Person person) {

					final ScoringParameters params = parameters.getScoringParameters(person);

					SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
					scoringFunctionAccumulator.addScoringFunction(
							new CharyparNagelLegScoring(params, controler.getScenario().getNetwork(), scenario.getConfig().transit().getTransitModes()));
					scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params));
					scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

					final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config,
							cContext);
					final double cadytsScoringWeight = 20. * config.planCalcScore().getBrainExpBeta();
					scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight);
					scoringFunctionAccumulator.addScoringFunction(scoringFunction);

					return scoringFunctionAccumulator;
				}
			});
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
				DvrpConfigGroup.get(config).setTravelTimeEstimationAlpha(0.15);
				DvrpConfigGroup.get(config).setTravelTimeEstimationBeta(600);
				// bind(RelocationWriter.class).asEagerSingleton();
				// addControlerListenerBinding().to(RelocationWriter.class);

			}
		});
		// controler.addOverridingModule(new ParkingRouterModule());
		controler.addOverridingModule(new SwissRailRaptorModule());
		
		// tell the system to use the congested car router for the ride mode:
		controler.addOverridingModule(new AbstractModule(){
			@Override public void install() {
				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());		}
		});

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
}
