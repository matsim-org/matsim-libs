/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package vwExamples.robotaxiVWExample;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.population.io.StreamingPopulationWriter;


/**
 * @author saxer
 *
 */

public class GetSubsampleFromPopulation {

	public static void main(String[] args) {
		String inputPlansFile = "C:/Users/VWBIDGN/Documents/MATSim/Szenarien/run102.100/run102.100.output_plans.xml.gz";
		String outputPersonAttributes = "C:/Users/VWBIDGN/git/vw-projects/src/main/java/robotest/input/run102.10.output_plans.xml";
		
		Integer i;
		Integer j;
		i=1;
		j=0;
		
//		Create new Writer
		StreamingPopulationWriter popWriter = new StreamingPopulationWriter();
		
//		Open stream
		popWriter.startStreaming(outputPersonAttributes);
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(inputPlansFile);
		for (Person p : scenario.getPopulation().getPersons().values())
			{
			
			//Take every 10th element
			if (i%10 == 0)
			{
				j=j+1;
				System.out.println(j);

				popWriter.writePerson(p);	
//				scenario.getPopulation().getPersonAttributes().putAttribute(p.getId().toString(), "subpopulation", subpopulation);
			}
		i=i+1;	
		}
		
		popWriter.closeStreaming();
//		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes()).writeFile(outputPersonAttributes);
	}
	
}
