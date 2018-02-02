/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package vwExamples.utils;

import java.util.Random;
import java.util.stream.DoubleStream;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
public class SubsamplePopulation {
public static void main(String[] args) {
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	new PopulationReader(scenario).readFile("C:/Users/VWBIDGN/Downloads/vw205.1.0/vw205.1.0.output_plans.xml.gz");
	Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	Population pop2 = scenario2.getPopulation();
	int i = 0;
	double pct = 0.1;
	
	
	for (Person p : scenario.getPopulation().getPersons().values()){
		double randValue = MatsimRandom.getRandom().nextDouble();
		
		System.out.println(randValue);
		

		
		if (randValue < pct){
			i++;
			pop2.addPerson(p);
			
		}
	}
	System.out.println(i + " persons extracted");
	new PopulationWriter(pop2).write("C:/Users/VWBIDGN/Downloads/vw205.1.0/vw205.1.0.output_plans_"+String.valueOf(pct)+".xml.gz");
}
}
