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
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.replanning.modules.ReRouteVehicles;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;
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

	public static void runFreight(final Scenario scenario, final Carriers carriers) {

		final Config config = scenario.getConfig();
		final Controler controler = new Controler(scenario);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		/* Create scoring function factory. */
		CarrierScoringFunctionFactory scoringFunctionFactory = new CarrierScoringFunctionFactory() {

			@Override
			public ScoringFunction createScoringFunction(Carrier carrier) {
				SumScoringFunction sum = new SumScoringFunction();

				/* Add leg and money scoring. */
				CharyparNagelScoringParameters params = new CharyparNagelScoringParameters.Builder(config.planCalcScore(), config.planCalcScore().getScoringParameters(null), config.scenario()).build();
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
			public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {

				GenericStrategyManager<CarrierPlan, Carrier> strategyManager = new GenericStrategyManager<CarrierPlan, Carrier>() ;

				/* Create a basic ReRouting module. */
				TravelTime travelTime = controler.getLinkTravelTimes();
				TravelDisutility travelCost = ControlerDefaults.createDefaultTravelDisutilityFactory(scenario).createTravelDisutility(travelTime);
				GenericPlanStrategyImpl<CarrierPlan, Carrier> rerouteStrategy = new GenericPlanStrategyImpl<CarrierPlan, Carrier>( new BestPlanSelector<CarrierPlan, Carrier>()) ;

				LeastCostPathCalculator router = controler.getLeastCostPathCalculatorFactory().createPathCalculator(scenario.getNetwork(), travelCost, travelTime);
				GenericPlanStrategyModule<CarrierPlan> rerouteModule = new ReRouteVehicles(router , scenario.getNetwork(), travelTime);
				rerouteStrategy.addStrategyModule( rerouteModule );
				strategyManager.addStrategy(rerouteStrategy, null, 0.1);

				/* Alternative: do nothing, but select best plan. */
				GenericPlanStrategy<CarrierPlan, Carrier> selectBestStrategy = new GenericPlanStrategyImpl<CarrierPlan, Carrier>( new BestPlanSelector<CarrierPlan, Carrier>() ) ;
				strategyManager.addStrategy(selectBestStrategy, null, 0.9);

				return strategyManager;
			}
		};

		CarrierModule carrierControlerListener = new CarrierModule(carriers, strategyManagerFactory, scoringFunctionFactory);
		carrierControlerListener.setPhysicallyEnforceTimeWindowBeginnings(false);

		controler.addOverridingModule(carrierControlerListener);
		controler.run();
	}

}
