/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package tutorial.programming.downsamplePopulation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author kn
 */
class RunPopulationDownsamplingExample {

	void run(final String[] args) {
		String inputPopFilename = null ;
		String outputPopFilename = null ;
		String netFilename = null ;
				
		if ( args!=null ) {
			if ( !(args.length==2 || args.length==3) ) {
				System.err.println( "Usage: cmd inputPop.xml.gz outputPop.xml.gz [network.xml.gz]");
			} else {
				inputPopFilename = args[0] ;
				outputPopFilename = args[1] ;
				if ( args.length==3 ) {
					netFilename = args[2] ;
				}
			}
		}
		
		
		Config config = ConfigUtils.createConfig() ;
		config.network().setInputFile( netFilename ) ;
		config.plans().setInputFile( inputPopFilename ) ;

		Population pop = ScenarioUtils.loadScenario(config).getPopulation() ;

		Population newPop = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation() ;
		for ( Person person : pop.getPersons().values() ) {
			if ( Math.random() < 0.1 ) {
				System.out.println("adding person...");
				newPop.addPerson(person);
			}
		}
		

		PopulationWriter popwriter = new PopulationWriter(newPop,ScenarioUtils.loadScenario(config).getNetwork()) ;
		popwriter.write( outputPopFilename ) ;

		System.out.println("done.");
	}

	public static void main(final String[] args) {
		RunPopulationDownsamplingExample app = new RunPopulationDownsamplingExample();
		app.run(args);
	}

}
