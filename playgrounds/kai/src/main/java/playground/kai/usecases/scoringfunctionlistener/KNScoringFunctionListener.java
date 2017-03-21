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
package playground.kai.usecases.scoringfunctionlistener;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction.BasicScoring;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.core.scoring.functions.ScoringParameters;

import com.google.inject.Inject;

/**
 * @author nagel
 *
 */
public class KNScoringFunctionListener {

	public static void main(String[] args) {

		final Config config  = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration(0);


		final Scenario scenario  = ScenarioUtils.loadScenario( config ) ;

		final PopulationFactory pf = scenario.getPopulation().getFactory();
		Person person = pf.createPerson( Id.createPersonId("test") ) ;
		Plan plan = pf.createPlan() ;
		person.addPlan(plan) ;
		scenario.getPopulation().addPerson(person);


		Controler controler = new Controler( scenario ) ;

		controler.addControlerListener(new ScoringListener() {
			@Override
			public void notifyScoring(ScoringEvent event) {
				throw new RuntimeException("not implemented");
				// this needs to get the info from the scoringListener (below) and attach it to the agent plan somehow.
			}
		});

		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			private ScoringParametersForPerson parameters = new SubpopulationScoringParameters(scenario);
			private Network network;

			@Inject
			MyScoringFunctionListener listener;

			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parameters.getScoringParameters( person );

				MySumScoringFunction sumScoringFunction = new MySumScoringFunction(person, listener);
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, this.network));
				sumScoringFunction.addScoringFunction(new MyMoneyScoring(person));
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
				return sumScoringFunction;


			}

			class MyMoneyScoring implements BasicScoring {
				private Person person;

				public MyMoneyScoring(Person person) {
					this.person = person;
				}

				@Override
				public void finish() {
					listener.reportMoney(25., this.person.getId());
				}

				@Override
				public double getScore() {
					return 25. * 5;
				}

			}

		});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.bind(MyScoringFunctionListener.class).asEagerSingleton();
			}
		});

		controler.run();
	}
}
