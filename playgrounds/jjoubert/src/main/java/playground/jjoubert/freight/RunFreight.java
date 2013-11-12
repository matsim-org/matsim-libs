/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.jjoubert.freight;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.carrier.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierController;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategy;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategyManager;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategyModule;
import org.matsim.contrib.freight.replanning.modules.ReRouteVehicles;
import org.matsim.contrib.freight.replanning.selectors.SelectBestPlan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;
import org.matsim.core.scoring.SumScoringFunction.MoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

final class RunFreight {

	/**
	 * @param args
	 */
	public static void runFreight(final Scenario scenario, final Carriers carriers) {

		final Config config = scenario.getConfig();
		final Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		
		/* Create scoring function factory. */
		CarrierScoringFunctionFactory scoringFunctionFactory = new CarrierScoringFunctionFactory() {
			
			@Override
			public ScoringFunction createScoringFunction(Carrier carrier) {
				SumScoringFunction sum = new SumScoringFunction();
				
				/* Add leg and money scoring. */
				CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(config.planCalcScore());
				final LegScoring legScoringFunction = new CharyparNagelLegScoring(params , scenario.getNetwork());
				final MoneyScoring moneyScoringFunction = new CharyparNagelMoneyScoring(params);
				sum.addScoringFunction(legScoringFunction);
				sum.addScoringFunction(moneyScoringFunction);
				
				return sum;
			}
		};
		
		/* Create strategy manager, i.e. replanning modules. */
		CarrierPlanStrategyManagerFactory strategyManagerFactory = new CarrierPlanStrategyManagerFactory() {
			
			@Override
			public CarrierReplanningStrategyManager createStrategyManager(
					Controler controler) {
				
				CarrierReplanningStrategyManager strategyManager = new CarrierReplanningStrategyManager();
				
				/* Create a basic ReRouting module. */
				TravelTime travelTime = controler.getLinkTravelTimes();
				TravelDisutility travelCost = ControlerDefaults.createDefaultTravelDisutilityFactory(scenario).createTravelDisutility(travelTime, config.planCalcScore());
				CarrierReplanningStrategy rerouteStrategy = new CarrierReplanningStrategy(new SelectBestPlan());
				LeastCostPathCalculator router = controler.getLeastCostPathCalculatorFactory().createPathCalculator(scenario.getNetwork(), travelCost, travelTime);
				CarrierReplanningStrategyModule rerouteModule = new ReRouteVehicles(router , scenario.getNetwork(), travelTime);
				rerouteStrategy.addModule(rerouteModule);
				strategyManager.addStrategy(rerouteStrategy, 0.1);
				
				/* Alternative: do nothing, but select best plan. */
				CarrierReplanningStrategy selectBestStrategy = new CarrierReplanningStrategy(new SelectBestPlan());
				strategyManager.addStrategy(selectBestStrategy, 0.9);
				
				return strategyManager;
			}
		};
		
		CarrierController carrierControlerListener = new CarrierController(carriers, strategyManagerFactory, scoringFunctionFactory);
		carrierControlerListener.setEnableWithinDayActivityReScheduling(false);
		
		controler.addControlerListener(carrierControlerListener);
		controler.run();
	}

}
