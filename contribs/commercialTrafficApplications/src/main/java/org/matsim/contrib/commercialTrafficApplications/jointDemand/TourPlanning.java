/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.commercialTrafficApplications.jointDemand;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.constraint.ServiceDeliveriesFirstConstraint;
import com.graphhopper.jsprit.core.problem.constraint.VehicleDependentTimeWindowConstraints;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlan;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.jsprit.MatsimJspritFactory;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;
import org.matsim.freight.carriers.jsprit.VRPTransportCosts;
import org.matsim.core.router.util.TravelTime;
import org.matsim.freight.carriers.jsprit.NetworkRouter;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

//import org.matsim.contrib.freight.jsprit.NetworkRouter;

class TourPlanning {

	private static final Logger log = LogManager.getLogger(TourPlanning.class);

	static void runTourPlanningForCarriersWithNetBasedCosts(Carriers carriers, Scenario scenario, int jSpritTimeSliceWidth,
															TravelTime travelTime) throws ExecutionException, InterruptedException {

		Set<VehicleType> vehicleTypes = new HashSet<>();
		carriers.getCarriers().values()
				.forEach(carrier -> vehicleTypes.addAll(carrier.getCarrierCapabilities().getVehicleTypes()));

		NetworkBasedTransportCosts.Builder netBuilder = NetworkBasedTransportCosts.Builder
				.newInstance(scenario.getNetwork(), vehicleTypes);
		log.info("SETTING TIME SLICE TO " + jSpritTimeSliceWidth);

		netBuilder.setTimeSliceWidth(jSpritTimeSliceWidth); // !!!! otherwise it will not do anything.
		netBuilder.setTravelTime(travelTime);

		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build();

		runTourPlanningForCarriers(carriers, scenario, netBasedCosts);
	}

	static void runTourPlanningForCarriers(Carriers carriers, Scenario scenario, VRPTransportCosts transportCosts) throws InterruptedException, ExecutionException {

		HashMap<Id<Carrier>, Integer> carrierServiceCounterMap = new HashMap<>();

		// Fill carrierServiceCounterMap
		for (Carrier carrier : carriers.getCarriers().values()) {
			carrierServiceCounterMap.put(carrier.getId(), carrier.getServices().size());

		}

		HashMap<Id<Carrier>, Integer> sortedMap = carrierServiceCounterMap.entrySet().stream()
				.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));


		ArrayList<Id<Carrier>> tempList = new ArrayList<>(sortedMap.keySet());
		ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
		forkJoinPool.submit(() -> tempList.parallelStream().forEach(carrierId -> {
			Carrier carrier = carriers.getCarriers().get(carrierId);

			// carriers.getCarriers().values().parallelStream().forEach(carrier -> {
			double start = System.currentTimeMillis();
			int serviceCount = carrier.getServices().size();
			log.info("start tour planning for " + carrier.getId() + " which has " + serviceCount + " services");

			// Build VRP

			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier,
					scenario.getNetwork());

			vrpBuilder.setRoutingCost(transportCosts);

			VehicleRoutingProblem problem = vrpBuilder.build();

			double radialShare = 0.3; // standard radial share is 0.3
			double randomShare = 0.5; // standard random share is 0.5
			if (serviceCount > 1000) { // if problem is huge, take only half the share for replanning
				radialShare = 0.15;
				randomShare = 0.25;
			}

			int radialServicesReplanned = Math.max(1, (int) (serviceCount * radialShare));
			int randomServicesReplanned = Math.max(1, (int) (serviceCount * randomShare));

			// use this in order to set a 'hard' constraint on time windows
			StateManager stateManager = new StateManager(problem);
			ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
			constraintManager.addConstraint(new ServiceDeliveriesFirstConstraint(),
					ConstraintManager.Priority.CRITICAL);
			constraintManager.addConstraint(new VehicleDependentTimeWindowConstraints(stateManager,
					problem.getTransportCosts(), problem.getActivityCosts()), ConstraintManager.Priority.HIGH);
			// add Multiple Threads
			int jspritThreads = 1;

			if (serviceCount > 50) {
				jspritThreads = (Runtime.getRuntime().availableProcessors());
			}
			VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem)
					.setStateAndConstraintManager(stateManager, constraintManager)
					.setProperty(Jsprit.Parameter.THREADS, String.valueOf(jspritThreads))
					.setProperty(Jsprit.Parameter.RADIAL_MIN_SHARE, String.valueOf(radialServicesReplanned))
					.setProperty(Jsprit.Parameter.RADIAL_MAX_SHARE, String.valueOf(radialServicesReplanned))
					.setProperty(Jsprit.Parameter.RANDOM_BEST_MIN_SHARE, String.valueOf(randomServicesReplanned))
					.setProperty(Jsprit.Parameter.RANDOM_BEST_MAX_SHARE, String.valueOf(randomServicesReplanned))
					.buildAlgorithm();

			// get the algorithm out-of-the-box, search solution and get the best one.
			// VehicleRoutingAlgorithm algorithm = new
			// SchrimpfFactory().createAlgorithm(problem);

			if (serviceCount == 0) {
				log.info("setting maxIterations=1 as carrier has no services");
				algorithm.setMaxIterations(1);
			} else {
				algorithm.setMaxIterations(CarriersUtils.getJspritIterations(carrier));
			}

			// variationCoefficient = stdDeviation/mean. so i set the threshold rather soft
			// algorithm.addTerminationCriterion(new VariationCoefficientTermination(5,
			// 0.1)); //this does not seem to work, tschlenther august 2019

			Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
			VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
			log.info("tour planning for carrier " + carrier.getId() + " took "
					+ (System.currentTimeMillis() - start) / 1000 + " seconds.");
			// get the CarrierPlan
			CarrierPlan carrierPlan = MatsimJspritFactory.createPlan(carrier, bestSolution);

			log.info("routing plan for carrier " + carrier.getId());
			NetworkRouter.routePlan(carrierPlan, transportCosts); // we need to route
																									// the plans in
																									// order to create
																									// reasonable
																									// freight-agent
																									// plans
			log.info("routing for carrier " + carrier.getId() + " finished. Tour planning plus routing took "
					+ (System.currentTimeMillis() - start) / 1000 + " seconds.");
			carrier.addPlan(carrierPlan);
			carrier.setSelectedPlan(carrierPlan);
		})).get();
		;
	}
}
