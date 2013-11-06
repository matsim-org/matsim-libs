/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.southafrica.kai.freight;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.carrier.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierController;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategy;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategyManagerI;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategyManagerKai;
import org.matsim.contrib.freight.replanning.modules.ReRouteVehicles;
import org.matsim.contrib.freight.replanning.modules.TimeAllocationMutator;
import org.matsim.contrib.freight.replanning.selectors.SelectBestPlan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.GenericPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.ActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

/**
 * This is just a template to get a very general feel of the freight stuff from Stephan Schröder (and, in part, Michael Zilske).  It just compiles ...
 * 
 * @author nagel
 */
final class KNFreight3 {

	public static void run( final Scenario scenario, final Carriers carriers ) {
		final Config config = scenario.getConfig() ;
		
		CarrierScoringFunctionFactory scoringFunctionFactory = new CarrierScoringFunctionFactory() {
			@Override
			public ScoringFunction createScoringFunction(final Carrier carrier) {
				SumScoringFunction sum = new SumScoringFunction() ;
				// yyyyyy I am almost sure that we better use separate scoring functions for carriers. kai, oct'13
				sum.addScoringFunction(new CharyparNagelLegScoring(new CharyparNagelScoringParameters(config.planCalcScore()), 
						scenario.getNetwork() ) ) ;
				sum.addScoringFunction( new CharyparNagelMoneyScoring(new CharyparNagelScoringParameters(config.planCalcScore()) ) ) ;
				ActivityScoring scoringFunction = new ActivityScoring() {
					private double score = 0. ;
					private final double margUtlOfTime_s = config.planCalcScore().getPerforming_utils_hr()/3600. ;
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
					public void reset() {
						score = 0. ;
					}
					@Override
					public void handleFirstActivity(Activity act) {
						// no penalty for everything that is before the first act (people don't work)
					}
					@Override
					public void handleActivity(Activity act) {
						// deduct score for the time spent at the facility:
						final double actStartTime = act.getStartTime();
						final double actEndTime = act.getEndTime();
						score -= (actEndTime - actStartTime) * this.margUtlOfTime_s ;

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
						final double penalty = 18/3600. ; // per second!
						if ( actStartTime < windowStartTime ) {
							score -= penalty * ( windowStartTime - actStartTime ) ;
							// mobsim could let them wait ... but this is also not implemented for regular activities. kai, nov'13
						}
						if ( windowEndTime < actEndTime ) {
							score -= penalty * ( actEndTime - windowEndTime ) ;
						}
						// (note: provide penalties that work with a gradient to help the evol algo. kai, nov'13)
						
					}
					@Override
					public void handleLastActivity(Activity act) {
						// no penalty for everything that is after the last act (people don't work)
					}} ;
				sum.addScoringFunction(scoringFunction); ;
				return sum ;
			}

		};

		final Controler ctrl = new Controler( scenario ) ;
		ctrl.setOverwriteFiles(true);

		CarrierPlanStrategyManagerFactory strategyManagerFactory  = new CarrierPlanStrategyManagerFactory() {
			@Override
			public CarrierReplanningStrategyManagerI createStrategyManager(Controler controler) {
				TravelTime travelTimes = controler.getLinkTravelTimes() ;
				TravelDisutility travelCosts = ControlerUtils.createDefaultTravelDisutilityFactory(scenario).createTravelDisutility( 
						travelTimes , config.planCalcScore() );
				LeastCostPathCalculator router = ctrl.getLeastCostPathCalculatorFactory().createPathCalculator(scenario.getNetwork(), 
						travelCosts, travelTimes) ;
				CarrierReplanningStrategyManagerKai manager = new CarrierReplanningStrategyManagerKai() ;
				{
					CarrierReplanningStrategy strategy = new CarrierReplanningStrategy( new SelectBestPlan() ) ;
					strategy.addModule(new ReRouteVehicles(router, scenario.getNetwork(), travelTimes));
					manager.addStrategy(strategy, 0.3);
					manager.addChangeRequest((int)(0.8*config.controler().getLastIteration()), strategy, 0.0);
				}
//				{
//					CarrierReplanningStrategy strategy = new CarrierReplanningStrategy( new SelectBestPlan() ) ;
//					CarrierReplanningStrategyModule module = new SolvePickupAndDeliveryProblem(scenario.getNetwork()) ;
//					strategy.addModule(module) ;
//					manager.addStrategy(strategy,0.1) ;
//				}
				{
					CarrierReplanningStrategy strategy = new CarrierReplanningStrategy( new BestPlanSelector<CarrierPlan>() ) ;
					strategy.addModule( new TimeAllocationMutator() ) ;
					manager.addStrategy(strategy, 0.9);
					manager.addChangeRequest((int)(0.8*config.controler().getLastIteration()), strategy, 0.0);
				}
				{
					CarrierReplanningStrategy strategy = new CarrierReplanningStrategy( new SelectBestPlan() ) ;
					manager.addStrategy( strategy, 0.01 ) ;
				}
				
				
				GenericStrategyManager<CarrierPlan> mgr = new GenericStrategyManager<CarrierPlan>() ;
				{	
					GenericPlanStrategyImpl<CarrierPlan> strategy = new GenericPlanStrategyImpl<CarrierPlan>(new RandomPlanSelector<CarrierPlan>()) ;
					GenericPlanStrategyModule<CarrierPlan> module = new ReRouteVehicles( router, scenario.getNetwork(), travelTimes ) ;
					strategy.addStrategyModule(module);
					mgr.addStrategy(strategy, null, 0.123);
					mgr.addChangeRequest(123, strategy, null, 0.231);
				}
				
				return manager ;
			}
		} ;
		
		CarrierController listener = new CarrierController(carriers, strategyManagerFactory, scoringFunctionFactory ) ;
//		listener.setActivityTimesGivenBy(ActivityTimesGivenBy.durationOnly);
		listener.setEnableWithinDayActivityReScheduling(false);

		ctrl.addControlerListener(listener) ;
		ctrl.run();

	}

}
