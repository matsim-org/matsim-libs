package org.matsim.contrib.freight.usecases.chessboard.replanning;

import java.io.File;
import java.util.Collection;

import jsprit.analysis.toolbox.AlgorithmSearchProgressChartListener;
import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.VehicleRoutingAlgorithmBuilder;
import jsprit.core.algorithm.io.VehicleRoutingAlgorithms;
import jsprit.core.algorithm.state.StateManager;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.constraint.ConstraintManager;
import jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import jsprit.core.problem.driver.Driver;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.vehicle.Vehicle;
import jsprit.core.util.Solutions;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.VehicleTypeDependentRoadPricingCalculator;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.router.util.TravelTime;

public class VehicleReRouter implements GenericPlanStrategyModule<CarrierPlan>{

    private final Network network;
//
//    private final CarrierVehicleTypes vehicleTypes;
//
//    private final TravelTime travelTimes;
//
    private final String vrpAlgorithmConfig;

    private final VehicleRoutingTransportCosts vehicleRoutingTransportCosts;

    private final VehicleRoutingActivityCosts vehicleRoutingActivityCosts;

    private VehicleTypeDependentRoadPricingCalculator roadPricing;

    public VehicleReRouter(Network network, CarrierVehicleTypes vehicleTypes, TravelTime travelTimes, String vrpAlgoConfigFile, VehicleTypeDependentRoadPricingCalculator roadPricing) {
        this.network = network;
        vehicleRoutingTransportCosts = getNetworkBasedTransportCosts(network,vehicleTypes,travelTimes,roadPricing);
        vehicleRoutingActivityCosts = new VehicleRoutingActivityCosts() {

            private double penalty4missedTws = 0.01;

            @Override
            public double getActivityCost(TourActivity act, double arrivalTime, Driver arg2, Vehicle vehicle) {
                double tooLate = Math.max(0, arrivalTime - act.getTheoreticalLatestOperationStartTime());
                double waiting = Math.max(0, act.getTheoreticalEarliestOperationStartTime() - arrivalTime);
                double service = act.getOperationTime() * vehicle.getType().getVehicleCostParams().perTimeUnit;
                return penalty4missedTws * tooLate + vehicle.getType().getVehicleCostParams().perTimeUnit * waiting + service;
            }
        };
        vrpAlgorithmConfig = vrpAlgoConfigFile;
    }

    public VehicleReRouter(Network network, VehicleRoutingTransportCosts transportCosts, VehicleRoutingActivityCosts activityCosts, String vrpAlgoConfigFile){
        this.network = network;
        vehicleRoutingActivityCosts = activityCosts;
        vehicleRoutingTransportCosts = transportCosts;
        this.vrpAlgorithmConfig = vrpAlgoConfigFile;
    }


    @Override
    public void handlePlan(CarrierPlan carrierPlan) {
        //		System.out.println("REPLAN " + carrierPlan.getCarrier().getId());
        Carrier carrier = carrierPlan.getCarrier();

        //construct the routing problem - here the interface to jsprit comes into play
        VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);

        vrpBuilder.setRoutingCost(vehicleRoutingTransportCosts);
        vrpBuilder.setActivityCosts(vehicleRoutingActivityCosts);

        //build the problem
        VehicleRoutingProblem vrp = vrpBuilder.build();

        VehicleRoutingAlgorithmBuilder vraBuilder = new VehicleRoutingAlgorithmBuilder(vrp, vrpAlgorithmConfig);
        vraBuilder.addDefaultCostCalculators();

        StateManager stateManager = new StateManager(vrp);
        stateManager.updateLoadStates();

        ConstraintManager constraintManager = new ConstraintManager(vrp,stateManager);
        constraintManager.addLoadConstraint();
        vraBuilder.setStateAndConstraintManager(stateManager, constraintManager);

        VehicleRoutingAlgorithm vra = vraBuilder.build();

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

        //route plan (currently jsprit does not memorizes the routes, thus route the plan)
//		NetworkRouter.routePlan(plan, networkBasedTransportCosts);

        //set new plan
        carrierPlan.getScheduledTours().clear();
        carrierPlan.getScheduledTours().addAll(plan.getScheduledTours());

    }

    private NetworkBasedTransportCosts getNetworkBasedTransportCosts(Network network, CarrierVehicleTypes vehicleTypes, TravelTime travelTimes, VehicleTypeDependentRoadPricingCalculator roadPricing) {
        //******
        //Define transport-costs
        //******
        //construct network-based routing costs
        //by default travelTimes are calculated with freeSpeed and vehicleType.maxVelocity on the network
        NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(network, vehicleTypes.getVehicleTypes().values());

        //sets time-dependent travelTimes
        tpcostsBuilder.setTravelTime(travelTimes);

        if(roadPricing != null) tpcostsBuilder.setRoadPricingCalculator(roadPricing);

        //sets time-slice to build time-dependent tpcosts and traveltime matrices
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
