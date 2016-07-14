/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.southafrica.sandboxes.kai.freight;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.replanning.modules.ReRouteVehicles;
import org.matsim.contrib.freight.replanning.modules.TimeAllocationMutator;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.scoring.FreightActivity;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.*;
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
import org.matsim.core.scoring.SumScoringFunction.ActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.box.SchrimpfFactory;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.util.Solutions;

/**
 * @author nagel
 *
 */
public class KNFreight4 {
	private static final Logger log = Logger.getLogger(KNFreight4.class) ;

	private static final String MATSIM_SA = "/Users/Nagel/southafrica/MATSim-SA/" ;

	private static final String QVH_FREIGHT=MATSIM_SA+"/sandbox/qvanheerden/input/freight/" ;

	//	private static final String NETFILENAME=QVANHEERDEN_FREIGHT+"/scenarioFromWiki/network.xml" ;
	//	private static final String CARRIERS = QVANHEERDEN_FREIGHT+"/scenarioFromWiki/carrier.xml" ;
	//	private static final String VEHTYPES = QVANHEERDEN_FREIGHT+"/scenarioFromWiki/vehicleTypes.xml" ;
	//	private static final String ALGORITHM = QVANHEERDEN_FREIGHT+"/scenarioFromWiki/algorithm.xml" ;

	//	private static final String NETFILENAME = MATSIM_SA + "data/areas/nmbm/network/NMBM_Network_FullV7.xml.gz"  ;
	//	private static final String NETFILENAME = MATSIM_SA + "data/areas/nmbm/network/NMBM_Network_CleanV7.xml.gz"  ;
	private static final String NETFILENAME = QVH_FREIGHT + "network/merged-network-simplified.xml.gz" ;

	//	private static final String VEHTYPES = QVH_FREIGHT + "myGridSim/vehicleTypes.xml" ;
	private static final String VEHTYPES = "/Users/nagel/kairuns/nmbm-freight/inputs/kai/vehicleTypes.xml" ;

	//	private static final String CARRIERS = QVH_FREIGHT + "myGridSim/carrier.xml" ;
	//	private static final String CARRIERS = "/Users/nagel/freight-kairuns/one-truck/carriers.xml.gz" ;
	//	private static final String CARRIERS = "/Users/nagel/freight-kairuns/inputs/2013-11-30-08h54/carrier.xml" ;
	private static final String CARRIERS = "/Users/nagel/kairuns/nmbm-freight/inputs/kai/carrier.xml" ;


	private static final String ALGORITHM = QVH_FREIGHT + "myGridSim/initialPlanAlgorithm.xml" ;
	//	private static final String ALGORITHM = QVANHEERDEN_FREIGHT + "myGridSim/algorithm.xml" ;

	private static final boolean generatingDemandFromModel = false ; // template only; not filled with "meaning"

	private static final boolean generatingCarrierPlansFromScratch = true ;

	private static final boolean addingCongestion = false ;

	private static final boolean usingWithinDayReScheduling = false ;

	public static void main(String[] args) {

		for ( int ii=0 ; ii<args.length ; ii++ ) {
			System.out.println( args[ii] ) ;
		}
		//		System.exit(-1);;

		// ### config stuff: ###	

		Config config = createConfig(args);
		config.addConfigConsistencyChecker(new VspConfigConsistencyCheckerImpl());
		ControlerUtils.checkConfigConsistencyAndWriteToLog(config, "dump");
		//		System.exit(-1);

		// ### scenario stuff: ###

		Scenario scenario = ScenarioUtils.loadScenario(config);

		if ( addingCongestion ) {
			configureTimeDependentNetwork(scenario);
		}

		CarrierVehicleTypes vehicleTypes = createVehicleTypes();

		Carriers carriers = createCarriers(vehicleTypes);
		if ( generatingDemandFromModel ) {
			// this will over-write the shipments in the "carrier" data structure
			for ( Carrier carrier : carriers.getCarriers().values() ) {
				Collection<CarrierService> services = carrier.getServices() ;
				services.clear(); // clear away whatever may have been in there
				//				MethodsToGenerateServices.createServicesMethod1(services);
				MethodsToGenerateServices.createServicesMethod2(services);
			}
		}

		if ( generatingCarrierPlansFromScratch || generatingDemandFromModel ) {
			// (need to (re)generate carrier plans if demand has changed!)

			generateCarrierPlans(scenario.getNetwork(), carriers, vehicleTypes);
		}

		new CarrierPlanXmlWriterV2(carriers).write( config.controler().getOutputDirectory() + "plannedCarriers.xml") ;

		// ### simple runs: ###

		//		new Visualiser( config, scenario).visualizeLive(carriers) ;
		//		new Visualiser(config,scenario).makeMVI(carriers,"yourFolder/carrierMVI.mvi",1);

		// ### iterations: ###

		CarrierScoringFunctionFactory scoringFunctionFactory = KNFreight4.createMyScoringFunction(scenario);

		final Controler ctrl = new Controler( scenario ) ;

		PlanCalcScoreConfigGroup cnScoringGroup = ctrl.getConfig().planCalcScore() ;
		TravelTime timeCalculator = ctrl.getLinkTravelTimes() ;
		TravelDisutility trDisutil = ctrl.getTravelDisutilityFactory().createTravelDisutility(timeCalculator) ;

		CarrierPlanStrategyManagerFactory strategyManagerFactory  = KNFreight4.createMyStrategyManager(scenario, ctrl) ;
		ctrl.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
							OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		{
			CarrierModule listener = new CarrierModule(carriers, strategyManagerFactory, scoringFunctionFactory ) ;
			listener.setPhysicallyEnforceTimeWindowBeginnings(usingWithinDayReScheduling);
			ctrl.addOverridingModule(listener); ;
		}

		ctrl.run();

		// ### some final output: ###

		new CarrierPlanXmlWriterV2(carriers).write( config.controler().getOutputDirectory() + "output_carriers.xml.gz") ;

	}

	private static Config createConfig(String[] args) {
		Config config = ConfigUtils.createConfig() ;

		if ((args == null) || (args.length == 0)) {
			config.controler().setOutputDirectory("/Users/nagel/kairuns/nmbm-freight/output/");
		} else {
			System.out.println( "args[0]:" + args[0] );
			config.controler().setOutputDirectory( args[0]+"/" );
		}
		config.controler().setLastIteration(0);

		config.network().setInputFile(NETFILENAME);
		if ( addingCongestion ) {
			config.network().setTimeVariantNetwork(true);
		}

		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );

		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.warn );
		return config;
	}


	private static Carriers createCarriers(CarrierVehicleTypes vehicleTypes) {
		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReaderV2(carriers).read(CARRIERS) ;

		// assign vehicle types to the carriers (who already have their vehicles (??)):
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;
		return carriers;
	}


	private static CarrierVehicleTypes createVehicleTypes() {
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader(vehicleTypes).read(VEHTYPES) ;
		return vehicleTypes;
	}


	private static void generateCarrierPlans(Network network, Carriers carriers, CarrierVehicleTypes vehicleTypes) {
		final NetworkBasedTransportCosts.Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance( network, vehicleTypes.getVehicleTypes().values() );
		//		netBuilder.setBaseTravelTimeAndDisutility(travelTime, travelDisutility) ;
		netBuilder.setTimeSliceWidth(1800) ; // !!!!, otherwise it will not do anything.
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build() ;

		for ( Carrier carrier : carriers.getCarriers().values() ) {

			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder( carrier, network ) ;
			vrpBuilder.setRoutingCost(netBasedCosts) ;
			VehicleRoutingProblem problem = vrpBuilder.build() ;

			//			VehicleRoutingAlgorithm algorithm = VehicleRoutingAlgorithms.readAndCreateAlgorithm(problem,ALGORITHM);
			VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);

			VehicleRoutingProblemSolution solution = Solutions.bestOf(algorithm.searchSolutions());
			CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution) ;

			NetworkRouter.routePlan(newPlan,netBasedCosts) ;
			// (maybe not optimal, but since re-routing is a matsim strategy, 
			// certainly ok as initial solution)

			carrier.setSelectedPlan(newPlan) ;

		}
	}


	private static void configureTimeDependentNetwork(Scenario scenario) {
		NetworkChangeEventFactory cef = new NetworkChangeEventFactoryImpl() ;
		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			double speed = link.getFreespeed() ;
			final double threshold = 5./3.6;
			if ( speed > threshold ) {
				{
					NetworkChangeEvent event = cef.createNetworkChangeEvent(7.*3600.) ;
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  threshold/10 ));
					event.addLink(link);
					((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
				}
				{
					NetworkChangeEvent event = cef.createNetworkChangeEvent(11.5*3600.) ;
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  speed ));
					event.addLink(link);
					((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
				}
			}
		}
	}


	private static CarrierPlanStrategyManagerFactory createMyStrategyManager(final Scenario scenario, final MatsimServices controler) {
		return new CarrierPlanStrategyManagerFactory() {
			@Override
			public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
				TravelTime travelTimes = controler.getLinkTravelTimes() ;
				TravelDisutility travelDisutility = ControlerDefaults.createDefaultTravelDisutilityFactory(scenario).createTravelDisutility(
						travelTimes );
				LeastCostPathCalculator router = controler.getLeastCostPathCalculatorFactory().createPathCalculator(scenario.getNetwork(),
						travelDisutility, travelTimes) ;

				GenericStrategyManager<CarrierPlan, Carrier> mgr = new GenericStrategyManager<CarrierPlan, Carrier>() ;
				{	
					GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<CarrierPlan, Carrier>(new RandomPlanSelector<CarrierPlan, Carrier>()) ;
					GenericPlanStrategyModule<CarrierPlan> module = new ReRouteVehicles( router, scenario.getNetwork(), travelTimes ) ;
					strategy.addStrategyModule(module);
					mgr.addStrategy(strategy, null, 0.1);
					mgr.addChangeRequest((int)(0.8*scenario.getConfig().controler().getLastIteration()), strategy, null, 0.);
				}
				{
					GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<CarrierPlan, Carrier>( new BestPlanSelector<CarrierPlan, Carrier>() ) ;
					GenericPlanStrategyModule<CarrierPlan> module = new TimeAllocationMutator() ;
					strategy.addStrategyModule(module);
					mgr.addStrategy(strategy, null, 0.9 );
					mgr.addChangeRequest((int)(0.8*scenario.getConfig().controler().getLastIteration()), strategy, null, 0. );
				}
				{
					GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<CarrierPlan, Carrier>( new BestPlanSelector<CarrierPlan, Carrier>() ) ;
					mgr.addStrategy( strategy, null, 0.01 ) ;
				}
				return mgr ;
			}
		};
	}


	private static CarrierScoringFunctionFactory createMyScoringFunction(final Scenario scenario) {
		return new CarrierScoringFunctionFactory() {
			@Override
			public ScoringFunction createScoringFunction(final Carrier carrier) {
				SumScoringFunction sum = new SumScoringFunction() ;
				// yyyyyy I am almost sure that we better use separate scoring functions for carriers. kai, oct'13
				sum.addScoringFunction(new CharyparNagelLegScoring(new CharyparNagelScoringParameters.Builder(scenario.getConfig().planCalcScore(), scenario.getConfig().planCalcScore().getScoringParameters(null), scenario.getConfig().scenario()).build(),
						scenario.getNetwork() ) ) ;
				sum.addScoringFunction( new CharyparNagelMoneyScoring(new CharyparNagelScoringParameters.Builder(scenario.getConfig().planCalcScore(), scenario.getConfig().planCalcScore().getScoringParameters(null), scenario.getConfig().scenario()).build()) ) ;
				ActivityScoring scoringFunction = new ActivityScoring() {
					private double score = 0. ;
					private final double margUtlOfTime_s = scenario.getConfig().planCalcScore().getPerforming_utils_hr()/3600. ;
					// yyyyyy signs???
					// yyyyyy do not use person params!!!
					@Override
					public void finish() {
					}
					@Override
					public double getScore() {
						return this.score ;
					}
					@Override
					public void handleFirstActivity(Activity act) {
						// no penalty for everything that is before the first act (people don't work)
					}
					@Override
					public void handleActivity(Activity activity) {

						if (activity instanceof FreightActivity) {
							FreightActivity act = (FreightActivity) activity;
							// deduct score for the time spent at the facility:
							final double actStartTime = act.getStartTime();
							final double actEndTime = act.getEndTime();
							score -= (actEndTime - actStartTime) * this.margUtlOfTime_s ;

							/*
							CarrierService matchedService = null ;
							for ( CarrierService service : carrier.getServices() ) {
								if ( act.getLinkId().equals( service.getLocationLinkId() ) ) {
									matchedService = service ;
									break ;
								}
								// yy would be more efficient with a pre-fabricated lookup table. kai, nov'13
								// yyyyyy there may be multiple services on the same link, which which case this will not function correctly. kai, nov'13
							}

							final double windowStartTime = matchedService.getServiceStartTimeWindow().getStart();
							final double windowEndTime = matchedService.getServiceStartTimeWindow().getEnd();
							 */

							// fixed that for you. :) michaz
							final double windowStartTime = act.getTimeWindow().getStart();
							final double windowEndTime = act.getTimeWindow().getEnd();

							final double penalty = 18/3600. ; // per second!
							if ( actStartTime < windowStartTime ) {
								score -= penalty * ( windowStartTime - actStartTime ) ;
								// mobsim could let them wait ... but this is also not implemented for regular activities. kai, nov'13
							}
							if ( windowEndTime < actEndTime ) {
								score -= penalty * ( actEndTime - windowEndTime ) ;
							}
							// (note: provide penalties that work with a gradient to help the evol algo. kai, nov'13)

						} else {
							log.warn("Carrier activities which are not FreightActivities are not scored here") ;
						}



					}
					@Override
					public void handleLastActivity(Activity act) {
						// no penalty for everything that is after the last act (people don't work)
					}} ;
					sum.addScoringFunction(scoringFunction); ;
					return sum ;
			}

		};
	}

}
