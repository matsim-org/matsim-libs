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

package playground.benjamin;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.ScenarioFactoryImpl;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;

import playground.benjamin.charts.BkChartWriter;
import playground.benjamin.charts.types.BkDeltaUtilsChartGeneral;

/**
 * @author bkickhoefer after kn and dgrether
 */

public class BkAnalysisGeneral {
	
	private static final Logger log = Logger.getLogger(BkAnalysisGeneral.class);
	

	public void run(final String[] args) {
		//instancing scenario1 with a config (path to network and plans)
		Scenario sc1 = new ScenarioFactoryImpl().createScenario();
		Config c1 = sc1.getConfig();
		c1.network().setInputFile(BkPaths.RUNSSVN + "/run860/run860.output_network.xml.gz");
		c1.plans().setInputFile(BkPaths.RUNSSVN + "/run860/run860.output_plans.xml.gz");
		
		//loading scenario1 and getting the population1
		ScenarioLoader sl1 = new ScenarioLoaderImpl(sc1) ;
		sl1.loadScenario() ;
		Population population1 = sc1.getPopulation();

	//===
		
		//instancing scenario2 with a config (path to network and plans)
		Scenario sc2 = new ScenarioFactoryImpl().createScenario();
		Config c2 = sc2.getConfig();
		c2.network().setInputFile(BkPaths.RUNSSVN + "/run860/run860.output_network.xml.gz");
		c2.plans().setInputFile(BkPaths.RUNSSVN + "/run864/run864.output_plans.xml.gz");
		
		//loading scenario2 and getting the population2
		ScenarioLoader sl2 = new ScenarioLoaderImpl(sc2) ;
		sl2.loadScenario() ;
		Population population2 = sc2.getPopulation();
		
	//===
		
		//instancing and reading households
		Households households = new HouseholdsImpl();
		HouseholdsReaderV10 reader = new HouseholdsReaderV10(households);
		reader.readFile(BkPaths.STUDIESBK + "/oneRouteTwoModeIncomeTest/households.xml");
		
//============================================================================================================		

		//get all needed information from the populations (one map for each attribute)
		SortedMap<Id, Double> scores1 = getScoresFromPlans(population1);
		SortedMap<Id, Double> scores2 = getScoresFromPlans(population2);
		
		//SortedMap<Id, Double> householdIncome = getHouseholdIncomeFromFile(households);
		SortedMap<Id, Double> personalIncome = getPersonalIncomeFromHouseholds(households);
		SortedMap<Id, Double> homeX = getHomeXFromPlans(population1);
		SortedMap<Id, Double> homeY = getHomeYFromPlans(population1);
		
		SortedMap<Id, Double> isCarAvail = getIsCarAvailFromPlans(population1);
		SortedMap<Id, Double> isSelectedPlanCar = getIsSelectedPlanCarFromPlans(population1);
		
		//if desired, add additional maps after scores1, scores2 (see maps above, eg: personalIncome, homeX, homeY, isCarAvail, isSelectedPlanCar)
		SortedMap<Id, Row> populationInformation = putAllNeededPopulationInfoInOneMap(scores1, scores2, personalIncome);

		//BkDeltaUtilsChartGeneral for creation of series and dataset
		BkDeltaUtilsChartGeneral deltaUtilsChart = new BkDeltaUtilsChartGeneral(populationInformation);
		
		//BkChartWriter gets an jchart object from BkDeltaUtilsChartGeneral to write the chart:
		BkChartWriter.writeChart(BkPaths.RUNSSVN + "/run864/analysis/deltaUtilsPerPersons", deltaUtilsChart.createChart());
		
		log.info( "\n" + "******************************" + "\n"
				       + "Chart(s) and table(s) written." + "\n"
				       + "******************************");
	}

//============================================================================================================	

	//additional maps can be inserted here after scores1, scores2, eg , SortedMap<Id, Double> personalIncome, SortedMap<Id, Double> homeX, SortedMap<Id, Double> homeY, SortedMap<Id, Double> isCarAvail, SortedMap<Id, Double> isSelectedPlanCar: 
	private SortedMap<Id, Row> putAllNeededPopulationInfoInOneMap(SortedMap<Id, Double> scores1, SortedMap<Id, Double> scores2,SortedMap<Id, Double> personalIncome) {
		
		SortedMap<Id, Row> result = new TreeMap<Id, Row>(new ComparatorImplementation());
		for(Id id : scores1.keySet()){						
			
			//Row can be extended to all the needed information
			Row row = new Row();
			row.setScore1(scores1.get(id));
			row.setScore2(scores2.get(id));
			
			row.setPersonalIncome(personalIncome.get(id));
//			row.setHomeX(homeX.get(id));
//			row.setHomeY(homeY.get(id));
//			
//			row.setCarAvail(isCarAvail.get(id));
//			row.setSelectedPlanCar(isSelectedPlanCar.get(id));
			
			result.put(id, row);
		}
		return result;
	}
	
	//===
	
	private SortedMap<Id, Double> getIsSelectedPlanCarFromPlans(Population population1) {
		// TODO Auto-generated method stub
		return null;
	}

	private SortedMap<Id, Double> getIsCarAvailFromPlans(Population population1) {
		// TODO Auto-generated method stub
		return null;
	}

	private SortedMap<Id, Double> getHomeYFromPlans(Population population1) {
		// TODO Auto-generated method stub
		return null;
	}

	private SortedMap<Id, Double> getHomeXFromPlans(Population population1) {
		// TODO Auto-generated method stub
		return null;
	}

	private SortedMap<Id, Double> getPersonalIncomeFromHouseholds(Households households) {
		SortedMap<Id,Double> personId2PersonalIncome = new TreeMap<Id, Double>(new ComparatorImplementation());
		
		//iterating over every household hh in order to get personIds and personal income 
		for (Household hh : households.getHouseholds().values()) {
			Id personId = hh.getMemberIds().get(0);
			double personalIncome = distributeHouseholdIncomeToMembers(hh);
			personId2PersonalIncome.put(personId, personalIncome);	
		}
		return personId2PersonalIncome;
	}
	
			private double distributeHouseholdIncomeToMembers(Household hh) {
				double personalIncome = hh.getIncome().getIncome() / hh.getMemberIds().size();
				return personalIncome;
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
		BkAnalysisGeneral app = new BkAnalysisGeneral();
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
