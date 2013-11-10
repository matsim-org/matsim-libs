package playground.mzilske.city2000w;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierController;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategy;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategyManager;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategyModule;
import org.matsim.contrib.freight.replanning.selectors.SelectBestPlan;
import org.matsim.core.api.internal.MatsimManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionAccumulator.BasicScoring;
import org.matsim.core.scoring.ScoringFunctionAccumulator.LegScoring;

import util.Solutions;
import algorithms.VehicleRoutingAlgorithms;
import analysis.SolutionPlotter;
import basics.VehicleRoutingAlgorithm;
import basics.VehicleRoutingProblem;
import basics.VehicleRoutingProblemSolution;

/**
 * Simple Sim
 * 
 * setup:
 * 1 carrier from gridScen
 * SimpleVRAStrategy that is performed in replanning of each iteration
 * - SelectBestPlan
 * - RuinAndRecreate
 * - Route
 * 
 * Memory: 3 Plans
 * 10 iterations
 * 
 * @author stefan
 *
 */
public class GridSim {
	
	static class DriversLegScoring implements BasicScoring, LegScoring {

		private double score = 0.0;

		private final Network network;
		
		private final Carrier carrier;
		
		private Set<CarrierVehicle> employedVehicles;
		
		private Leg currentLeg = null;
		
		private double currentLegStartTime;
		
		public DriversLegScoring(Carrier carrier, Network network) {
			super();
			this.network = network;
			this.carrier = carrier;
			employedVehicles = new HashSet<CarrierVehicle>();
		}

		
		@Override
		public void finish() {
			
		}


		@Override
		public double getScore() {
			return score;
		}


		@Override
		public void reset() {
			score = 0.0;
			employedVehicles.clear();
		}


		@Override
		public void startLeg(double time, Leg leg) {
			currentLeg = leg;
			currentLegStartTime = time; 
		}


		@Override
		public void endLeg(double time) {
			if(currentLeg.getRoute() instanceof NetworkRoute){
				NetworkRoute nRoute = (NetworkRoute) currentLeg.getRoute();
				Id vehicleId = nRoute.getVehicleId();
				CarrierVehicle vehicle = getVehicle(vehicleId);
				assert vehicle != null : "cannot find vehicle with id=" + vehicleId;
				if(!employedVehicles.contains(vehicle)){
					employedVehicles.add(vehicle);
				}
				double distance = 0.0;
				if(currentLeg.getRoute() instanceof NetworkRoute){
					distance += network.getLinks().get(currentLeg.getRoute().getStartLinkId()).getLength();
					for(Id linkId : ((NetworkRoute) currentLeg.getRoute()).getLinkIds()){
						distance += network.getLinks().get(linkId).getLength();
					}
					distance += network.getLinks().get(currentLeg.getRoute().getEndLinkId()).getLength();
				}
				score += (-1)*distance*getDistanceParameter(vehicle,null);
			}
			
		}
		
		private double getDistanceParameter(CarrierVehicle vehicle, Person driver) {
			return vehicle.getVehicleType().getVehicleCostInformation().perDistanceUnit;
		}

		private CarrierVehicle getVehicle(Id vehicleId) {
			for(CarrierVehicle cv : carrier.getCarrierCapabilities().getCarrierVehicles()){
				if(cv.getVehicleId().equals(vehicleId)){
					return cv;
				}
			}
			return null;
		}
		
		
	}
	
	public static void main(String[] args) {
		//load network, config and scenario --> load matsimStuff
//		Logger.getRootLogger().setLevel(Level.)
//		MatsimStuff matsimStuff = MatsimStuffLoader.loadNetworkAndGetStuff("input/freight/network.xml");
		
		Config config = new Config();
		config.addCoreModules();
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(10);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile("input/freight/network.xml");
		
		
		//create carrier container
		Carriers carriers = new Carriers();
		
		//read carriers and their capabilities
		new CarrierPlanXmlReaderV2(carriers).read("input/freight/carrier.xml");
		
//		Carrier.PLAN_MEMORY = 2;
		
		//read vehicleTypes and
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(vehicleTypes).read("input/freight/vehicleTypes.xml");
		//assign them to their corresponding vehicles
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes);
		
		createInitialPlans(carriers,vehicleTypes,scenario.getNetwork());
		
		//define scoring function - score plan according to distance and time parameter of vehicleTypes
		CarrierScoringFunctionFactory scoringFunctionFactory = defineAndCreateScoringFunction(carriers, scenario.getNetwork());
		
		//define replanning strategy
		final CarrierReplanningStrategyManager strategyManager = new CarrierReplanningStrategyManager();
		CarrierReplanningStrategy vra = defineAndCreateVehicleRoutingAlgorithm(scenario.getNetwork(),vehicleTypes);
		strategyManager.addStrategy(vra, 1.0);
		
		CarrierPlanStrategyManagerFactory strategyManagerFactory = new CarrierPlanStrategyManagerFactory() {
			
			@Override
			public MatsimManager createStrategyManager(Controler controler) {
				return strategyManager;
			}
		};
		
		//put all together
		CarrierController carrierController = new CarrierController(carriers, strategyManagerFactory, scoringFunctionFactory);
		
		//and run simulation
		Controler matsimController = new Controler(scenario);
		matsimController.addControlerListener(carrierController);
		matsimController.setOverwriteFiles(true);
		matsimController.run();
	}

	private static CarrierReplanningStrategy defineAndCreateVehicleRoutingAlgorithm(final Network network, final CarrierVehicleTypes vehicleTypes) {
		CarrierReplanningStrategy vra = new CarrierReplanningStrategy(new SelectBestPlan());
		
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
				VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, "input/freight/algorithm.xml");
				
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

			@Override
			public void prepareReplanning(ReplanningContext replanningContext) {
			}

			@Override
			public void finishReplanning() {
			}
		
		};
		
		vra.addModule(vraModule);
		return vra;
	}

	private static CarrierScoringFunctionFactory defineAndCreateScoringFunction(Carriers carriers, final Network network) {
		return new CarrierScoringFunctionFactory() {
			
			@Override
			public ScoringFunction createScoringFunction(Carrier carrier) {
				ScoringFunctionAccumulator sf = new ScoringFunctionAccumulator();
				DriversLegScoring driverLegScoring = new DriversLegScoring(carrier, network);
				sf.addScoringFunction(driverLegScoring);
				return sf;
			}
		};
	}

	private static void createInitialPlans(Carriers carriers, CarrierVehicleTypes vehicleTypes, Network network) {
		for(Carrier carrier : carriers.getCarriers().values()){
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
			VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, "input/freight/initialPlanAlgorithm.xml");
			
			//solve problem
			Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();
			
			//get best 
			VehicleRoutingProblemSolution solution = Solutions.getBest(solutions);
			
			SolutionPlotter.plotSolutionAsPNG(vrp, solution, "input/freight/sol_"+System.currentTimeMillis()+".png", "sol");
			
			//create carrierPlan from solution
			CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);
			
			//route plan (currently jsprit does not memorizes the routes, thus route the plan)
			NetworkRouter.routePlan(plan, netBasedTransportCosts);
			
			//add plan to carrier and select it
			carrier.setSelectedPlan(plan);
		}
		
	}

}
