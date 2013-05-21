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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.car.CadytsPlanChanger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class CadytsController2 {
	// new
	private final static Logger log = Logger.getLogger(CadytsController2.class);
	
	// private final static boolean USE_BRUTE_FORCE = true;
	//

	public static void main(String[] args) {
		final double cadytsScoringWeight = 1.0;
		
		String configFile = args[0];
		final Config config = ConfigUtils.loadConfig(configFile) ;
		final Scenario scn = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scn);
		//Controler controler = new Controler(args);
		
		final CadytsContext cContext = new CadytsContext(controler.getConfig());
		controler.addControlerListener(cContext);
		
		// changed id to 3
		// before run_34_c: change id to 2
		StrategySettings stratSets = new StrategySettings(new IdImpl(2));
		stratSets.setModuleName("ccc");
		// before run_34_c: change probability from 1.0 to 0.9; set back before run_36_c
		stratSets.setProbability(1.0);
		controler.getConfig().strategy().addStrategySettings(stratSets);
		
		controler.addPlanStrategyFactory("ccc", new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager events2) {
				final CadytsPlanChanger planSelector = new CadytsPlanChanger(cContext);
				return new PlanStrategyImpl(planSelector);
			}
		});
		
		
		// 2013-05-20, before run_37: create new scoring function directly here
//		final CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(config.planCalcScore());
//		
//		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
//			@Override
//			public ScoringFunction createNewScoringFunction(Plan plan) {
//				
//				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
//				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
//				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
//				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
//
//				final CadytsCarScoring scoringFunction = new CadytsCarScoring(plan,config, cContext);
//				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
//				scoringFunctionAccumulator.addScoringFunction(scoringFunction );
//
//				return scoringFunctionAccumulator;
//			}
//		}) ;
		// end 2013-05-20
		
		
//		controler.addControlerListener(new KaiAnalysisListener());
		
		controler.run();
	}
}
