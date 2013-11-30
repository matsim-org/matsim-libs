package playground.qvanheerden.freight;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierControlerListener;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategy;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategyManager;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategyModule;
import org.matsim.contrib.freight.replanning.modules.ReRouteVehicles;
import org.matsim.contrib.freight.replanning.selectors.SelectBestPlan;
import org.matsim.core.api.internal.MatsimManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;
import org.matsim.core.scoring.SumScoringFunction.MoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.southafrica.kai.freight.SolvePickupAndDeliveryProblem;
import playground.southafrica.utilities.Header;
import util.Solutions;
import basics.VehicleRoutingAlgorithm;
import basics.VehicleRoutingProblem;
import basics.VehicleRoutingProblemSolution;


public class MyCarrierSimulation {
	public static Scenario scenario;
	public static Config config;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(MyCarrierSimulation.class.toString(), args);

		String configFile = args[0];
		String networkFile = args[1];
		String carrierPlanFile = args[2];
		String vehicleTypesFile = args[3];
		String initialPlanAlgorithm = args[4];
		String algorithm = args[5];

		config = ConfigUtils.loadConfig(configFile);
		//		config.addCoreModules();
		//		config.controler().setFirstIteration(0);
		//		config.controler().setLastIteration(1);
		//		config.addQSimConfigGroup(new QSimConfigGroup());
		//		config.getQSimConfigGroup().setStartTime(0);
		//		config.getQSimConfigGroup().setEndTime(144000);
		//		config.getQSimConfigGroup().setSnapshotStyle("queue");
		//		config.getQSimConfigGroup().setSnapshotPeriod(10);
		//		
		//		config.controler().setMobsim("qsim");
		//		List<String> snap = new ArrayList<String>();
		//		snap.add("otfvis");
		//		config.controler().setSnapshotFormat(snap);


		//Read network
		scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile(networkFile);


		//read carriers and their capabilities
		Carriers carriers = new Carriers();
		new CarrierPlanXmlReaderV2(carriers).read(carrierPlanFile);

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(vehicleTypes).read(vehicleTypesFile);

		//assign them to their corresponding vehicles - carriers already have vehicles in the carrier plan file
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes);

		//get initial solution (using this from KnFreight2)
//		for ( Carrier carrier : carriers.getCarriers().values() ) {
//			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder( carrier, scenario.getNetwork() ) ;
//			NetworkBasedTransportCosts netBasedCosts =
//					NetworkBasedTransportCosts.Builder.newInstance( scenario.getNetwork()
//							, vehicleTypes.getVehicleTypes().values() ).build() ;
//			vrpBuilder.setRoutingCost(netBasedCosts) ;
//			VehicleRoutingProblem problem = vrpBuilder.build() ;
//
//			VehicleRoutingAlgorithm vra = algorithms.VehicleRoutingAlgorithms.readAndCreateAlgorithm(problem,initialPlanAlgorithm);
//
//			VehicleRoutingProblemSolution solution = Solutions.getBest(vra.searchSolutions());
//			CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution) ;
//
//			NetworkRouter.routePlan(newPlan,netBasedCosts) ;
//			// (maybe not optimal, but since re-routing is a matsim strategy, 
//			// certainly ok as initial solution)
//			carrier.setSelectedPlan(newPlan) ;
//
//		}
		
		//get replan strategy and scoring function factory
		MyCarrierSimulation mcs = new MyCarrierSimulation();
		CarrierPlanStrategyManagerFactory stratManFactory = mcs.createReplanStrategyFactory();
		CarrierScoringFunctionFactory scoringFactory = mcs.createScoringFactory();
		
		
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		
		CarrierControlerListener carrierController = new CarrierControlerListener(carriers, stratManFactory, scoringFactory);
		carrierController.setEnableWithinDayActivityReScheduling(false);
		controler.addControlerListener(carrierController);
		controler.run();

		Header.printFooter();

	}
	
	public CarrierPlanStrategyManagerFactory createReplanStrategyFactory(){
		// From KnFreight
		CarrierPlanStrategyManagerFactory stratManFactory = new CarrierPlanStrategyManagerFactory() {
			@Override
			public MatsimManager createStrategyManager(Controler controler) {
				TravelTime travelTimes = controler.getLinkTravelTimes() ;
				TravelDisutility travelCosts = ControlerDefaults.createDefaultTravelDisutilityFactory(scenario).createTravelDisutility( 
						travelTimes , config.planCalcScore() );
				LeastCostPathCalculator router = controler.getLeastCostPathCalculatorFactory().createPathCalculator(scenario.getNetwork(), 
						travelCosts, travelTimes) ;
				CarrierReplanningStrategyManager manager = new CarrierReplanningStrategyManager() ;
				{
					CarrierReplanningStrategy strategy = new CarrierReplanningStrategy( new SelectBestPlan() ) ;
					CarrierReplanningStrategyModule module = new ReRouteVehicles(router, scenario.getNetwork(), travelTimes) ;
					strategy.addModule(module);
					manager.addStrategy(strategy, 1.0);
				}
				{
					CarrierReplanningStrategy strategy = new CarrierReplanningStrategy( new SelectBestPlan() ) ;
					CarrierReplanningStrategyModule module = new SolvePickupAndDeliveryProblem(scenario.getNetwork()) ;
					strategy.addModule(module) ;
					manager.addStrategy(strategy,0) ;
				}
				{
					CarrierReplanningStrategy strategy = new CarrierReplanningStrategy( new SelectBestPlan() ) ;
					manager.addStrategy( strategy, 0 ) ;
				}
				return manager;
			}
		};
		return stratManFactory;
		
	}

	public CarrierScoringFunctionFactory createScoringFactory(){
		//From KnFreight
		CarrierScoringFunctionFactory scoringFunctionFactory = new CarrierScoringFunctionFactory() {
			@Override
			public ScoringFunction createScoringFunction(Carrier carrier) {
				SumScoringFunction sum = new SumScoringFunction() ;
				// yyyyyy I am almost sure that we better use separate scoring functions for carriers. kai, oct'13
				final LegScoring legScoringFunction = new CharyparNagelLegScoring(new CharyparNagelScoringParameters(config.planCalcScore()), 
						scenario.getNetwork() );
				sum.addScoringFunction(legScoringFunction ) ;
				final MoneyScoring moneyScoringFunction = new CharyparNagelMoneyScoring(new CharyparNagelScoringParameters(config.planCalcScore()) );
				sum.addScoringFunction( moneyScoringFunction ) ;
				return sum ;
			}

		};
		
		return scoringFunctionFactory;
	}
	
}
