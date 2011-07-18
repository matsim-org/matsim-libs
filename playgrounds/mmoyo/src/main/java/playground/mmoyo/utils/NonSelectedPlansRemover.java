/* *********************************************************************** *
 * project: org.matsim.*
 * NonSelectedPlansRemover.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mmoyo.utils;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkImpl;
import org.matsim.population.algorithms.PersonAlgorithm;
 
/**
  *filters all plans out leaving only the selected ones
  * @author manuel
  */
public class NonSelectedPlansRemover implements PersonAlgorithm {

	@Override
	public void run(Person person){
		Collection <Plan> selectedPlanList = new ArrayList<Plan>();
		selectedPlanList.add(person.getSelectedPlan());
		person.getPlans().retainAll(selectedPlanList);
	}

	public void run(Population population){
		for (Person person : population.getPersons().values()){
			this.run(person);
		}
	}
	
	public static void main(String[] args) {
		String popFilePath;
		String netFilePath;
		String outputFilePath;
		
		if (args.length==3){
			popFilePath = args[0];
			netFilePath = args[1];
			outputFilePath = args[2];
		}else{
			popFilePath = "../../input/6_0_1200_2clons.xml.gz";
			netFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
			outputFilePath = "../../input/6_0_1200_2clonsNoSelPlans.xml.gz";
		}

		DataLoader dLoader = new DataLoader();
		
		Population population = dLoader.readPopulation(popFilePath);
		new NonSelectedPlansRemover().run(population);
		NetworkImpl net = dLoader.readNetwork(netFilePath);
		
		System.out.println("writing output plan file..." + outputFilePath);
		new PopulationWriter(population, net).write(outputFilePath);
	}
}
