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
package playground.imob.feathers2asMatsimPlanStrategy;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.GenericPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nagel
 *
 */
class Main {

	final static String FEATHERS2 = "feathers2";

	public static void main(String[] args) {

		// loading and modifying the config:
		Config config ; 
		if ( args.length > 0 ) { 
			config = ConfigUtils.loadConfig( args[0] ) ;
		} else {
			throw new RuntimeException("needs argument config.xml") ;
		}
		
		// request FEATHERS2 as a PlanStrategy (it is added to the controler further below):
		StrategySettings stratSets = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) ) ;
		stratSets.setStrategyName( FEATHERS2 );
		stratSets.setWeight(0.1);
		config.strategy().addStrategySettings(stratSets);
		
		// loading the scenario:
		final Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		// loading and modifying the controler:
		final Controler ctrl = new Controler( scenario ) ;
		
		// generate the FEATHERS adapter class:
		final FeathersModule feathers2 = new FeathersModule() ;
		
		// make it an events handler (so it can listen to events; a different solution may be desired here)
		ctrl.getEvents().addHandler(feathers2);
		
		// add it as a PlanStrategy:
		final javax.inject.Provider<PlanStrategy> planStrategyFactory = new javax.inject.Provider<PlanStrategy>() {
			@Override
			public PlanStrategy get() {
				GenericPlanSelector<Plan, Person> planSelector = new RandomPlanSelector<>() ;
				PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(planSelector) ;
				PlanStrategyModule module = new PlanStrategyModule() {
					@Override
					public void prepareReplanning(ReplanningContext replanningContext) {
					}
					@Override
					public void handlePlan(Plan plan) {
						// the following should (test!!!) only retain the plan we are looking at:
						Person person = scenario.getPopulation().getPersons().get( plan.getPerson() ) ;
						List<Plan> planToKeep = new ArrayList<>() ;
						planToKeep.add( plan ) ;
						person.getPlans().retainAll( planToKeep ) ;

						PopulationFactory pf = scenario.getPopulation().getFactory() ;

						// modify plan by feathers:
						plan = feathers2.createPlan(  plan.getPerson() , pf ) ;
					}
					@Override
					public void finishReplanning() {
					}
				} ;
				builder.addStrategyModule(module);
				return builder.build() ;
			}
		};
		ctrl.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding(FEATHERS2).toProvider(planStrategyFactory);
			}
		});

		// running the controler:
		ctrl.run() ;
		
	}

}
