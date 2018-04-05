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

/**
 * 
 */
package vwExamples.cemdap4wob.planspreprocessing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class PlansMerger {
	
	
	public static void main(String[] args) {
		String inputFolder = "D:/cemdap-vw/cemdap_output/";
		new PlansMerger().run(inputFolder);
	}
	
	public void run (String inputFolder){
		Population[] population = new Population[5];
		for (int i = 1; i<=5; i++){
		String plansfile = inputFolder + "/"+ i + "/plans.xml.gz";
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		population[i-1] = scenario.getPopulation();
		new PopulationReader(scenario).readFile(plansfile);
		}
		
		for (Person p : population[0].getPersons().values()){
			for (int i =1;i<5;i++){
				Plan plan = population[i].getPersons().get(p.getId()).getPlans().get(0);
				p.addPlan(plan);
			}
		}
		new PopulationWriter(population[0]).write(inputFolder+"/mergedplans.xml.gz");
		
		
	}
}
