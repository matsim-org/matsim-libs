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

package org.matsim.freight.carriers.usecases.chessboard;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.io.algorithm.AlgorithmConfig;
import com.graphhopper.jsprit.io.algorithm.AlgorithmConfigXmlReader;
import com.graphhopper.jsprit.io.algorithm.VehicleRoutingAlgorithms;
import java.util.Collection;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.jsprit.MatsimJspritFactory;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;
import org.matsim.freight.carriers.jsprit.NetworkRouter;

final class InitialCarrierPlanCreator {

    private final Network network;

    public InitialCarrierPlanCreator(Network network) {
        this.network = network;
    }

    public CarrierPlan createPlan(Carrier carrier){
//		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
//		NetworkBasedTransportCosts.Builder costsBuilder = NetworkBasedTransportCosts.Builder.newInstance(network, carrier.getCarrierCapabilities().getVehicleTypes());
//		NetworkBasedTransportCosts costs = costsBuilder.build();
//		vrpBuilder.setRoutingCost(costs);
//		VehicleRoutingProblem vrp = vrpBuilder.build();
//
//		VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, "input/usecases/chessboard/vrpalgo/ini_algorithm_v2.xml");
////		vra.getAlgorithmListeners().addListener(new AlgorithmSearchProgressChartListener("output/"+carrier.getId()+".png"));
//		Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();
//
//		CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, Solutions.bestOf(solutions));
//


        //construct the routing problem - here the interface to jsprit comes into play
        VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);

        //******
        //Define transport-costs
        //******
        //construct network-based routing costs
        //by default travelTimes are calculated with freeSpeed and vehicleType.maxVelocity on the network
        NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(network, carrier.getCarrierCapabilities().getVehicleTypes());
        //sets time-dependent travelTimes
        //				tpcostsBuilder.setTravelTime(travelTimes);
        //sets time-slice to build time-dependent tpcosts and traveltime matrices
        //				tpcostsBuilder.setTimeSliceWidth(900);
        //				tpcostsBuilder.setFIFO(true);
        //assign netBasedCosts to RoutingProblem
        NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();

        //set transport-costs
        vrpBuilder.setRoutingCost(netbasedTransportcosts);

        //******
        //Define activity-costs
        //******
        //should be inline with activity-scoring
        VehicleRoutingActivityCosts activitycosts = new VehicleRoutingActivityCosts(){

            private final double penalty4missedTws = 0.01;

            @Override
            public double getActivityCost(TourActivity act, double arrivalTime, Driver arg2, Vehicle vehicle) {
                double tooLate = Math.max(0, arrivalTime - act.getTheoreticalLatestOperationStartTime());
                double waiting = Math.max(0, act.getTheoreticalEarliestOperationStartTime() - arrivalTime);
                //						double waiting = 0.;
                double service = act.getOperationTime()*vehicle.getType().getVehicleCostParams().perServiceTimeUnit;
                return penalty4missedTws*tooLate + vehicle.getType().getVehicleCostParams().perWaitingTimeUnit*waiting + service;
            }

			@Override
			public double getActivityDuration(TourActivity tourAct, double arrivalTime, Driver driver,
					Vehicle vehicle) {
				return Math.max(0, tourAct.getEndTime() - tourAct.getArrTime());
			}

        };
        vrpBuilder.setActivityCosts(activitycosts);

        //build the problem
        VehicleRoutingProblem vrp = vrpBuilder.build();

        //configure the algorithm
        AlgorithmConfig algorithmConfig = new AlgorithmConfig();
        AlgorithmConfigXmlReader xmlReader = new AlgorithmConfigXmlReader(algorithmConfig);
        xmlReader.read("input/usecases/chessboard/vrpalgo/ini_algorithm_v2.xml");

        StateManager stateManager = new StateManager(vrp);
        stateManager.updateLoadStates();

        ConstraintManager constraintManager = new ConstraintManager(vrp,stateManager);
        constraintManager.addLoadConstraint();

        boolean addDefaultCostCalculators = true;

        VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, algorithmConfig, 0, null, stateManager, constraintManager, addDefaultCostCalculators);

        //get configures algorithm
        //				VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, vrpAlgorithmConfig);
        //				vra.getAlgorithmListeners().addListener(new AlgorithmSearchProgressChartListener("output/"+carrierPlan.getCarrier().getId() + "_" + carrierPlan.hashCode() + ".png"));
        //add initial-solution - which is the initialSolution for the vehicle-routing-algo
        //				vra.addInitialSolution(MatsimJspritFactory.createSolution(carrierPlan, network));

        //solve problem
        Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();

        //get best
        VehicleRoutingProblemSolution solution = Solutions.bestOf(solutions);

        //		SolutionPlotter.plotSolutionAsPNG(vrp, solution, "output/sol_"+System.currentTimeMillis()+".png", "sol");

        //create carrierPlan from solution
        CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);
        NetworkRouter.routePlan(plan, netbasedTransportcosts);
        return plan;
    }

    public static void main(String[] args) {

        Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("input/usecases/chessboard/network/grid9x9_cap20.xml");

        CarrierVehicleTypes types = new CarrierVehicleTypes();
        new CarrierVehicleTypeReader(types).readFile("input/usecases/chessboard/freight/vehicleTypes.xml");

        Carriers carriers = new Carriers();
        new CarrierPlanXmlReader(carriers, types ).readFile("input/usecases/chessboard/freight/carrierPlansWithoutRoutes_10minTW.xml" );

        for(Carrier carrier : carriers.getCarriers().values()){
            CarrierPlan plan = new InitialCarrierPlanCreator(scenario.getNetwork()).createPlan(carrier);
            carrier.addPlan(plan);
        }
		CarriersUtils.writeCarriers(carriers, "input/usecases/chessboard/freight/carrierPlans_10minTW.xml");
    }

}
