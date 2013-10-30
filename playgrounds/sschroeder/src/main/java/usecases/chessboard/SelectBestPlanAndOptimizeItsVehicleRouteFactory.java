package usecases.chessboard;

import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategy;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategyModule;
import org.matsim.contrib.freight.replanning.selectors.SelectBestPlan;

import util.Solutions;
import algorithms.VehicleRoutingAlgorithms;
import analysis.SolutionPlotter;
import basics.VehicleRoutingAlgorithm;
import basics.VehicleRoutingProblem;
import basics.VehicleRoutingProblemSolution;

public class SelectBestPlanAndOptimizeItsVehicleRouteFactory {
	
	private Network network;
	
	private CarrierVehicleTypes vehicleTypes;
	
	public CarrierReplanningStrategy createStrategy(){
		CarrierReplanningStrategy replanningStrat = new CarrierReplanningStrategy(new SelectBestPlan());
		
		CarrierReplanningStrategyModule vraModule = new CarrierReplanningStrategyModule() {
			
			@Override
			public void handlePlan(CarrierPlan carrierPlan) {
				Carrier carrier = carrierPlan.getCarrier();
				
				//construct the routing problem - here the interface to jsprit comes into play
				VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
				
				//construct network-based routing costs
				//by default travelTimes are calculated with freeSpeed and vehicleType.maxVelocity on the network
				NetworkBasedTransportCosts netBasedTransportCosts = NetworkBasedTransportCosts.Builder.newInstance(network, vehicleTypes.getVehicleTypes().values()).build();
				
				//assign netBasedCosts to RoutingProblem
				vrpBuilder.setRoutingCost(netBasedTransportCosts);
				
				//build the problem
				VehicleRoutingProblem vrp = vrpBuilder.build();
				
				//get configures algorithm
				VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, "input/algorithm.xml");
				
				//add initial-solution
				vra.addInitialSolution(MatsimJspritFactory.createSolution(carrierPlan, network));
				
				//solve problem
				Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();
				
				//get best 
				VehicleRoutingProblemSolution solution = Solutions.getBest(solutions);
				
				SolutionPlotter.plotSolutionAsPNG(vrp, solution, "output/sol_"+System.currentTimeMillis()+".png", "sol");
				
				//create carrierPlan from solution
				CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);
				
				//route plan (currently jsprit does not memorizes the routes, thus route the plan)
				NetworkRouter.routePlan(plan, netBasedTransportCosts);
				
				//set new plan
				carrierPlan.getScheduledTours().clear();
				carrierPlan.getScheduledTours().addAll(plan.getScheduledTours());
				carrierPlan.setScore(plan.getScore());
				
			}
		
		};
	
		replanningStrat.addModule(vraModule);
		return replanningStrat;
	}

}
