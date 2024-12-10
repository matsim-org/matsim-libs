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

package org.matsim.freight.carriers.controller;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.io.algorithm.AlgorithmConfig;
import com.graphhopper.jsprit.io.algorithm.AlgorithmConfigXmlReader;
import com.graphhopper.jsprit.io.algorithm.VehicleRoutingAlgorithms;
import java.util.Collection;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlan;
import org.matsim.freight.carriers.CarrierVehicleTypes;
import org.matsim.freight.carriers.jsprit.MatsimJspritFactory;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;

class CarrierVehicleReRouter implements GenericPlanStrategyModule<CarrierPlan>{

    private final Network network;

    private final String vrpAlgorithmConfig;

    private final VehicleRoutingTransportCosts vehicleRoutingTransportCosts;

    private final VehicleRoutingActivityCosts vehicleRoutingActivityCosts;

	public CarrierVehicleReRouter( Network network, CarrierVehicleTypes vehicleTypes, TravelTime travelTimes, String vrpAlgoConfigFile, RoadPricingScheme roadPricing ) {
        this.network = network;
        vehicleRoutingTransportCosts = getNetworkBasedTransportCosts(network,vehicleTypes,travelTimes,roadPricing);
        vehicleRoutingActivityCosts = new VehicleRoutingActivityCosts() {

            private final double penalty4missedTws = 0.01;

            //TODO: KMT/jan18 Replace per TimeUnit to per Transport/Service/WaitingTimeUnit ... but make sure that this where set correctly.
            @Override
            public double getActivityCost(TourActivity act, double arrivalTime, Driver arg2, Vehicle vehicle) {
                double tooLate = Math.max(0, arrivalTime - act.getTheoreticalLatestOperationStartTime());
                double waiting = Math.max(0, act.getTheoreticalEarliestOperationStartTime() - arrivalTime);
                double service = act.getOperationTime() * vehicle.getType().getVehicleCostParams().perServiceTimeUnit;
                return penalty4missedTws * tooLate + vehicle.getType().getVehicleCostParams().perWaitingTimeUnit * waiting + service;		//TODO: KMT/jan 18 It is a bit confusing to me why there are some values already multiplied with costParams and others not.
            }

			@Override
			public double getActivityDuration(TourActivity tourAct, double arrivalTime, Driver driver,
					Vehicle vehicle) {
				return Math.max(0, tourAct.getEndTime() - tourAct.getArrTime());
			}
        };
        vrpAlgorithmConfig = vrpAlgoConfigFile;
    }

    public CarrierVehicleReRouter( Network network, VehicleRoutingTransportCosts transportCosts, VehicleRoutingActivityCosts activityCosts, String vrpAlgoConfigFile ){
        this.network = network;
        vehicleRoutingActivityCosts = activityCosts;
        vehicleRoutingTransportCosts = transportCosts;
        this.vrpAlgorithmConfig = vrpAlgoConfigFile;
    }


    @Override
    public void handlePlan(CarrierPlan carrierPlan) {
        Carrier carrier = carrierPlan.getCarrier();

        //construct the routing problem - here the interface to jsprit comes into play
        VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);

        vrpBuilder.setRoutingCost(vehicleRoutingTransportCosts);
        vrpBuilder.setActivityCosts(vehicleRoutingActivityCosts);

        //build the problem
        VehicleRoutingProblem vrp = vrpBuilder.build();

        //configure the algorithm
        AlgorithmConfig algorithmConfig = new AlgorithmConfig();
        AlgorithmConfigXmlReader xmlReader = new AlgorithmConfigXmlReader(algorithmConfig);
        xmlReader.read(vrpAlgorithmConfig);

        StateManager stateManager = new StateManager(vrp);
        stateManager.updateLoadStates();

        ConstraintManager constraintManager = new ConstraintManager(vrp,stateManager);
        constraintManager.addLoadConstraint();

        boolean addDefaultCostCalculators = true;

        VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, algorithmConfig, 0, null, stateManager, constraintManager, addDefaultCostCalculators);

        //get configures algorithm
//		VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, vrpAlgorithmConfig);
//		vra.addListener(new AlgorithmSearchProgressChartListener("output/"+carrierPlan.getCarrier().getId() + "_" + carrierPlan.hashCode() + ".png"));

        //add initial-solution - which is the initialSolution for the vehicle-routing-algo
        vra.addInitialSolution(MatsimJspritFactory.createSolution(carrierPlan, vrp));

        //solve problem
        Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();

        //get best
        VehicleRoutingProblemSolution solution = Solutions.bestOf(solutions);

        //		SolutionPlotter.plotSolutionAsPNG(vrp, solution, "output/sol_"+System.currentTimeMillis()+".png", "sol");

        //create carrierPlan from solution
        CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);

        //route plan (currently jsprit does not memorize the routes, thus route the plan)
//		NetworkRouter.routePlan(plan, networkBasedTransportCosts);

        //set new plan
        carrierPlan.getScheduledTours().clear();
        carrierPlan.getScheduledTours().addAll(plan.getScheduledTours());

    }

    private NetworkBasedTransportCosts getNetworkBasedTransportCosts(Network network, CarrierVehicleTypes vehicleTypes, TravelTime travelTimes, RoadPricingScheme roadPricing ) {
        //******
        //Define transport-costs
        //******
        //construct network-based routing costs
        //by default travelTimes are calculated with freeSpeed and vehicleType.maxVelocity on the network
        NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(network, vehicleTypes.getVehicleTypes().values());

        //sets time-dependent travelTimes
        tpcostsBuilder.setTravelTime(travelTimes);

        if(roadPricing != null) tpcostsBuilder.setRoadPricingScheme(roadPricing );

        //sets time-slice to build time-dependent tpcosts and travelTime matrices
        tpcostsBuilder.setTimeSliceWidth(900);
//		tpcostsBuilder.setFIFO(true);
        //assign netBasedCosts to RoutingProblem
        return tpcostsBuilder.build();
    }

    @Override
    public void prepareReplanning(ReplanningContext replanningContext) {
        // TODO Auto-generated method stub

    }

    @Override
    public void finishReplanning() {
        // TODO Auto-generated method stub

    }

}
