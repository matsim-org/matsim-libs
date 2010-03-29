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

package playground.benjamin.plans;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.ScenarioFactoryImpl;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.benjamin.charts.BkChartWriter;
import playground.benjamin.old.BkPopulationScoreDifferenceWriter;

/**
 * @author bkickhoefer after kn
 */

public class BkPlansLoaderFor2Populations {
	

	public void run(final String[] args) {
		//instancing the scenario1 with a config (path to network and plans)
		Scenario sc1 = new ScenarioFactoryImpl().createScenario();
		Config c1 = sc1.getConfig();
		c1.network().setInputFile("../matsim/examples/equil/network.xml");
		c1.plans().setInputFile("../matsim/output/multipleIterations10/output_plans.xml.gz");
		
		//loading the scenario1 and getting the population1
		ScenarioLoader sl1 = new ScenarioLoaderImpl(sc1) ;
		sl1.loadScenario() ;
		Population population1 = sc1.getPopulation();

	//===
		
		//instancing the scenario2 with a config (path to network and plans)
		Scenario sc2 = new ScenarioFactoryImpl().createScenario();
		Config c2 = sc2.getConfig();
		c2.network().setInputFile("../matsim/examples/equil/network.xml");
		c2.plans().setInputFile("../matsim/output/multipleIterations20/output_plans.xml.gz");
		
		//loading the scenario2 and getting the population2
		ScenarioLoader sl2 = new ScenarioLoaderImpl(sc2) ;
		sl2.loadScenario() ;
		Population population2 = sc2.getPopulation();
		
//============================================================================================================		

		//get the sorted scores from the plans
		SortedMap<Id, Double> scores1 = getScoresFromPlans(population1);
		SortedMap<Id, Double> scores2 = getScoresFromPlans(population2);
		
		//calculate the score differences per person
		SortedMap<Id, Double> scoreDifferences = calculateScoreDifferences(scores1, scores2);
		
			//writing the output with the help of my scoreWriter
			BkPopulationScoreDifferenceWriter scoreWriter = new BkPopulationScoreDifferenceWriter(scoreDifferences);
			scoreWriter.writeChart("../matsim/output/multipleIterations20/scoreDifferencePerPerson20-10.png");
			scoreWriter.writeTxt("../matsim/output/multipleIterations20/scoreDifferencePerPerson20-10.txt");
		
		System.out.println("Charts and tables written.");
	}

//============================================================================================================	
	
	//this calculates the score differences
	private SortedMap<Id, Double> calculateScoreDifferences(SortedMap<Id, Double> scores1, SortedMap<Id, Double> scores2) {
		SortedMap<Id, Double> result = new TreeMap<Id, Double>(new ComparatorImplementation());
		
		for (Id id : scores1.keySet()){
			//value = map.get(key) !!!
			Double score1 = scores1.get(id);
			Double score2 = scores2.get(id);
			Double scoreDifference = score2 - score1;
			result.put(id, scoreDifference);
		}		
		
		return result;
	}

	private SortedMap<Id, Double> getScoresFromPlans(Population population) {
		//instancing the sorted map (comparator - see below - is needed to compare Ids not as Strings but as Integers)
		SortedMap<Id,Double> result = new TreeMap<Id, Double>(new ComparatorImplementation());
		
		//adding the ids and the scores to the map 
		for(Person person : population.getPersons().values()) {
			Id id = person.getId();
			Double score = person.getSelectedPlan().getScore();
			result.put(id, score);
		}
		return result;
	}

//============================================================================================================		
	
	//main class
	public static void main(final String[] args) {
		BkPlansLoaderFor2Populations app = new BkPlansLoaderFor2Populations();
		app.run(args);
	}

	//comparator to compare Ids not as Strings but as Integers (see above)
	private final class ComparatorImplementation implements Comparator<Id> {
		@Override
		public int compare(Id id1, Id id2) {
			Integer i1 = Integer.parseInt(id1.toString());
			Integer i2 = Integer.parseInt(id2.toString()); 
			return i1.compareTo(i2);
		}
	}

}
