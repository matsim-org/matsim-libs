
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
import java.util.HashSet;
import java.util.Set;

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
import vwExamples.utils.VehicleFromCSV.CreatePeoplemoverVehicleFromCSV;

/**
 * @author axer
 */

public class RunDrtScenarioBatchH_MOIA {

	static int[] fleetRange = {100,125,150,175,200,225,250};

	public static void main(String[] args) throws IOException {
		//int count = 7;
		int n_iterations = 1;
		for (int it = 0; it < n_iterations; it++) {
			for (int fleet : fleetRange ) {
				int vehicles = (int) (fleet);

				run(vehicles, it);
			}

		}

	}

	public static void run(int vehicles, int iterationIdx) throws IOException {

		// Enable or Disable rebalancing
		String runId = "MOIA_1_" + vehicles + "_veh_idx" + iterationIdx;
		boolean rebalancing = true;

		String inbase = "D:\\Matsim\\Axer\\Hannover\\MOIA\\";

		final Config config = ConfigUtils.loadConfig(inbase + "\\input\\hannover_edrt.xml",
				new MultiModeDrtConfigGroup(),
				new DvrpConfigGroup(), new OTFVisConfigGroup());
		
	
		config.travelTimeCalculator().setTraveltimeBinSize(900);
		Set<String> modes = new HashSet<String>();
		modes.add("car");
		modes.add("drt");
		config.travelTimeCalculator().setAnalyzedModes(modes);

		// config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		// Overwrite existing configuration parameters
		config.plans().setInputFile(inbase + "\\input\\plans\\plans.xml.gz");
		config.controler().setLastIteration(2); // Number of simulation iterations
		config.controler().setWriteEventsInterval(2); // Write Events file every x-Iterations
		config.controler().setWritePlansInterval(2); // Write Plan file every x-Iterations
		config.qsim().setStartTime(0);
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);

		String networkFilePath = inbase + "\\input\\network\\network.xml.gz";
		String shapeFilePath = inbase + "\\input\\shp\\Hannover_Stadtteile.shp";
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
		drt.setMaxTravelTimeBeta(600.0);
		drt.setMaxTravelTimeAlpha(1.4);
		drt.setMaxWaitTime(900.0);
		drt.setStopDuration(105.0);
		drt.setRequestRejection(true);

		// Create the virtual stops for the drt service
		// VirtualStops are dynamically generated
//		vwExamples.utils.CreateStopsFromGrid.run(config.network().getInputFile(), 400.0, drtTag);
		drt.setTransitStopFile(inbase + "\\input\\network\\virtualStops_MOIA.xml");
		drt.setMaxWalkDistance(800.0);

		config.controler().setRunId(runId);

		config.controler().setOutputDirectory(inbase + "\\output\\" + runId); // Define dynamically the the
		// output path


		CreatePeoplemoverVehicleFromCSV initalizePositions = new CreatePeoplemoverVehicleFromCSV(
				inbase + "\\input\\simulation_input_data_to_steffen\\preinitialized_hannover_vehicle_locations.csv",
				inbase + "\\input\\network\\network.xml.gz",
				inbase + "\\input\\fleets\\fleet.xml.gz");
		initalizePositions.readVehicleLocationsCSV();
		initalizePositions.createFleet(vehicles);
		
		drt.setVehiclesFile(inbase + "\\input\\fleets\\fleet.xml.gz");
		drt.setIdleVehiclesReturnToDepots(false);
		drt.setOperationalScheme(DrtConfigGroup.OperationalScheme.stopbased);
		drt.setPlotDetailedCustomerStats(true);


		Scenario scenario = ScenarioUtils.loadScenario(config);

		// Scale PT Network Capacities
		adjustPtNetworkCapacity(scenario.getNetwork(), config.qsim().getFlowCapFactor());

		// Define the MATSim Controler
		// Based on the prepared configuration this part creates a controller that runs
		Controler controler = DrtControlerCreator.createControlerWithSingleModeDrt(config, false);

		//Controler controler = createControler(config);

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
			rebalancingParams.setTargetAlpha(0.3);
			rebalancingParams.setTargetBeta(0.3);
			rebalancingParams.setMaxTimeBeforeIdle(500);
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
//				DvrpConfigGroup.get(config).setTravelTimeEstimationAlpha(0.05);
//				DvrpConfigGroup.get(config).setTravelTimeEstimationBeta(3600*24);
				// bind(RelocationWriter.class).asEagerSingleton();
				// addControlerListenerBinding().to(RelocationWriter.class);

			}
		});

		// controler.addOverridingModule(new ParkingRouterModule());
		controler.addOverridingModule(new SwissRailRaptorModule());
		//controler.addOverridingModule(new MyDrtTrajectoryAnalysisModule(drt));
		

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
