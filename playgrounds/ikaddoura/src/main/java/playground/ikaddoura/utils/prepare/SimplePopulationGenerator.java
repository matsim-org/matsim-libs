/* *********************************************************************** *
 * project: org.matsim.*
 * CreatePopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.ikaddoura.utils.prepare;

import java.io.File;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class SimplePopulationGenerator {

	private Scenario scenario;
	private Population population;
	
	public static void main(String[] args) {
		
		final String networkFile = "../../../runs-svn/vickreyPricing/input/network.xml";
		final String outputDirectory = "../../../runs-svn/vickreyPricing/input/";
		final int totalDemand = 7200;
		
		final File directory = new File(outputDirectory);
		directory.mkdirs();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

		String populationFile = outputDirectory + "population_" + totalDemand + "trips_optimal_4-10.xml";
		
		SimplePopulationGenerator pG = new SimplePopulationGenerator(scenario);
		pG.writePopulation(totalDemand, populationFile);
	}

	public SimplePopulationGenerator(Scenario scenario) {
		this.scenario = scenario;
		this.population = scenario.getPopulation();
	}

	public void writePopulation(int totalNumberOfAgents, String populationFile) {
		
		generatePopulation(totalNumberOfAgents);
		
		PopulationWriter populationWriter = new PopulationWriter(population, scenario.getNetwork());
		populationWriter.write(populationFile);
	}

	private void generatePopulation(int totalNumberOfAgents) {
		for (int i=0; i<totalNumberOfAgents; ){
			Coord homeCoord = new Coord(0., 0.);	
			Coord workCoord = new Coord(15000., 0.);
			
			Person person = this.population.getFactory().createPerson(Id.createPersonId("person_" + i));
			Plan plan = this.population.getFactory().createPlan();
	
			Activity activity1 = this.population.getFactory().createActivityFromCoord("home", homeCoord);
			activity1.setEndTime(4.0 * 3600. + i);
			plan.addActivity(activity1);
				
			plan.addLeg(this.population.getFactory().createLeg(TransportMode.car));

			Activity activity2 = this.population.getFactory().createActivityFromCoord("work", workCoord);
			plan.addActivity(activity2);
						
			person.addPlan(plan);
			this.population.addPerson(person);
			
			i += 3;
		}
	}	
	
}

