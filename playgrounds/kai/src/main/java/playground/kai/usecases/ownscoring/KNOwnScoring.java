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
package playground.kai.usecases.ownscoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.core.scoring.functions.ScoringParameters;

/**
 * @author nagel
 *
 */
public class KNOwnScoring {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Config config = ConfigUtils.loadConfig( args[0] ) ;
		
		final Scenario scenario = ScenarioUtils.loadScenario( config  ) ;
		
		Controler controler = new Controler( scenario ) ;
		
		ScoringFunctionFactory scoringFunctionFactory = new ScoringFunctionFactory(){
			final ScoringParametersForPerson parametersForPerson = new SubpopulationScoringParameters( scenario );
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parametersForPerson.getScoringParameters( person );

				MySumScoringFunction sumScoringFunction = new MySumScoringFunction() ;
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, scenario.getNetwork()));
				sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				return sumScoringFunction ;
			}
		} ;
		controler.setScoringFunctionFactory(scoringFunctionFactory);
		
		controler.run() ;
	}

}
