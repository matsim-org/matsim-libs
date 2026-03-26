/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.jsprit;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Solutions;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controller.CarrierModule;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.testcases.MatsimTestUtils;

public class IntegrationIT {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testJsprit() throws ExecutionException, InterruptedException, IOException {
		final String networkFilename = utils.getClassInputDirectory() + "/merged-network-simplified.xml.gz";
		final String vehicleTypeFilename = Path.of(utils.getPackageInputDirectory()).getParent().resolve("vehicleTypes_v2.xml").toString();
		final String carrierFilename = utils.getClassInputDirectory() + "/carrier.xml";

		Config config = ConfigUtils.createConfig();
		config.global().setRandomSeed(4177);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setCarriersFile(carrierFilename);
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(vehicleTypeFilename);
		freightCarriersConfigGroup.setTravelTimeSliceWidth(24*3600);
		freightCarriersConfigGroup.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.enforceBeginnings);

		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);

		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
		Controller controller = ControllerUtils.createController(scenario);
		controller.addOverridingModule(new CarrierModule());
		controller.getInjector();
		for (Carrier carrier : CarriersUtils.getCarriers(scenario).getCarriers().values()) {
			CarriersUtils.setJspritIterations(carrier, 1);
		}

		CarriersUtils.runJsprit(scenario);

		controller.run();

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "freight");
		Assertions.assertTrue(Files.deleteIfExists(out.resolve("VRP_Solution_Stats.csv")));
		Assertions.assertTrue(Files.deleteIfExists(out.resolve("VRP_Solution_Stats_perCarrier.csv")));
		Assertions.assertTrue(Files.deleteIfExists(out.resolve("VRP_Solution_Stats.png")));

		double scoreWithRunJsprit = 0;
		for (Carrier carrier : CarriersUtils.getCarriers(scenario).getCarriers().values()) {
			scoreWithRunJsprit = scoreWithRunJsprit + carrier.getSelectedPlan().getJspritScore();
		}
		double scoreRunWithOldStructure = generateCarrierPlans(scenario.getNetwork(), CarriersUtils.getCarriers(scenario), CarriersUtils.getOrAddCarrierVehicleTypes(scenario));
		Assertions.assertEquals(scoreWithRunJsprit, scoreRunWithOldStructure, MatsimTestUtils.EPSILON, "The score of both runs are not the same");

		for (Carrier carrier : CarriersUtils.getCarriers(scenario).getCarriers().values()) {
			CarriersUtils.setJspritIterations(carrier, 20);
		}
		CarriersUtils.runJsprit(scenario, CarriersUtils.CarrierSelectionForSolution.solveForAllCarriersAndAddPLans);
		for (Carrier carrier : CarriersUtils.getCarriers(scenario).getCarriers().values()) {
			Assertions.assertEquals(2, carrier.getPlans().size(), "The number of plans is not as expected");
			// Test method if all jobs are handled
			Assertions.assertTrue(CarriersUtils.allJobsHandledBySelectedPlan(carrier), "Not all jobs are handled");
			CarrierService newService  = CarrierService.Builder.newInstance(Id.create("service" + carrier.getServices().size(), CarrierService.class), Id.createLinkId("100603"),0)
				.setServiceDuration(10.)
				.setServiceStartingTimeWindow(TimeWindow.newInstance(0, 86000))
				.build();
			CarriersUtils.addService(carrier, newService);
			Assertions.assertFalse(CarriersUtils.allJobsHandledBySelectedPlan(carrier), "All jobs are handled although a new service was added");
		}

		Assertions.assertTrue(Files.exists(out.resolve("VRP_Solution_Stats.csv")));
		Assertions.assertTrue(Files.exists(out.resolve("VRP_Solution_Stats_perCarrier.csv")));
		Assertions.assertTrue(Files.exists(out.resolve("VRP_Solution_Stats.png")));

		try (BufferedReader reader = IOUtils.getBufferedReader(out.resolve("VRP_Solution_Stats.csv").toString())) {
			CSVParser parse = CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter('\t').setHeader()
				.setSkipHeaderRecord(true).get().parse(reader);
			int count = 0;
			double lastBestScore = Double.MAX_VALUE;
			for (CSVRecord record : parse) {
				int numberOfIterations = Integer.parseInt(record.get("jsprit_iteration"));
				int numberOfRunCarrier = Integer.parseInt(record.get("runCarrier"));
				double bestScoreOfThisIteration = Double.parseDouble(record.get("sumJspritScores"));
				Assertions.assertTrue(bestScoreOfThisIteration <= lastBestScore);

				if (bestScoreOfThisIteration<lastBestScore)
					lastBestScore = bestScoreOfThisIteration;

				Assertions.assertEquals(count, numberOfIterations, "The number of iterations is not as expected");
				Assertions.assertEquals(2, numberOfRunCarrier);
				count++;
			}
			Assertions.assertEquals(21, count); // this should be 21, because the carrier have 20 iterations + initial solution
		}
		try (BufferedReader reader = IOUtils.getBufferedReader(out.resolve("VRP_Solution_Stats_perCarrier.csv").toString())) {
			CSVParser parse = CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter('\t').setHeader()
				.setSkipHeaderRecord(true).get().parse(reader);
			int count = 0;
			for (CSVRecord record : parse) {
				int iteration = Integer.parseInt(record.get("jsprit_iteration"));
				Double scoreThisSolution = Double.parseDouble(record.get("costsOfThisSolution"));
				Double scoreBest = Double.parseDouble(record.get("currentBestSolutionCost"));
				String strategy = record.get("strategyOfThisIteration");
				Assertions.assertNotNull(scoreBest);
				Assertions.assertNotNull(scoreThisSolution);
				Assertions.assertNotNull(strategy);
				if (iteration == 0)
					Assertions.assertEquals("initialSolution", strategy);
				Assertions.assertTrue(scoreBest<=scoreThisSolution);
				count++;
			}
			Assertions.assertEquals(42, count); // this should be 42, because the two carrier have 20 iterations + initial solution each
		}
	}

	@Test
	void testJspritWithDefaultSolutionOption() throws ExecutionException, InterruptedException {
		final String networkFilename = utils.getClassInputDirectory() + "/merged-network-simplified.xml.gz";
		final String vehicleTypeFilename = Path.of(utils.getPackageInputDirectory()).getParent().resolve("vehicleTypes_v2.xml").toString();
		final String carrierFilename = utils.getClassInputDirectory() + "/carrier.xml";

		Config config = ConfigUtils.createConfig();
		config.global().setRandomSeed(4177);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setCarriersFile(carrierFilename);
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(vehicleTypeFilename);
		freightCarriersConfigGroup.setTravelTimeSliceWidth(24*3600);
		freightCarriersConfigGroup.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.enforceBeginnings);

		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);

		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		for (Carrier carrier : CarriersUtils.getCarriers(scenario).getCarriers().values()) {
			CarriersUtils.setJspritIterations(carrier, 1);
		}

		CarriersUtils.runJsprit(scenario, CarriersUtils.CarrierSelectionForSolution.solveForAllCarriersAndOverrideExistingPlans);
		double scoreWithRunJsprit = 0;
		for (Carrier carrier : CarriersUtils.getCarriers(scenario).getCarriers().values()) {
			scoreWithRunJsprit = scoreWithRunJsprit + carrier.getSelectedPlan().getJspritScore();
		}
		double scoreRunWithOldStructure = generateCarrierPlans(scenario.getNetwork(), CarriersUtils.getCarriers(scenario), CarriersUtils.getOrAddCarrierVehicleTypes(scenario));
		Assertions.assertEquals(scoreWithRunJsprit, scoreRunWithOldStructure, MatsimTestUtils.EPSILON, "The score of both runs are not the same");
	}

	private static double generateCarrierPlans(Network network, Carriers carriers, CarrierVehicleTypes vehicleTypes) {
		final Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance(network,
				vehicleTypes.getVehicleTypes().values());
		// netBuilder.setBaseTravelTimeAndDisutility(travelTime, travelDisutility) ;
		netBuilder.setTimeSliceWidth(1800); // !!!!, otherwise it will not do anything.
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build();
		double score = 0;

		for (Carrier carrier : carriers.getCarriers().values()) {

			carrier.clearPlans();
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier,
					network);
			vrpBuilder.setRoutingCost(netBasedCosts);
			VehicleRoutingProblem problem = vrpBuilder.build();

			VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);

			VehicleRoutingProblemSolution solution = Solutions.bestOf(algorithm.searchSolutions());
			CarrierPlan newPlan = MatsimJspritFactory.createPlan(solution);

			NetworkRouter.routePlan(newPlan, netBasedCosts);
			// (maybe not optimal, but since re-routing is a matsim strategy,
			// certainly ok as initial solution)

			carrier.addPlan(newPlan);

			SolutionPrinter.print(problem, solution, SolutionPrinter.Print.VERBOSE);
			score = score + newPlan.getJspritScore();
		}
		return score;
	}

}
