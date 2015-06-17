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
package basteleien;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.jsprit.VehicleTypeDependentRoadPricingCalculator;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoadPricingConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

import freightKt.RoadPricingReaderFreightXMLv1;

import java.io.File;

/**
 * @author nagel modified by kturner
 * Dies ist die allererste Version der auf "KNFreight4" basierenden Frachtimplementation
 */
/**
 * @author kt
 *
 */
public class BasteleienXmlTollReader {
//	private static final Logger log = Logger.getLogger(BasteleienXmlTollReader.class) ;
	
//Beginn Namesdefinition KT
	private static final String INPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Berlin_Szenario/" ;
	private static final String OUTPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Bastelei_MautReader/" ;
	
	private static final String NETFILENAME = INPUT_DIR + "network.xml" ;
	private static final String VEHTYPES = INPUT_DIR + "vehicleTypes.xml" ;
	
//	private static final String CARRIERS = INPUT_DIR + "one_carrier_kt.xml" ;
//	private static final String CARRIERS = INPUT_DIR + "carrierLEH_v2_withFleet_kt.xml" ;
	
	private static final String TOLLFILE = INPUT_DIR + "toll_distance_test_kt.xml" ;  //only used if "addingToll == true); kturner 07.08.2014
	

	
//Ende Namesdefinition KT
	
//	private static final boolean generatingDemandFromModel = false ; //Wenn True --> Fehler, da locationLinkId = null gesetzt wird (In: MethodsToGenerateServices.createServicesMethod2(services);
	
//	private static final boolean generatingCarrierPlansFromScratch = true ;  //was true, kt 23.7.14  
	
//	private static final boolean addingCongestion = false ;

//	private static final boolean usingWithinDayReScheduling = false ;
	
	private static final boolean addingToll = true;  //added, kt. 07.08.2014

	public static void main(String[] args) {
		
		createOutputDir();
		
		Config config = createConfig(args);
		
		CarrierVehicleTypes vehicleTypes = createVehicleTypes();
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		System.out.println("##### Szenraio erstellt. Weiter gehts");
		
		generateCarrierPlans(scenario.getNetwork(), vehicleTypes, config);
		
		System.out.println("##### Ende Main erreicht... habe fertig");

	} // End Main


	//Ergänzung kt: 1.8.2014 Erstellt das OUTPUT-Verzeichnis. Falls es bereits exisitert, geschieht nichts
	private static void createOutputDir() {
		File file = new File(OUTPUT_DIR);
		System.out.println("Verzeichnis " + OUTPUT_DIR + "erstellt: "+ file.mkdirs());	
	}


//	private static Carriers createCarriers(CarrierVehicleTypes vehicleTypes) {
//		Carriers carriers = new Carriers() ;
//		new CarrierPlanXmlReaderV2(carriers).read(CARRIERS) ;
//
//		// assign vehicle types to the carriers (who already have their vehicles (??)):
//		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;
//		return carriers;
//	}


	private static CarrierVehicleTypes createVehicleTypes() {
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader(vehicleTypes).read(VEHTYPES) ;
		return vehicleTypes;
	}
	private static Config createConfig(String[] args) {
		Config config = ConfigUtils.createConfig() ;
		
		if ((args == null) || (args.length == 0)) {
			config.controler().setOutputDirectory(OUTPUT_DIR);
		} else {
			System.out.println( "args[0]:" + args[0] );
			config.controler().setOutputDirectory( args[0]+"/" );
		}
		config.controler().setLastIteration(0);							//Anzahl der Iterationen
		
		config.network().setInputFile(NETFILENAME);
		
		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration );
		
		config.vspExperimental().setVspDefaultsCheckingLevel(	VspExperimentalConfigGroup.WARN);
		return config;
	}  //End createConfig
	
	
	/*
	 * Anm.: KT, 23.07.14
	 * 
	 */
	private static void generateCarrierPlans(Network network, CarrierVehicleTypes vehicleTypes, Config config) {
		final Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance( network, vehicleTypes.getVehicleTypes().values() );
//		netBuilder.setBaseTravelTimeAndDisutility(travelTime, travelDisutility) ;
		netBuilder.setTimeSliceWidth(1800) ; // !!!!, otherwise it will not do anything.

		if (addingToll){		 //Added, KT, 07.08.2014
			System.out.println("#### Und nun in die Mautgeschichte einsteigen");
			generateRoadPricingCalculator(netBuilder, config);
		}
		

		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build() ;

//		for ( Carrier carrier : carriers.getCarriers().values() ) {
//
//			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder( carrier, network ) ;
//			vrpBuilder.setRoutingCost(netBasedCosts) ;
//			VehicleRoutingProblem problem = vrpBuilder.build() ;
//
////			VehicleRoutingAlgorithm algorithm = VehicleRoutingAlgorithms.readAndCreateAlgorithm(problem,ALGORITHM);
//						VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
//
//			VehicleRoutingProblemSolution solution = Solutions.bestOf(algorithm.searchSolutions());
//			CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution) ;
//
//			NetworkRouter.routePlan(newPlan,netBasedCosts) ;
//			// (maybe not optimal, but since re-routing is a matsim strategy, 
//			// certainly ok as initial solution)
//
//			carrier.setSelectedPlan(newPlan) ;
//
//		}
	}


	/*
	 * KT, 07.08.2014
	 * Hinzufügen des RoadPricing-Calculators --> Toll wird enabled	
	 */
	private static void generateRoadPricingCalculator(final Builder netBuilder, final Config config) {
//		VehicleTypeDependentRoadPricingCalculator rpCalculator = new VehicleTypeDependentRoadPricingCalculator();
//		RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
//		Id linkIdMaut = new IdImpl("25844");  //bisher nur für den einen Link
//		scheme.setType("distance");
//		scheme.addLinkCost(linkIdMaut, 0.0, 24.0*3600, 0.1); //Aktuell noch 0-14 Uhr und extrem hohe Maut!
//
//		rpCalculator.addPricingScheme("heavy26t", scheme );  //bisher nur für die 26t
//		netBuilder.setRoadPricingCalculator(rpCalculator);
		
		config.scenario().addParam("useRoadpricing", "true");
		config.roadpricing().setTollLinksFile(TOLLFILE);
		
		final RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
		RoadPricingReaderFreightXMLv1 rpReader = new RoadPricingReaderFreightXMLv1(scheme);
		try {
			RoadPricingConfigGroup rpConfig = (RoadPricingConfigGroup) config.getModule(RoadPricingConfigGroup.GROUP_NAME) ;
			rpReader.parse(rpConfig.getTollLinksFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		VehicleTypeDependentRoadPricingCalculator rpCalculator = new VehicleTypeDependentRoadPricingCalculator();

		rpCalculator.addPricingScheme("heavy26t", scheme );  //bisher nur für die 26t
		System.out.println(rpCalculator.getSchemes().get(new IdImpl("heavy26t")).size());		
		netBuilder.setRoadPricingCalculator(rpCalculator);
	}


	/*
	 * Anm.: KT, 23.07.14
	 * 
	 */
//	private static CarrierPlanStrategyManagerFactory createMyStrategyManager(final Scenario scenario, final Controler controler) {
//			return new CarrierPlanStrategyManagerFactory() {
//				@Override
//				public GenericStrategyManager<CarrierPlan> createStrategyManager() {
//					TravelTime travelTimes = controler.getLinkTravelTimes() ;
//					TravelDisutility travelDisutility = ControlerDefaults.createDefaultTravelDisutilityFactory(scenario).createTravelDisutility( 
//							travelTimes , scenario.getConfig().planCalcScore() );
//					LeastCostPathCalculator router = controler.getLeastCostPathCalculatorFactory().createPathCalculator(scenario.getNetwork(), 
//							travelDisutility, travelTimes) ;
//					
//					GenericStrategyManager<CarrierPlan> mgr = new GenericStrategyManager<CarrierPlan>() ;
//					{	
//						GenericPlanStrategyImpl<CarrierPlan> strategy = new GenericPlanStrategyImpl<CarrierPlan>(new RandomPlanSelector<CarrierPlan>()) ;
//						GenericPlanStrategyModule<CarrierPlan> module = new ReRouteVehicles( router, scenario.getNetwork(), travelTimes ) ;
//						strategy.addStrategyModule(module);
//						mgr.addStrategy(strategy, null, 0.1);
//						mgr.addChangeRequest((int)(0.8*scenario.getConfig().controler().getLastIteration()), strategy, null, 0.);
//					}
//					{
//						GenericPlanStrategyImpl<CarrierPlan> strategy = new GenericPlanStrategyImpl<CarrierPlan>( new BestPlanSelector<CarrierPlan>() ) ;
//						GenericPlanStrategyModule<CarrierPlan> module = new TimeAllocationMutator() ;
//						strategy.addStrategyModule(module);
//						mgr.addStrategy(strategy, null, 0.9 );
//						mgr.addChangeRequest((int)(0.8*scenario.getConfig().controler().getLastIteration()), strategy, null, 0. );
//					}
//					{
//						GenericPlanStrategyImpl<CarrierPlan> strategy = new GenericPlanStrategyImpl<CarrierPlan>( new BestPlanSelector<CarrierPlan>() ) ;
//						mgr.addStrategy( strategy, null, 0.01 ) ;
//					}
//					return mgr ;
//				}
//			};
//		}
//
//
//	private static CarrierScoringFunctionFactory createMyScoringFunction(final Scenario scenario) {
//		return new CarrierScoringFunctionFactory() {
//			@Override
//			public ScoringFunction createScoringFunction(final Carrier carrier) {
//				SumScoringFunction sum = new SumScoringFunction() ;
//				// yyyyyy I am almost sure that we better use separate scoring functions for carriers. kai, oct'13
//				sum.addScoringFunction(new CharyparNagelLegScoring(new CharyparNagelScoringParameters(scenario.getConfig().planCalcScore()), 
//						scenario.getNetwork() ) ) ;
//				sum.addScoringFunction( new CharyparNagelMoneyScoring(new CharyparNagelScoringParameters(scenario.getConfig().planCalcScore()) ) ) ;
//				ActivityScoring scoringFunction = new ActivityScoring() {
//					private double score = 0. ;
//					private final double margUtlOfTime_s = scenario.getConfig().planCalcScore().getPerforming_utils_hr()/3600. ;
//					// yyyyyy signs???
//					// yyyyyy do not use person params!!!
//					@Override
//					public void finish() {
//					}
//					@Override
//					public double getScore() {
//						return this.score ;
//					}
//					@Override
//					public void handleFirstActivity(Activity act) {
//						// no penalty for everything that is before the first act (people don't work)
//					}
//					@Override
//					public void handleActivity(Activity activity) {
//						
//						if (activity instanceof FreightActivity) {
//							FreightActivity act = (FreightActivity) activity;
//							// deduct score for the time spent at the facility:
//							final double actStartTime = act.getStartTime();
//							final double actEndTime = act.getEndTime();
//							score -= (actEndTime - actStartTime) * this.margUtlOfTime_s ;
//		
//							/*
//							CarrierService matchedService = null ;
//							for ( CarrierService service : carrier.getServices() ) {
//								if ( act.getLinkId().equals( service.getLocationLinkId() ) ) {
//									matchedService = service ;
//									break ;
//								}
//								// yy would be more efficient with a pre-fabricated lookup table. kai, nov'13
//								// yyyyyy there may be multiple services on the same link, which which case this will not function correctly. kai, nov'13
//							}
//							
//							final double windowStartTime = matchedService.getServiceStartTimeWindow().getStart();
//							final double windowEndTime = matchedService.getServiceStartTimeWindow().getEnd();
//							*/
//							
//							// fixed that for you. :) michaz
//							final double windowStartTime = act.getTimeWindow().getStart();
//							final double windowEndTime = act.getTimeWindow().getEnd();
//							
//							final double penalty = 18/3600. ; // per second!
//							if ( actStartTime < windowStartTime ) {
//								score -= penalty * ( windowStartTime - actStartTime ) ;
//								// mobsim could let them wait ... but this is also not implemented for regular activities. kai, nov'13
//							}
//							if ( windowEndTime < actEndTime ) {
//								score -= penalty * ( actEndTime - windowEndTime ) ;
//							}
//							// (note: provide penalties that work with a gradient to help the evol algo. kai, nov'13)
//							
//						} else {
//							log.warn("Carrier activities which are not FreightActivities are not scored here") ;
//						}
//						
//						
//						
//					}
//					@Override
//					public void handleLastActivity(Activity act) {
//						// no penalty for everything that is after the last act (people don't work)
//					}} ;
//				sum.addScoringFunction(scoringFunction); ;
//				return sum ;
//			}
//	
//		};
//	}

}
