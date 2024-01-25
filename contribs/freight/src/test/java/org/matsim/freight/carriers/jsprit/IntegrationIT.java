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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.testcases.MatsimTestUtils;

import java.util.concurrent.ExecutionException;

public class IntegrationIT {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testJsprit() throws ExecutionException, InterruptedException {
		final String networkFilename = utils.getClassInputDirectory() + "/merged-network-simplified.xml.gz";
		final String vehicleTypeFilename = utils.getClassInputDirectory() + "/vehicleTypes.xml";
		final String carrierFilename = utils.getClassInputDirectory() + "/carrier.xml";

		Config config = ConfigUtils.createConfig();
		config.global().setRandomSeed(4177);

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

		CarriersUtils.runJsprit(scenario);
		double scoreWithRunJsprit = 0;
		for (Carrier carrier : CarriersUtils.getCarriers(scenario).getCarriers().values()) {
			scoreWithRunJsprit = scoreWithRunJsprit + carrier.getSelectedPlan().getJspritScore();
		}
		double scoreRunWithOldStructure = generateCarrierPlans(scenario.getNetwork(), CarriersUtils.getCarriers(scenario), CarriersUtils.getCarrierVehicleTypes(scenario));
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
			CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution);

			NetworkRouter.routePlan(newPlan, netBasedCosts);
			// (maybe not optimal, but since re-routing is a matsim strategy,
			// certainly ok as initial solution)

			carrier.setSelectedPlan(newPlan);

			SolutionPrinter.print(problem, solution, SolutionPrinter.Print.VERBOSE);
			score = score + newPlan.getJspritScore();
		}
		return score;
	}

}
