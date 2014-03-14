/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.johannes.gsv.demand;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.johannes.coopsim.mental.choice.ChoiceSet;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author johannes
 *
 */
public class DepartureTimeDistributor implements PopulationTask {

	private ChoiceSet<Integer> choiceSet;
	
	private Random random = new XORShiftRandom();
	
	public DepartureTimeDistributor(String file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		choiceSet = new ChoiceSet<Integer>(random);
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split(";");
			for(int i = 2; i < 26; i++) {
				choiceSet.addChoice(i-2, Double.parseDouble(tokens[i].replace(",", ".")));
			}
			break;
		}
		reader.close();
	}
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.demand.PopulationTask#apply(org.matsim.api.core.v01.population.Population)
	 */
	@Override
	public void apply(Population pop) {
		for(Person p : pop.getPersons().values()) {
			int hour = choiceSet.randomWeightedChoice();
			double min = random.nextDouble();
			double time = 24*60*hour + 60*min;
			Activity home = (Activity) p.getPlans().get(0).getPlanElements().get(0);
			home.setEndTime(time);
		}
	}
	
	public static void main(String args[]) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile("/home/johannes/gsv/netz2030/data/population.xy.xml");
		
		DepartureTimeDistributor distributor = new DepartureTimeDistributor("/home/johannes/gsv/netz2030/data/raw/Ganglinien_Standardtag.csv");
		distributor.apply(scenario.getPopulation());
		
		PopulationWriter writer = new PopulationWriter(scenario.getPopulation(), null);
		writer.write("/home/johannes/gsv/netz2030/data/population.xy.dt.xml");
	}

}
