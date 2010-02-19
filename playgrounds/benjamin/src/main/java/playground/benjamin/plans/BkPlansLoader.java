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

/**
 * @author bkickhoefer after kn
 */

public class BkPlansLoader {
	

	public void run(final String[] args) {
		Scenario sc = new ScenarioFactoryImpl().createScenario();
		Config c = sc.getConfig();
		c.network().setInputFile("../matsim/examples/equil/network.xml");
		c.plans().setInputFile("../matsim/output/multipleIterations/output_plans.xml.gz");
		
		
		ScenarioLoader sl = new ScenarioLoaderImpl(sc) ;
		sl.loadScenario() ;
		Population population = sc.getPopulation();

		SortedMap<Id, Double> scores = getScoresFromPlans(population);
		
		
		BkPopulationScoreWriter scoreWriter = new BkPopulationScoreWriter(scores);
		scoreWriter.writeChart("../matsim/output/multipleIterations/scorePerPerson.png");
		scoreWriter.writeTxt("../matsim/output/multipleIterations/scorePerPerson.txt");

		System.out.println("done.");
	}

	private SortedMap<Id, Double> getScoresFromPlans(Population population) {
		SortedMap<Id,Double> result = new TreeMap<Id, Double>(new ComparatorImplementation());
		for(Person person : population.getPersons().values()) {
			Id id = person.getId();
			Double score = person.getSelectedPlan().getScore();
			result.put(id, score);
		}
		return result;
	}

	public static void main(final String[] args) {
		BkPlansLoader app = new BkPlansLoader();
		app.run(args);
	}

	
	private final class ComparatorImplementation implements Comparator<Id> {
		@Override
		public int compare(Id id1, Id id2) {
			Integer i1 = Integer.parseInt(id1.toString());
			Integer i2 = Integer.parseInt(id2.toString()); 
			return i1.compareTo(i2);
		}
	}

}
