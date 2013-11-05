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
package tutorial.programming.multipleSubpopulations;

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PlanStrategyRegistrar;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author nagel
 *
 */
public class SubpopulationsExample {

	static enum MyPopulationTypes { normal, special } ;
	
	public static void main(String[] args) {
		
		// ### config stuff: ###

		Config config = ConfigUtils.createConfig() ;
		
		config.plans().setInputFile( "plans.xml" ); 
		config.plans().setSubpopulationAttributeName("abc");

		
		Random rnd = MatsimRandom.getLocalInstance() ;
		// (generate ids the same way git does: as random keys)
		
		// define strategies for "normal" population:
		{
			StrategySettings stratSets = new StrategySettings( new IdImpl(rnd.nextLong()) ) ;
			stratSets.setModuleName( PlanStrategyRegistrar.Names.TimeAllocationMutator.toString() );
			stratSets.setSubpopulation( MyPopulationTypes.normal.toString() );
			stratSets.setProbability(0.5);
			config.strategy().addStrategySettings(stratSets);
		}
		// define strategies for "special" population:
		{
			StrategySettings stratSets = new StrategySettings( new IdImpl(rnd.nextLong()) ) ;
			stratSets.setModuleName( PlanStrategyRegistrar.Names.TimeAllocationMutator.toString() );
			stratSets.setSubpopulation( MyPopulationTypes.normal.toString() );
			stratSets.setProbability(0.5);
			config.strategy().addStrategySettings(stratSets);
		}
		{
			StrategySettings stratSets = new StrategySettings( new IdImpl( rnd.nextLong()) ) ;
			stratSets.setModuleName( PlanStrategyRegistrar.Names.ReRoute.toString() );
			stratSets.setSubpopulation( MyPopulationTypes.normal.toString() );
			stratSets.setProbability(0.5);
			config.strategy().addStrategySettings(stratSets);
		}
		
		// ### scenario stuff: ###

		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		final Population population = scenario.getPopulation();
		ObjectAttributes attrs = population.getPersonAttributes() ;
		
		// assign the attributes to the persons:
		for ( Person person : population.getPersons().values() ) {
			if ( person.getId().toString().startsWith("Nagel")) {
				attrs.putAttribute( person.getId().toString(), config.plans().getSubpopulationAttributeName(),  MyPopulationTypes.special ) ;
			} else {
				attrs.putAttribute( person.getId().toString(), config.plans().getSubpopulationAttributeName(),  MyPopulationTypes.normal ) ;
			}
		}
		
		// ### run the controler: ###
		
		new Controler(scenario).run();
		
	}

}
