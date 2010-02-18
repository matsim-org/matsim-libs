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

import java.util.HashMap;
import java.util.Map;

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

		Map<Id, Double> scores = getScoresFromPlans(population);
		
		
		BkPopulationScoreWriter scorewriter = new BkPopulationScoreWriter(scores);
		scorewriter.writeChart("../matsim/output/multipleIterations/scorePerPerson.png");
		scorewriter.writeTxt("../matsim/output/multipleIterations/scorePerPerson.txt");

		System.out.println("done.");
	}

	private Map<Id, Double> getScoresFromPlans(Population population) {
		Map<Id, Double> result = new HashMap<Id, Double>();
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

}
