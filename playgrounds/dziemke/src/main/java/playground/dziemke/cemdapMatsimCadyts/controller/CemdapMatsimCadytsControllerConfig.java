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

package playground.dziemke.cemdapMatsimCadyts.controller;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class CemdapMatsimCadytsControllerConfig {
//	private final static Logger log = Logger.getLogger(CemdapMatsimCadytsControllerConfig.class);
	
	public static void main(final String[] args) {
		final Config config = ConfigUtils.loadConfig(args[0]);
		
		// start controller
		final Controler controler = new Controler(config);
				
		// cadytsContext (and cadytsCarConfigGroup)
		final CadytsContext cContext = new CadytsContext(controler.getConfig());
		// CadytsContext generates new CadytsCarConfigGroup with name "cadytsCar"
		controler.addControlerListener(cContext);
				
		// plan strategy
        // not necessary anymore, just use normal ChangeExpBeta

//		controler.addPlanStrategyFactory("cadytsCar", new PlanStrategyFactory() {
//			@Override
//			public PlanStrategy get() {
//				return new PlanStrategyImpl(new ExpBetaPlanChangerWithCadytsPlanRegistration<Link>(
//						controler.getConfig().planCalcScore().getBrainExpBeta(), cContext));
//			}
//		});
		
		// scoring function
		final CharyparNagelScoringParameters params = CharyparNagelScoringParameters.getBuilder(config.planCalcScore()).createCharyparNagelScoringParameters();
				controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
					@Override
					public ScoringFunction createNewScoringFunction(Person person) {
						
						SumScoringFunction sumScoringFunction = new SumScoringFunction();
						sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
						sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
						sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

						final CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(person.getSelectedPlan(), config, cContext);
						final double cadytsScoringWeight = Double.parseDouble(args[1]);
						scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
						sumScoringFunction.addScoringFunction(scoringFunction );

						return sumScoringFunction;
					}
				}) ;

		controler.run();
	}
}
