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

package playground.benjamin.old;

import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.ScenarioFactoryImpl;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.benjamin.charts.BkChartWriter;

/**
 * @author bkickhoefer after kn and dgrether
 */

public class BkAnalysisFirstApproach {
	
	private static final Logger log = Logger.getLogger(BkAnalysisFirstApproach.class);
	

	public void run(final String[] args) {
		//instancing scenario1 with a config (path to network and plans)
		Scenario sc1 = new ScenarioFactoryImpl().createScenario();
		Config c1 = sc1.getConfig();
		c1.network().setInputFile("../matsim/examples/equil/network.xml");
		c1.plans().setInputFile("../matsim/output/multipleIterations10/output_plans.xml.gz");
		
		//loading scenario1 and getting the population1
		ScenarioLoader sl1 = new ScenarioLoaderImpl(sc1) ;
		sl1.loadScenario() ;
		Population population1 = sc1.getPopulation();

	//===
		
		//instancing scenario2 with a config (path to network and plans)
		Scenario sc2 = new ScenarioFactoryImpl().createScenario();
		Config c2 = sc2.getConfig();
		c2.network().setInputFile("../matsim/examples/equil/network.xml");
		c2.plans().setInputFile("../matsim/output/multipleIterations20/output_plans.xml.gz");
		
		//loading scenario2 and getting the population2
		ScenarioLoader sl2 = new ScenarioLoaderImpl(sc2) ;
		sl2.loadScenario() ;
		Population population2 = sc2.getPopulation();
		
	//===
		
		//instancing and reading households
		
//============================================================================================================		

		//get the scores from the plans (sorted by personId)
		SortedMap<Id, Double> scores1 = getScoresFromPlans(population1);
		SortedMap<Id, Double> scores2 = getScoresFromPlans(population2);
		
		//BkDeltaUtilsChart for score difference calculations and creation of series and dataset
		BkDeltaUtilsChartFirstApproach deltaUtilsChart = new BkDeltaUtilsChartFirstApproach(scores1, scores2);
		
		//BkChartWriter gets an jchart object from BkDeltaUtilsChart to write the chart:
		BkChartWriter.writeChart("../matsim/output/multipleIterations20/deltaUtilsPerPerson_20-10", deltaUtilsChart.createChart());
		
		log.info( "\n" + "******************************" + "\n"
				       + "Chart(s) and table(s) written." + "\n"
				       + "******************************");
	}

//============================================================================================================	
	
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
		BkAnalysisFirstApproach app = new BkAnalysisFirstApproach();
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
