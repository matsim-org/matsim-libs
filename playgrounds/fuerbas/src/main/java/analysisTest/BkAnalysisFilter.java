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


package analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;


/**
 * @author fuerbas
 */

public class BkAnalysisFilter extends BkAnalysisTest {
	
	protected String output;
	
	BkAnalysisFilter() {
		this.output=output;
	}

	protected Double getTotalIncome(final SortedMap<Id, Double> personalIncome) {
		Double totalIncome = .0;
		for (Double income : personalIncome.values()) {
			totalIncome+=income;
		}		
		return totalIncome;
	}
	
	protected Double calculateAverageIncome(final Population population1, final SortedMap<Id, Double> personalIncome) {
		Double totalIncome = .0;
		for (Double income : personalIncome.values()) {
			totalIncome+=income;
		}
		return totalIncome/population1.getPersons().size();
	}
	
	protected Double getMaximumIncome (final SortedMap<Id, Double> personalIncome) {
		Double maximumIncome=.0;
		for (Double income : personalIncome.values()) {
			if (income>maximumIncome) maximumIncome=income;
		}
		return maximumIncome;
	}
	
	protected Double getIncomeShare (RowTest row, final SortedMap<Id, Double> personalIncome) {				
		return row.getPersonalIncome()/getMaximumIncome(personalIncome);	
	}
	
	protected void createIncomeRanking (SortedMap<Id, RowTest> populationInformation) throws IOException {
		output = "incomeranking";
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(output+".txt")));
		SortedMap<Double, RowTest> incomeranking = new TreeMap<Double, RowTest>(new IncomeComparator());
		bw.write("Rank \t PersonalIncome \t Score1 \t Score2 \t ScoreDiff");
		bw.newLine();
		for (RowTest row : populationInformation.values()) {
			incomeranking.put(row.getPersonalIncome(), row);
		}
		Double iii=.0;
		for (RowTest row : incomeranking.values()) {
			iii++;
			bw.write(iii/incomeranking.size()+"\t"+row.getPersonalIncome()+"\t"+row.getScore1()+"\t"+row.getScore2()+"\t"+row.getScoreDiff());
			bw.newLine();
		}
		bw.close();
	}
	
	protected void getHigherScorePopulation (SortedMap<Id, RowTest> populationInformation) throws IOException {	
		output = "increasedScore";
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(output+".txt")));
		SortedMap<Id, RowTest> improved = new TreeMap<Id, RowTest>(new ComparatorImplementation());
		bw.write("PersonalIncome \t Score1 \t Score2 \t ScoreDiff");
		bw.newLine();
		for (RowTest row : populationInformation.values()) {
			if (row.getScore2()>row.getScore1()) {
				improved.put(row.getId(), row);
			}
		}
		for (RowTest row : improved.values()) {
			bw.write(row.getPersonalIncome()+"\t"+row.getScore1()+"\t"+row.getScore2()+"\t"+row.getScoreDiff());
			bw.newLine();
		}
		bw.close();
	}
	
	protected void getLowerScorePopulation (SortedMap<Id, RowTest> populationInformation) throws IOException {
		output = "decreasedScore";
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(output+".txt")));
		SortedMap<Id, RowTest> worse = new TreeMap<Id, RowTest>(new ComparatorImplementation());
		bw.write("PersonalIncome \t Score1 \t Score2 \t ScoreDiff");
		bw.newLine();
		for (RowTest row : populationInformation.values()) {
			if (row.getScore2()<=row.getScore1()) {
				worse.put(row.getId(), row);
			}
		}
		for (RowTest row : worse.values()) {
			bw.write(row.getPersonalIncome()+"\t"+row.getScore1()+"\t"+row.getScore2()+"\t"+row.getScoreDiff());
			bw.newLine();
		}
		bw.close();
	}
	
//	protected SortedMap<Id, Row> changesMode (Population pop1, Population pop2) {
//	SortedMap<Id, Row> changes = new TreeMap<Id, Row>(new ComparatorImplementation());
//	for (Person person : pop1.getPersons().values()) {
//		Row row = new Row();
//		row.setId(person.getId());
//		row.setScore1();
//		
//		for (Plan plan1 : person.getPlans()) {
//			for (PlanElement element1 : plan1.getPlanElements()){ 
//				if (element1 instanceof Leg) {
//					Plan plan2 = (Plan) pop2.getPersons().get(person.getId()).getPlans();
//					PlanElement element2 = (PlanElement) plan2.getPlanElements();
//					if (((Leg) element1).getMode() != ((Leg)element2).getMode())	{
//						
//						changes.put(person.getId(), value)
//					}
//				}
//			}
//		}
//	}
//	return null;
//}
	
	protected final class IncomeComparator implements Comparator<Double> {
		public int compare(Double income1, Double income2) {
			return income1.compareTo(income2);
		}
	}

}
