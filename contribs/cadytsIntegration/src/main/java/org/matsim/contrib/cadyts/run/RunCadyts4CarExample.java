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
package org.matsim.contrib.cadyts.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import javax.inject.Inject;

/**
 * Script-in-java to include cadyts into a matsim run.
 * <p/>
 * For the listing click on the class name above.
 * 
 * @author nagel
 *
 */
public class RunCadyts4CarExample {

	public static void main(String[] args) {
		final Config config = ConfigUtils.loadConfig( args[0], new CadytsConfigGroup() ) ;

		final Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		// ---

		final Controler controler = new Controler( scenario ) ;
		controler.addOverridingModule(new CadytsCarModule());

		// include cadyts into the plan scoring (this will add the cadyts corrections to the scores):
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Inject CadytsContext cadytsContext;
			@Inject CharyparNagelScoringParametersForPerson parameters;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final CharyparNagelScoringParameters params = parameters.getScoringParameters(person);
				
				SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
				scoringFunction.setWeightOfCadytsCorrection(30. * config.planCalcScore().getBrainExpBeta()) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction );

				return scoringFunctionAccumulator;
			}
		}) ;

		
		controler.run() ;
	}

}
