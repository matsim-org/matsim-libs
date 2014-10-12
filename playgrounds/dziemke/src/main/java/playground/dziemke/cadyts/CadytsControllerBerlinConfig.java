/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.dziemke.cadyts;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.cadyts.general.ExpBetaPlanChangerWithCadytsPlanRegistration;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class CadytsControllerBerlinConfig {
	private final static Logger log = Logger.getLogger(CadytsControllerBerlinConfig.class);
	
	public static void main(String[] args) {
		String configFile = "D:/Workspace/container/demand/input/config/config-new.xml";
		final Config config = ConfigUtils.loadConfig(configFile);
		
		// strategy
		StrategySettings strategySetinngs = new StrategySettings(Id.create(2, StrategySettings.class));
		strategySetinngs.setModuleName("cadytsCar") ;
		strategySetinngs.setProbability(1.0) ;
		config.strategy().addStrategySettings(strategySetinngs);
		
		// start controller
		final Controler controler = new Controler(config);
		
		// cadytsContext (and cadytsCarConfigGroup)
		final CadytsContext cContext = new CadytsContext(controler.getConfig());
		// CadytsContext generates new CadytsCarConfigGroup with name "cadytsCar"
		controler.addControlerListener(cContext);
		
		// new plan strategy
		controler.addPlanStrategyFactory("cadytsCar", new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager) {
				//return new PlanStrategyImpl(new CadytsExtendedExpBetaPlanChanger(
				return new PlanStrategyImpl(new ExpBetaPlanChangerWithCadytsPlanRegistration(
						scenario.getConfig().planCalcScore().getBrainExpBeta(), cContext));
			}
		} ) ;
		
		// scoring function
		final CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(config.planCalcScore());
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				
				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				//final CadytsCarScoring scoringFunction = new CadytsCarScoring(plan, config, cContext);
				final CadytsScoring scoringFunction = new CadytsScoring(person.getSelectedPlan(), config, cContext);
				final double cadytsScoringWeight = 0.0;
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction );

				return scoringFunctionAccumulator;
			}
		}) ;
		
		// zero scoring function
//		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
//			@Override
//			public ScoringFunction createNewScoringFunction(Plan plan) {
//				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
//				return scoringFunctionAccumulator;
//			}
//		});
		
		controler.run();
	}
}
