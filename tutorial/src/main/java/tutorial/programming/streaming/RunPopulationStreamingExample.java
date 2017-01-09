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
package tutorial.programming.streaming;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Example for how to use the revised streaming API.  Streaming is no longer in the Population (as it was before), but in a special reader.
 * <p></p>
 * The class below was not tested; writing a regression test around it (and making in work if necessary) would be highly welcome.
 * 
 * @author nagel
 *
 */
final class RunPopulationStreamingExample {

	private static class RemoveUnselectedPlans implements PersonAlgorithm {
		@Override public void run(Person person) {
			Plan selectedPlan = person.getSelectedPlan() ;
			
			Collection<Plan> toBeRemoved = new ArrayList<>() ;
			for ( Plan plan : person.getPlans() ) {
				if ( plan != selectedPlan ) {
					toBeRemoved.add( plan ) ;
				}
			}
			for ( Plan plan : toBeRemoved ) {
				person.removePlan(plan) ;
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String inputPopFile="inputPop.xml.gz" ;
		final String outputPopFile="outputPop.xml.gz" ;

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig()) ;
		StreamingPopulationReader reader = new StreamingPopulationReader( scenario ) ;
		
		// --- add an algorithm:
		reader.addAlgorithm( new RemoveUnselectedPlans() ) ;

		// --- add writing the population needs to be explicitly added!:
		final StreamingPopulationWriter writer = new StreamingPopulationWriter( );
		// with current design, PopulationWriter(...) minimally demands a population.  Which can, however, be null when used for streaming.
		
		writer.startStreaming(outputPopFile);
		// (write the header of the population file)
		
		reader.addAlgorithm( writer ) ;
		
		// --- run everything:
		reader.readFile( inputPopFile );
		
		writer.closeStreaming();
		// (write the footer of the population file)
		
		
	}

}
