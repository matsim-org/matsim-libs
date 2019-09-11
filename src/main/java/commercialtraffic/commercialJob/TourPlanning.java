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
import commercialtraffic.NetworkBasedTransportCosts;
import commercialtraffic.NetworkRouter;
import commercialtraffic.integration.CarrierJSpritIterations;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
//import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
//import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

public class TourPlanning  {

    static Logger log = Logger.getLogger(TourPlanning.class);


    static void runTourPlanningForCarriers(Carriers carriers, Scenario scenario, CarrierJSpritIterations iterations, int jSpritTimeSliceWidth, TravelTime travelTime) {
        Set<CarrierVehicleType> vehicleTypes = new HashSet<>();
        carriers.getCarriers().values().forEach(carrier -> vehicleTypes.addAll(carrier.getCarrierCapabilities().getVehicleTypes()));
        NetworkBasedTransportCosts.Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance(scenario.getNetwork(), vehicleTypes);
        log.info("SETTING TIME SLICE TO " + jSpritTimeSliceWidth);
        
        
        netBuilder.setTimeSliceWidth(jSpritTimeSliceWidth); // !!!! otherwise it will not do anything.
        netBuilder.setTravelTime(travelTime);

        

        
        final NetworkBasedTransportCosts netBasedCosts1 = netBuilder.build();
        
carriers.getCarriers().values().parallelStream().forEach(carrier -> {
                    double start = System.currentTimeMillis();
                    int serviceCount =  carrier.getServices().size();
                    log.info("start tour planning for " + carrier.getId() + " which has " + serviceCount + " services");

                    //Build VRP
                    
                    VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, scenario.getNetwork());
                    
                    vrpBuilder.setRoutingCost(netBasedCosts1);// this may be too expensive for the size of the problem
                    
                    VehicleRoutingProblem problem = vrpBuilder.build();

                    double radialShare =  0.3;  //standard radial share is 0.3
                    double randomShare = 0.5;   //standard random share is 0.5
                    if(serviceCount > 1000){ //if problem is huge, take only half the share for replanning
                        radialShare = 0.15;
                        randomShare = 0.25;
                    }

                    int radialServicesReplanned = Math.max( 1, (int) (serviceCount * radialShare));
                    int randomServicesReplanned = Math.max( 1, (int) (serviceCount * randomShare));

                    //use this in order to set a 'hard' constraint on time windows
                    StateManager stateManager = new StateManager(problem);
                    ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
                    constraintManager.addConstraint(new ServiceDeliveriesFirstConstraint(), ConstraintManager.Priority.CRITICAL);
                    constraintManager.addConstraint(new VehicleDependentTimeWindowConstraints(stateManager, problem.getTransportCosts(), problem.getActivityCosts()), ConstraintManager.Priority.HIGH);
                    //add Multiple Threads
                    VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem)
                            .setStateAndConstraintManager(stateManager,constraintManager)
                            .setProperty(Jsprit.Parameter.THREADS,String.valueOf(32))
                            .setProperty(Jsprit.Parameter.RADIAL_MIN_SHARE, String.valueOf(radialServicesReplanned))
                            .setProperty(Jsprit.Parameter.RADIAL_MAX_SHARE, String.valueOf(radialServicesReplanned))
                            .setProperty(Jsprit.Parameter.RANDOM_BEST_MIN_SHARE, String.valueOf(randomServicesReplanned))
                            .setProperty(Jsprit.Parameter.RANDOM_BEST_MAX_SHARE, String.valueOf(randomServicesReplanned))
                            .buildAlgorithm();


                    // get the algorithm out-of-the-box, search solution and get the best one.
//                    VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);

                    if(serviceCount == 0){
                        log.info("setting maxIterations=1 as carrier has no services");
                        algorithm.setMaxIterations(1);
                    } else{
                        algorithm.setMaxIterations(iterations.getNrOfJSpritIterationsForCarrier(carrier.getId()));
                    }

                    // variationCoefficient = stdDeviation/mean. so i set the threshold rather soft
//                    algorithm.addTerminationCriterion(new VariationCoefficientTermination(5, 0.1)); //this does not seem to work, tschlenther august 2019

                    Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
                    VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
                    log.info("tour planning for carrier " + carrier.getId() + " took " + (System.currentTimeMillis() - start)/1000 + " seconds.");
                    //get the CarrierPlan
                    CarrierPlan carrierPlan = MatsimJspritFactory.createPlan(carrier, bestSolution);

                    log.info("routing plan for carrier " + carrier.getId());
                    NetworkRouter.routePlan(carrierPlan, netBasedCosts1);    //we need to route the plans in order to create reasonable freight-agent plans
                    log.info("routing for carrier " + carrier.getId() + " finished. Tour planning plus routing took " + (System.currentTimeMillis() - start)/1000 + " seconds." );
                    carrier.setSelectedPlan(carrierPlan);
                }
        );
    }
}
