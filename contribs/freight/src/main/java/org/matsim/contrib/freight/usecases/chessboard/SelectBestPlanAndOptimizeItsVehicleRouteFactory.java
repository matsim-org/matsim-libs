package org.matsim.contrib.freight.usecases.chessboard;

import java.net.URL;
import java.util.Collection;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.io.algorithm.VehicleRoutingAlgorithms;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

final class SelectBestPlanAndOptimizeItsVehicleRouteFactory {
	
	final URL url = ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9");
	
	private Network network;
	
	private CarrierVehicleTypes vehicleTypes;
	
	private TravelTime travelTimes;
	
	public SelectBestPlanAndOptimizeItsVehicleRouteFactory(Network network, CarrierVehicleTypes vehicleTypes, TravelTime travelTimes) {
		super();
		this.network = network;
		this.vehicleTypes = vehicleTypes;
		this.travelTimes = travelTimes;
	}

	public GenericPlanStrategy<CarrierPlan, Carrier> createStrategy(){
//		CarrierReplanningStrategy replanningStrat = new CarrierReplanningStrategy(new SelectBestPlan());
		GenericPlanStrategyImpl<CarrierPlan, Carrier> replanningStrat = new GenericPlanStrategyImpl<CarrierPlan, Carrier>( new BestPlanSelector<CarrierPlan, Carrier>() ) ;
		
		GenericPlanStrategyModule<CarrierPlan> vraModule = new GenericPlanStrategyModule<CarrierPlan>() {
			
			@Override
			public void handlePlan(CarrierPlan carrierPlan) {
//				System.out.println("REPLAN " + carrierPlan.getCarrier().getId());
				Carrier carrier = carrierPlan.getCarrier();
				
				//construct the routing problem - here the interface to jsprit comes into play
				VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
				
				//******
				//Define transport-costs
				//******
				//construct network-based routing costs
				//by default travelTimes are calculated with freeSpeed and vehicleType.maxVelocity on the network
				NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(network, vehicleTypes.getVehicleTypes().values());
				//sets time-dependent travelTimes
				tpcostsBuilder.setTravelTime(travelTimes);
				//sets time-slice to build time-dependent tpcosts and traveltime matrices
				tpcostsBuilder.setTimeSliceWidth(900);
				
				//assign netBasedCosts to RoutingProblem
				NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();
				
				//set transport-costs
				vrpBuilder.setRoutingCost(netbasedTransportcosts);
				
				//******
				//Define activity-costs
				//******
				//should be inline with activity-scoring 
				VehicleRoutingActivityCosts activitycosts = new VehicleRoutingActivityCosts(){

					private double penalty4missedTws = 0.008; 
					
					//TODO: Why is here always returned 0.0? KMT jan/2018
					@Override
					public double getActivityCost(TourActivity act, double arrivalTime, Driver arg2, Vehicle vehicle) {	
						double tooLate = Math.max(0, arrivalTime - act.getTheoreticalLatestOperationStartTime());
//						double waiting = Math.max(0, act.getTheoreticalEarliestOperationStartTime() - arrivalTime);
//						double service = act.getOperationTime()*vehicle.getType().getVehicleCostParams().perTimeUnit;
//						return penalty4missedTws*tooLate + vehicle.getType().getVehicleCostParams().perTimeUnit*waiting + service;
//						return penalty4missedTws*tooLate;
						return 0.0;
					}

					@Override
					public double getActivityDuration(TourActivity tourAct, double arrivalTime, Driver driver,
							Vehicle vehicle) {
						// TODO Auto-generated method stub
						return 0;
					}
					
				};
				vrpBuilder.setActivityCosts(activitycosts);
				
				//build the problem
				VehicleRoutingProblem vrp = vrpBuilder.build();
				
				//get configures algorithm
				VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, IOUtils.extendUrl(url, "algorithm.xml"));
//				vra.getAlgorithmListeners().addListener(new AlgorithmSearchProgressChartListener("output/"+carrierPlan.getCarrier().getId() + "_" + carrierPlan.hashCode() + ".png"));
				//add initial-solution - which is the initialSolution for the vehicle-routing-algo
//				vra.addInitialSolution(MatsimJspritFactory.createSolution(carrierPlan, network));
				
				//solve problem
				Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();
				
				//get best 
				VehicleRoutingProblemSolution solution = Solutions.bestOf(solutions);
				
//				SolutionPlotter.plotSolutionAsPNG(vrp, solution, "output/sol_"+System.currentTimeMillis()+".png", "sol");
				
				//create carrierPlan from solution
				CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);
				
				//route plan (currently jsprit does not memorizes the routes, thus route the plan)
				NetworkRouter.routePlan(plan, netbasedTransportcosts);
				
				//set new plan
				carrierPlan.getScheduledTours().clear();
				carrierPlan.getScheduledTours().addAll(plan.getScheduledTours());
				
				//h
//				carrierPlan.setScore(plan.getScore());
				
			}

			@Override
			public void prepareReplanning(ReplanningContext replanningContext) {
			}

			@Override
			public void finishReplanning() {
			}
		
		};
	
		replanningStrat.addStrategyModule(vraModule) ;
		return replanningStrat;
	}

}
