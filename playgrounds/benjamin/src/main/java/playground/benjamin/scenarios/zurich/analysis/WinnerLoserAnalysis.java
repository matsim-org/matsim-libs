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

package playground.benjamin.scenarios.zurich.analysis;

import java.io.IOException;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;

import playground.benjamin.scenarios.zurich.analysis.filter.WinnerLoserAnalysisFilter;

/**
 * @author benjamin after kn and dgrether
 */

public class WinnerLoserAnalysis {
	
	private static final Logger log = Logger.getLogger(WinnerLoserAnalysis.class);
	
	String netfile = "../runs-svn/run860/run860.output_network.xml.gz";
	String plansfile1 = "../runs-svn/run860/run860.output_plans.xml.gz";
	String plansfile2 = "../runs-svn/run864/run864.output_plans.xml.gz";
	String householdsfile = "../shared-svn/studies/bkick/oneRouteTwoModeIncomeTest/households.xml";
	String outputPath = "../runs-svn/run864/postprocess/";
	
	//main class
	public static void main(final String[] args) throws IOException {
		WinnerLoserAnalysis app = new WinnerLoserAnalysis();
		app.run(args);
	}

//============================================================================================================	
	
	public void run(final String[] args) throws IOException {
		//instancing scenario1 with a config (path to network and plans)
		Config config = ConfigUtils.createConfig();
		Scenario sc1 = (ScenarioImpl) ScenarioUtils.createScenario(config);
		config.network().setInputFile(netfile);
		config.plans().setInputFile(plansfile1);
		
		//loading scenario1 and getting the population1
		ScenarioLoaderImpl sl1 = new ScenarioLoaderImpl(sc1) ;
		sl1.loadScenario() ;
		Population population1 = sc1.getPopulation();
		

	//===
		
		//instancing scenario2 with a config (path to network and plans)
		Config config2 = ConfigUtils.createConfig();
		Scenario sc2 = (ScenarioImpl) ScenarioUtils.createScenario(config2);
		config2.network().setInputFile(netfile);
		config2.plans().setInputFile(plansfile2);
		
		//loading scenario2 and getting the population2
		ScenarioLoaderImpl sl2 = new ScenarioLoaderImpl(sc2) ;
		sl2.loadScenario() ;
		Population population2 = sc2.getPopulation();
		
	//===
		
		//instancing and reading households
		Households households = new HouseholdsImpl();
		HouseholdsReaderV10 reader = new HouseholdsReaderV10(households);
		reader.readFile(householdsfile);
		
//============================================================================================================		

		//get all needed information from populations (one map for each attribute)
		SortedMap<Id, Double> personalIncome = getPersonalIncomeFromHouseholds(households);
		SortedMap<Id, Double> incomeRank = createIncomeRank(personalIncome);
		SortedMap<Id, Double> scores1 = getScoresFromPlans(population1);
		SortedMap<Id, Double> scores2 = getScoresFromPlans(population2);
		SortedMap<Id, Double> homeX = getHomeXFromPlans(population1);
		SortedMap<Id, Double> homeY = getHomeYFromPlans(population1);
		
		SortedMap<Id, Double> isCarAvail = getIsCarAvailFromPlans(population1);
		SortedMap<Id, Double> isSelectedPlanCar = getIsSelectedPlanCarFromPlans(population1);
		
		//if desired, add additional maps (see maps above, eg: homeX, homeY, isCarAvail, isSelectedPlanCar)
		SortedMap<Id, WinnerLoserAnalysisRow> populationInformation = putAllNeededPopulationInfoInOneMap(personalIncome, incomeRank, scores1, scores2);

//======================================================================================================================================================

		// apply filters and output to text files
		WinnerLoserAnalysisFilter filter = new WinnerLoserAnalysisFilter();
		SortedMap<Id, WinnerLoserAnalysisRow> winner = filter.getWinner(populationInformation);
		SortedMap<Id, WinnerLoserAnalysisRow> loser = filter.getLoser(populationInformation);
		
		WinnerLoserAnalysisWriter writer = new WinnerLoserAnalysisWriter(outputPath);
		writer.writeFile(winner, "winner");
		writer.writeFile(loser, "loser");
		writer.writeFile(populationInformation, "wholePopulation");

		log.info( "\n" + "************************************************************" + "\n"
				       + "Data written to " + outputPath + "\n"
				       + "************************************************************");
	}

//============================================================================================================	

	private SortedMap<Id, WinnerLoserAnalysisRow> putAllNeededPopulationInfoInOneMap(SortedMap<Id, Double> personalIncome, SortedMap<Id, Double> incomeRank,
			SortedMap<Id, Double> scores1, SortedMap<Id, Double> scores2) {
		
		SortedMap<Id, WinnerLoserAnalysisRow> result = new TreeMap<Id, WinnerLoserAnalysisRow>(new ComparatorImplementation());
		for(Id id : scores1.keySet()){						
			
			//Row can be extended to all the needed information
			WinnerLoserAnalysisRow winnerLoserAnalysisRow = new WinnerLoserAnalysisRow();
			winnerLoserAnalysisRow.setId(id);
			winnerLoserAnalysisRow.setPersonalIncome(personalIncome.get(id));
			winnerLoserAnalysisRow.setIncomeRank(incomeRank.get(id));
			winnerLoserAnalysisRow.setScore1(scores1.get(id));
			winnerLoserAnalysisRow.setScore2(scores2.get(id));
			
//			row.setHomeX(homeX.get(id));
//			row.setHomeY(homeY.get(id));
//			
//			row.setCarAvail(isCarAvail.get(id));
//			row.setSelectedPlanCar(isSelectedPlanCar.get(id));
			
			result.put(id, winnerLoserAnalysisRow);
		}
		return result;
	}
	
	//===
	private SortedMap<Id, Double> getHomeXFromPlans(Population population) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private SortedMap<Id, Double> getHomeYFromPlans(Population population) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private SortedMap<Id, Double> getIsSelectedPlanCarFromPlans(Population population) {
		// TODO Auto-generated method stub
		return null;
	}

	private SortedMap<Id, Double> getIsCarAvailFromPlans(Population population) {
		// TODO Auto-generated method stub
		return null;
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
	
	//income related methods
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

	private SortedMap<Id, Double> createIncomeRank(SortedMap<Id, Double> personalIncome) {
		SortedMap<Id, Double> incomeRank = new TreeMap<Id, Double>(new ComparatorImplementation());
		SortedMap<Double, Id> sortedIncome = new TreeMap<Double, Id>();
		
		for (Id id: personalIncome.keySet()){
			sortedIncome.put(personalIncome.get(id), id);
		}
		Double ii = 0.0;
		for(Double dd : sortedIncome.keySet()){
			ii++;
			Id id = sortedIncome.get(dd);
			Double rank = ii / sortedIncome.size();
			incomeRank.put(id, rank);
		}
		return incomeRank;
	}
	
//	protected Double getTotalIncome(final SortedMap<Id, Double> personalIncome) {
//		Double totalIncome = .0;
//		for (Double income : personalIncome.values()) {
//			totalIncome += income;
//		}
//		return totalIncome;
//	}
//
//	protected Double calculateAverageIncome(final Population population1, final SortedMap<Id, Double> personalIncome) {
//		Double totalIncome = .0;
//		for (Double income : personalIncome.values()) {
//			totalIncome += income;
//		}
//		return totalIncome/population1.getPersons().size();
//	}
//
//	protected Double getMaximumIncome (final SortedMap<Id, Double> personalIncome) {
//		Double maximumIncome = .0;
//		for (Double income : personalIncome.values()) {
//			if (income > maximumIncome){
//				maximumIncome = income;
//			}
//		}
//		return maximumIncome;
//	}
//
//	protected Double getIncomeShare (Row row, final SortedMap<Id, Double> personalIncome) {
//		return row.getPersonalIncome() / getMaximumIncome(personalIncome);
//	}
	
//============================================================================================================		

	//comparator to compare Ids not as Strings but as Integers (see above)
	public final class ComparatorImplementation implements Comparator<Id> {
		@Override
		public int compare(Id id1, Id id2) {
			Integer i1 = Integer.parseInt(id1.toString());
			Integer i2 = Integer.parseInt(id2.toString()); 
			return i1.compareTo(i2);
		}
	}
	
}
