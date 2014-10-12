/* *********************************************************************** *
 * project: org.matsim.*
 * DgController
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
package playground.dgrether;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.dgrether.signalsystems.sylvia.controler.DgSylviaConfig;
import playground.dgrether.signalsystems.sylvia.controler.DgSylviaControlerListenerFactory;


/**
 * @author dgrether
 *
 */
public class DgController {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig( args[0]) ;
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		PopulationFactory pf = scenario.getPopulation().getFactory() ;
		
		Population pop2 = ScenarioUtils.createScenario(config).getPopulation() ; // dummy population, see next ;
		
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			for ( int ii=0 ; ii<9 ; ii++ ) {
				Person newPerson = pf.createPerson( Id.create( person.getId() + "_" + ii, Person.class )) ;
				
				// deepcopy of person into newPerson
				
				// Can't we implement Person newPerson = PopulationUtils.deepCopy( person ) ???
				// (there may be a problem with the routes)
				
				pop2.addPerson(newPerson);
			}
		}
		
		Controler c = new Controler( scenario );
		c.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		DgSylviaConfig sylviaConfig = new DgSylviaConfig();
		final DgSylviaControlerListenerFactory signalsFactory = new DgSylviaControlerListenerFactory(sylviaConfig);
		signalsFactory.setAlwaysSameMobsimSeed(false);
		c.setSignalsControllerListenerFactory(signalsFactory);
		c.setOverwriteFiles(true);
		
		if ( false ) {
			IterationStartsListener strategyWeightsManager = new IterationStartsListener() {
				@Override
				public void notifyIterationStarts(IterationStartsEvent event) {

					GenericPlanStrategy<Plan, Person> strategy 
					= new PlanStrategyImpl(new ExpBetaPlanChanger(Double.NaN) );
					// (dummy strategy, just to get the type.  Not so great.  Not even sure if it will work. Ask MZ. Kai)

					String subpopulation= null ;
					// (I think this is just null. Kai)

					double newWeight = 1./event.getIteration() ;
					// (program function as you want/need)

					event.getControler().getStrategyManager().changeWeightOfStrategy(strategy, subpopulation, newWeight) ;
				}
			} ;
			c.addControlerListener(strategyWeightsManager);
		}
		
		c.run();
	}

}
