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

package analysisTest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.ScenarioFactoryImpl;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;

import charts.BkChartWriter;
import charts.types.BkDeltaUtilsChart;
import charts.types.BkDeltaUtilsQuantilesChart;

/**
 * @author bkickhoefer after kn and dgrether
 */

public class BkAnalysisTest {
	
	private static final Logger log = Logger.getLogger(BkAnalysisTest.class);
	
//	String netfile = "run860.output_network.xml.gz";
//	String plansfile1 = "run860.output_plans.xml.gz";
//	String plansfile2 = "run864.output_plans.xml.gz";
//	String householdsfile = "households.xml";
//	String outputfiles = "../runs-svn/run864/analysis/deltaUtilsPerPersons";
	
	String netfile = "../runs-svn/run860/run860.output_network.xml.gz";
	String plansfile1 = "../runs-svn/run860/run860.output_plans.xml.gz";
	String plansfile2 = "../runs-svn/run864/run864.output_plans.xml.gz";
	String householdsfile = "../shared-svn/studies/bkick/oneRouteTwoModeIncomeTest/households.xml";
//	String outputfiles = "../runs-svn/run864/analysis/deltaUtilsPerPersons";
	
	//main class
	public static void main(final String[] args) throws IOException {
		BkAnalysisTest app = new BkAnalysisTest();
		app.run(args);
	}

//============================================================================================================	
	
	public void run(final String[] args) throws IOException {
		//instancing scenario1 with a config (path to network and plans)
		Scenario sc1 = new ScenarioFactoryImpl().createScenario();
		Config c1 = sc1.getConfig();
		c1.network().setInputFile(netfile);
		c1.plans().setInputFile(plansfile1);
		
		//loading scenario1 and getting the population1
		ScenarioLoader sl1 = new ScenarioLoaderImpl(sc1) ;
		sl1.loadScenario() ;
		Population population1 = sc1.getPopulation();

	//===
		
		//instancing scenario2 with a config (path to network and plans)
		Scenario sc2 = new ScenarioFactoryImpl().createScenario();
		Config c2 = sc2.getConfig();
		c2.network().setInputFile(netfile);
		c2.plans().setInputFile(plansfile2);
		
		//loading scenario2 and getting the population2
		ScenarioLoader sl2 = new ScenarioLoaderImpl(sc2) ;
		sl2.loadScenario() ;
		Population population2 = sc2.getPopulation();
		
	//===
		
		//instancing and reading households
		Households households = new HouseholdsImpl();
		HouseholdsReaderV10 reader = new HouseholdsReaderV10(households);
		reader.readFile(householdsfile);
		
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
		
		//if desired, add additional maps (see maps above, eg: homeX, homeY, isCarAvail, isSelectedPlanCar)
		SortedMap<Id, RowTest> populationInformation = putAllNeededPopulationInfoInOneMap(scores1, scores2, personalIncome);

	//=== Defining and writing data and charts (for creation of series and dataset)
		
//======================================================================================================================================================
		
		
		//		OUTPUT TO TXT FILE
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File("output1.txt")));
		
		bw.write("PersonalIncome\tScore1\tScore2\tScoreDiff");
		bw.newLine();
			
		for (RowTest row : populationInformation.values()) {
			bw.write(getIncomeShare(row, personalIncome)+"\t"+row.getScore1()+"\t"+row.getScore2()+"\t"+row.getScoreDiff());
			bw.newLine();
		}
		
		bw.close();
		
		
		
		
		//	Alternative Output
		
		
		
		//BkDeltaUtilsChartGeneral - plots individual utility differences over income (linear axis)
		BkDeltaUtilsChart deltaUtilsChart = new BkDeltaUtilsChart(populationInformation);
		
		//
		
//		MISSING TYPE ROW?!?!
//		BkDeltaUtilsQuantilesChart deltaUtilsQuantilesChart = new BkDeltaUtilsQuantilesChart(populationInformation);
		
		//===
		
		//BkChartWriter gets an jchart object from the defined charts above to write the chart:
//		BkChartWriter.writeChart(outputfiles, deltaUtilsChart.createChart());
		
		log.info( "\n" + "******************************" + "\n"
				       + "Chart(s) and table(s) written." + "\n"
				       + "******************************");
	}
	
//============================================================================================================	
	
//	CALCULATION AND FILTERS
	
	private Double calculateAverageIncome(final Population pop, final SortedMap<Id, Double> personalIncome) {
		Double totalIncome = .0;
		for (Double income : personalIncome.values()) {
			totalIncome+=income;
		}
		return totalIncome/pop.getPersons().size();		//average Income
	}
	
	private Double getMaximumIncome (final SortedMap<Id, Double> personalIncome) {
		Double maximumIncome=.0;
		for (Double income : personalIncome.values()) {
			if (income>maximumIncome) maximumIncome=income;
		}
		return maximumIncome;
	}
	
	private Double getIncomeShare (RowTest row, final SortedMap<Id, Double> personalIncome) {				
		return row.getPersonalIncome()/getMaximumIncome(personalIncome);	
	}
	
	private SortedMap<Id, RowTest> getHigherScorePopulation (SortedMap<Id, RowTest> populationInformation) {
		SortedMap<Id, RowTest> improved = new TreeMap<Id, RowTest>(new ComparatorImplementation());
		for (RowTest row : populationInformation.values()) {
			if (row.getScore2()>row.getScore1()) {
				improved.put(row.getId(), row);
			}
		}
		return improved;
	}
	
	private SortedMap<Id, RowTest> getLowerScorePopulation (SortedMap<Id, RowTest> populationInformation) {
		SortedMap<Id, RowTest> worse = new TreeMap<Id, RowTest>(new ComparatorImplementation());
		for (RowTest row : populationInformation.values()) {
			if (row.getScore2()<=row.getScore1()) {
				worse.put(row.getId(), row);
			}
		}
		return worse;
	}
	
//	private SortedMap<Id, Row> changesMode (Population pop1, Population pop2) {
//		SortedMap<Id, Row> changes = new TreeMap<Id, Row>(new ComparatorImplementation());
//		for (Person person : pop1.getPersons().values()) {
//			Row row = new Row();
//			row.setId(person.getId());
//			row.setScore1();
//			
//			for (Plan plan1 : person.getPlans()) {
//				for (PlanElement element1 : plan1.getPlanElements()){ 
//					if (element1 instanceof Leg) {
//						Plan plan2 = (Plan) pop2.getPersons().get(person.getId()).getPlans();
//						PlanElement element2 = (PlanElement) plan2.getPlanElements();
//						if (((Leg) element1).getMode() != ((Leg)element2).getMode())	{
//							
//							changes.put(person.getId(), value)
//						}
//					}
//				}
//			}
//		}
//		return null;
//	}
	

//============================================================================================================	

	//additional maps can be inserted here, e.g., SortedMap<Id, Double> homeX, SortedMap<Id, Double> homeY, SortedMap<Id, Double> isCarAvail, SortedMap<Id, Double> isSelectedPlanCar: 
	private SortedMap<Id, RowTest> putAllNeededPopulationInfoInOneMap(SortedMap<Id, Double> scores1, SortedMap<Id, Double> scores2,SortedMap<Id, Double> personalIncome) {
		
		SortedMap<Id, RowTest> result = new TreeMap<Id, RowTest>(new ComparatorImplementation());
		for(Id id : scores1.keySet()){						
			
			//Row can be extended to all the needed information
			RowTest row = new RowTest();
			row.setId(id);
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
			// this distributes the household income on its members - in this case equally...
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
