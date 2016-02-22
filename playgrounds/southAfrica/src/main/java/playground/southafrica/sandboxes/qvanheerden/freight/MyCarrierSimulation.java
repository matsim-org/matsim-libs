/* *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.southafrica.sandboxes.qvanheerden.freight;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.replanning.modules.ReRouteVehicles;
import org.matsim.contrib.freight.replanning.modules.TimeAllocationMutator;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.usecases.analysis.CarrierScoreStats;
import org.matsim.contrib.freight.usecases.analysis.LegHistogram;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.southafrica.utilities.Header;


public class MyCarrierSimulation {
	private static final Logger log = Logger.getLogger(MyCarrierSimulation.class);
	public static Scenario scenario;
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
		String changeEventsInputFile = args[6];

		Config config = ConfigUtils.createConfig();
//		config.addCoreModules();
		config.controler().setOutputDirectory("./output/");
		config.controler().setLastIteration(3);
		config.controler().setWriteEventsInterval(3);
//		config.planCalcScore().setMarginalUtilityOfMoney(100.);
//		config.planCalcScore().setMarginalUtlOfDistanceOther(-120.);
		config.network().setInputFile(networkFile);
//		config.strategy().setMaxAgentPlanMemorySize(5);
//		config.network().setTimeVariantNetwork(true);

		//Read network
		scenario = ScenarioUtils.loadScenario(config);
		//config.network().setInputFile(networkFile);
//		scenario.getConfig().network().setTimeVariantNetwork(true);
//		new MatsimNetworkReader(scenario).readFile(networkFile);
//		scenario.getConfig().network().setChangeEventInputFile(changeEventsInputFile);

		MyCarrierSimulation mcs = new MyCarrierSimulation();
//		MyCarrierSimulation.getNetworkChangeEvents(scenario, 0, 6, 15, 22);

		//read carriers and their capabilities
		Carriers carriers = new Carriers();
		new CarrierPlanXmlReaderV2(carriers).read(carrierPlanFile);

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(vehicleTypes).read(vehicleTypesFile);

		//assign them to their corresponding vehicles - carriers already have vehicles in the carrier plan file
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes);


		Controler controler = new Controler(scenario);

        //get replan strategy and scoring function factory
        CarrierPlanStrategyManagerFactory stratManFactory = mcs.createReplanStrategyFactory(vehicleTypes, controler);
        CarrierScoringFunctionFactory scoringFactory = mcs.createScoringFactory();

		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		CarrierModule carrierController = new CarrierModule(carriers, stratManFactory, scoringFactory);
		carrierController.setPhysicallyEnforceTimeWindowBeginnings(false);
		controler.addOverridingModule(carrierController);

		mcs.prepareFreightOutput(controler, carriers);

		controler.run();

		new CarrierPlanXmlWriterV2(carriers).write(scenario.getConfig().controler().getOutputDirectory() + "output_carriers.xml.gz") ;

		Header.printFooter();

	}

	public static void getNetworkChangeEvents(Scenario scenario, double amStart, double amEnd, double pmStart, double pmEnd) {
		Collection<NetworkChangeEvent> events = new ArrayList<NetworkChangeEvent>();

		NetworkChangeEventFactory cef = new NetworkChangeEventFactoryImpl();

		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			double speed = link.getFreespeed() ;
//			double speed = 0 ;
			double kmph = 0;
			final double threshold = kmph/3.6; //convert to m/s
			if ( speed > threshold ) {
				{//morning peak starts
					NetworkChangeEvent event = cef.createNetworkChangeEvent(amStart*3600.) ;
					event.addLink(link);
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  threshold ));
//					ni.addNetworkChangeEvent(event);
					((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
//					events.add(event);
				}
				{//morning peak ends
					NetworkChangeEvent event = cef.createNetworkChangeEvent(amEnd*3600.) ;
					event.addLink(link);
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  speed ));
//					ni.addNetworkChangeEvent(event);
//					((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
//					events.add(event);
				}
				{//afternoon peak starts
					NetworkChangeEvent event = cef.createNetworkChangeEvent(pmStart*3600.) ;
					event.addLink(link);
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  threshold ));
//					ni.addNetworkChangeEvent(event);
//					((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
//					events.add(event);
				}
				{//afternoon peak ends
					NetworkChangeEvent event = cef.createNetworkChangeEvent(pmEnd*3600.) ;
					event.addLink(link);
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  speed ));
//					ni.addNetworkChangeEvent(event);
					((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
//					events.add(event);
				}
			}
		}

	}

	public CarrierPlanStrategyManagerFactory createReplanStrategyFactory(final CarrierVehicleTypes types, final MatsimServices controler){
		// From KnFreight
		CarrierPlanStrategyManagerFactory stratManFactory = new CarrierPlanStrategyManagerFactory() {
			@Override
		public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
				TravelTime travelTimes = controler.getLinkTravelTimes() ;
				TravelDisutility travelCosts = ControlerDefaults.createDefaultTravelDisutilityFactory(scenario).createTravelDisutility(
						travelTimes );
				LeastCostPathCalculator router = controler.getLeastCostPathCalculatorFactory().createPathCalculator(scenario.getNetwork(),
						travelCosts, travelTimes) ;
				GenericStrategyManager<CarrierPlan, Carrier> mgr = new GenericStrategyManager<CarrierPlan, Carrier>() ;
				{
					GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<CarrierPlan, Carrier>(new RandomPlanSelector<CarrierPlan, Carrier>()) ;
					GenericPlanStrategyModule<CarrierPlan> module = new ReRouteVehicles( router, scenario.getNetwork(), travelTimes ) ;
					strategy.addStrategyModule(module);
					mgr.addStrategy(strategy, null, 0.7);
					mgr.addChangeRequest((int)(0.8*scenario.getConfig().controler().getLastIteration()), strategy, null, 0.);
				}
				{
					GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<CarrierPlan, Carrier>( new RandomPlanSelector<CarrierPlan, Carrier>() ) ;
					GenericPlanStrategyModule<CarrierPlan> module = new TimeAllocationMutator() ;
					strategy.addStrategyModule(module);
					mgr.addStrategy(strategy, null, 0.3 );
					mgr.addChangeRequest((int)(0.8*scenario.getConfig().controler().getLastIteration()), strategy, null, 0. );
				}
				{
//					GenericPlanStrategy<CarrierPlan> strategy = 
//							new SelectBestPlanAndOptimizeItsVehicleRouteFactory(scenario.getNetwork(), types, services.getLinkTravelTimes()).createStrategy() ;
//					mgr.addStrategy( strategy, null, 0.3 );
//					mgr.addChangeRequest((int)(0.8*scenario.getConfig().services().getLastIteration()), strategy, null, 0. );
				}
				{				
					// the strategy to solve the pickup-and-delivery problem during the iterations is gone for the time being.  enough other
					// things to figure out, I think.  kai
				}
				{
					GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<CarrierPlan, Carrier>( new BestPlanSelector<CarrierPlan, Carrier>() ) ;
					mgr.addStrategy( strategy, null, 0.01 ) ;
				}
				return mgr ;
			}
		};
		return stratManFactory;

	}

	public CarrierScoringFunctionFactory createScoringFactory(){
		CarrierScoringFunctionFactory scoringFunctionFactory = new CarrierScoringFunctionFactory() {
			@Override
			public ScoringFunction createScoringFunction(Carrier carrier) {
				SumScoringFunction sum = new SumScoringFunction() ;

				final LegScoring legScoringFunction = new CharyparNagelLegScoring(new CharyparNagelScoringParameters.Builder(scenario.getConfig().planCalcScore(), scenario.getConfig().planCalcScore().getScoringParameters(null), scenario.getConfig().scenario()).build(),
						scenario.getNetwork() );
				sum.addScoringFunction(legScoringFunction ) ;

//				final MoneyScoring moneyScoringFunction = new CharyparNagelMoneyScoring(new CharyparNagelScoringParameters(scenario.getConfig().planCalcScore()) );
//				sum.addScoringFunction( moneyScoringFunction ) ;
				return sum ;
			}

		};

		return scoringFunctionFactory;
	}

	//freight part from sschroeder/usecases/chessboard/RunPassengerAlongWithCarrier
	public void prepareFreightOutput(MatsimServices controler, final Carriers carriers) {
		final LegHistogram freightOnly = new LegHistogram(900);
        freightOnly.setPopulation(controler.getScenario().getPopulation());
		freightOnly.setInclPop(false);

		CarrierScoreStats scores = new CarrierScoreStats(carriers, "output/carrier_scores", true);

		controler.getEvents().addHandler(freightOnly);
		controler.addControlerListener(scores);
		controler.addControlerListener(new IterationEndsListener() {

			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				//write plans
				String dir = event.getServices().getControlerIO().getIterationPath(event.getIteration());
				new CarrierPlanXmlWriterV2(carriers).write(dir + "/" + event.getIteration() + ".carrierPlans.xml");

				//write stats
				freightOnly.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_freight.png");
				freightOnly.reset(event.getIteration());
			}
		}
		);
	}
}
