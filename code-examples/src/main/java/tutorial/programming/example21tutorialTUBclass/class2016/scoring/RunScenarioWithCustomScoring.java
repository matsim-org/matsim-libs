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
package tutorial.programming.example21tutorialTUBclass.class2016.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoringParameters;


public class RunScenarioWithCustomScoring {


	public static void main(String[] args) {
		
		// This loads a default matsim config:
		Config config = ConfigUtils.loadConfig("input/ha2/ha2policyCaseConfig.xml");

		//Relative path locations must be relative to the project folder (both in the config and here)
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration(10);
		// This loads the scenario
		final Scenario scenario = ScenarioUtils.loadScenario(config) ;
		final KindergartenArrivalHandler kindergartenArrivalHandler = new KindergartenArrivalHandler();
		
		Controler controler = new Controler( scenario ) ;
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(kindergartenArrivalHandler);				
			}
		});;
		
		controler.addControlerListener(new IterationEndsListener() {
			
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				Logger.getLogger(getClass()).info("Kids in kindergarten 8142 :"+kindergartenArrivalHandler.kinder );
			}
		});
		
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {

			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				SumScoringFunction sumScoringFunction = new SumScoringFunction();

				// Score activities, legs, payments and being stuck
				// with the default MATSim scoring based on utility parameters in the config file.
				final ScoringParameters params =
						new ScoringParameters.Builder(scenario, person.getId()).build();
				sumScoringFunction.addScoringFunction(new KindergartenActivityScoring(person.getId(), kindergartenArrivalHandler));
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, scenario.getNetwork()));
				sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
				return sumScoringFunction;
							
			}

		});
		
		controler.run();

	}

}
