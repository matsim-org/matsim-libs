/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
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

package commercialtraffic.commercialJob;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.constraint.ServiceDeliveriesFirstConstraint;
import com.graphhopper.jsprit.core.problem.constraint.VehicleDependentTimeWindowConstraints;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import commercialtraffic.integration.CarrierMode;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.core.router.util.TravelTime;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TourPlanning  {

    static Logger log = Logger.getLogger(TourPlanning.class);

    @Inject
    CarrierMode carrierMode;

    static void runTourPlanningForCarriers(Carriers carriers, Scenario scenario, int maxIterations, TravelTime travelTime) {
        Set<CarrierVehicleType> vehicleTypes = new HashSet<>();
        carriers.getCarriers().values().forEach(carrier -> vehicleTypes.addAll(carrier.getCarrierCapabilities().getVehicleTypes()));
        NetworkBasedTransportCosts.Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance(scenario.getNetwork(), vehicleTypes);
        netBuilder.setTimeSliceWidth(1800); // !!!! otherwise it will not do anything.
        netBuilder.setTravelTime(travelTime);
        final NetworkBasedTransportCosts netBasedCosts = netBuilder.build();
        carriers.getCarriers().values().parallelStream().forEach(carrier -> {
                    log.info("start tour planning for " + carrier.getId());
                    //Build VRP
                    VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, scenario.getNetwork());
                    vrpBuilder.setRoutingCost(netBasedCosts);// this may be too expensive for the size of the problem

                    VehicleRoutingProblem problem = vrpBuilder.build();

                    //use this in order to set a 'hard' constraint on time windows
                    StateManager stateManager = new StateManager(problem);
                    ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
                    constraintManager.addConstraint(new ServiceDeliveriesFirstConstraint(), ConstraintManager.Priority.CRITICAL);
                    constraintManager.addConstraint(new VehicleDependentTimeWindowConstraints(stateManager, problem.getTransportCosts(), problem.getActivityCosts()), ConstraintManager.Priority.HIGH);
                    VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem)
                                                        .setStateAndConstraintManager(stateManager,constraintManager)
                                                        .buildAlgorithm();

                    // get the algorithm out-of-the-box, search solution and get the best one.
//                    VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);

                    algorithm.setMaxIterations(maxIterations);

                    // variationCoefficient = stdDeviation/mean. so i set the threshold rather soft
//                    algorithm.addTerminationCriterion(new VariationCoefficientTermination(5, 0.1));

                    Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
                    VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
                    //get the CarrierPlan
                    CarrierPlan carrierPlan = MatsimJspritFactory.createPlan(carrier, bestSolution);

                    log.info("routing plan for carrier " + carrier.getId());
                    NetworkRouter.routePlan(carrierPlan, netBasedCosts);    //we need to route the plans in order to create reasonable freight-agent plans
                    log.info("routing for carrier " + carrier.getId() + " finished");
                    carrier.setSelectedPlan(carrierPlan);
                }
        );
    }
}
