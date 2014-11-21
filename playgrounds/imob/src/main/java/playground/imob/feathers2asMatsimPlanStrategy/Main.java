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

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.GenericPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
class Main {

	final static String FEATHERS2 = "feathers2";

	public static void main(String[] args) {

		Config config ; 
		if ( args.length > 0 ) { 
			config = ConfigUtils.loadConfig( args[0] ) ;
		} else {
			throw new RuntimeException("needs argument config.xml") ;
		}
		
		StrategySettings stratSets = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) ) ;
		stratSets.setModuleName( FEATHERS2 );
		stratSets.setProbability(0.1);
		config.strategy().addStrategySettings(stratSets);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		final Controler ctrl = new Controler( scenario ) ;
		
		final FeathersModule feathers2 = new FeathersModule() ;
		ctrl.getEvents().addHandler(feathers2);
		
		PlanStrategyFactory planStrategyFactory = new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(final Scenario scenario, EventsManager eventsManager) {
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
		} ;
		ctrl.addPlanStrategyFactory( FEATHERS2, planStrategyFactory);

		
		
	}

}
