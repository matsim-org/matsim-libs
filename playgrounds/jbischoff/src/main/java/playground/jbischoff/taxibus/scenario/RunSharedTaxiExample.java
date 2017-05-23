/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jbischoff.taxibus.scenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxibus.run.configuration.*;
import org.matsim.core.config.*;
import org.matsim.core.controler.*;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.*;
import org.matsim.core.scoring.functions.*;

import playground.jbischoff.sharedTaxi.*;

/**
 * @author jbischoff
 *
 */
public class RunSharedTaxiExample {

	public static void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig(args[0], new TaxibusConfigGroup(), new DvrpConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
	
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		new TaxibusControlerCreator(controler).initiateTaxibusses();
		
		//Analysis code:
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
			bind(SharedTaxiTripAnalyzer.class).asEagerSingleton();
			addControlerListenerBinding().to(SharedTaxiContolerListener.class);
			bindScoringFunctionFactory().toInstance(new ScoringFunctionFactory() {
				
				@Override
				public ScoringFunction createNewScoringFunction(Person person) {
					SumScoringFunction sumScoringFunction = new SumScoringFunction();
					final ScoringParameters params =
							new ScoringParameters.Builder(scenario, person.getId()).build();
					sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
					sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, scenario.getNetwork()));
					sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
					sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
					
					return sumScoringFunction;
				}
			});
			}
		});
		controler.run();
	}
}
