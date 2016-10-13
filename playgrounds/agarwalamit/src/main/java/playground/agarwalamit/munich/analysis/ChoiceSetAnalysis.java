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
package playground.agarwalamit.munich.analysis;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */

public class ChoiceSetAnalysis {

	private final PersonFilter pf = new PersonFilter();
	private Map<List<String>, Integer> modeSequence2Count;
	private Map<List<String>, Double> modeSequence2TotalScore;
	private Map<List<String>, Double> modeSequence2TotalBestScore;
	private Map<List<String>, Double> modeSequence2TotalExecutedScore ;

	public static void main(String[] args) {
		String outputDir = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run10/policies/";
		String [] runCases = {"bau","ei","ci","eci","10ei"};

		for(String runCase :runCases){
			Scenario sc = LoadMyScenarios.loadScenarioFromOutputDir(outputDir+runCase);
			new ChoiceSetAnalysis().run(sc);
		}
	}

	private void run(Scenario sc){

		BufferedWriter writer = IOUtils.getBufferedWriter(sc.getConfig().controler().getOutputDirectory()+"/analysis/"+"choiceSet_stat.txt");
		try {
			writer.write("userGroup \t modeSequence \t numberOfSuchPlans \t carsInChoiceSet \t totalScoreAllPlans \t totalBestScore \t totalExecutedScore \n ");
			
			for(UserGroup ug :UserGroup.values()){
				processScenario(pf.getPopulation(sc.getPopulation(), ug));

				for(List<String> modes : modeSequence2Count.keySet()){
					writer.write(ug+"\t"+modes.toString()+"\t"+modeSequence2Count.get(modes)+"\t"+
							getCarsInList(modes)+"\t"+modeSequence2TotalScore.get(modes)+"\t"+
						modeSequence2TotalBestScore.get(modes)+"\t"+modeSequence2TotalExecutedScore.get(modes)+"\n");
				}

			}
			writer.close();	
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}

	private int getCarsInList(List<String> modes){
		int no =0;
		for(String mode :modes){
			if(mode.equals("car")) no++;
		}
		return no;
	}
	
	private void processScenario(Population pop){
		
		//initialize maps
		modeSequence2Count = new HashMap<>();
		modeSequence2TotalScore = new HashMap<>();
		modeSequence2TotalBestScore = new HashMap<>();
		modeSequence2TotalExecutedScore = new HashMap<>();
		
		for(Person p :pop.getPersons().values()){
			List<String> modes = new ArrayList<>();
			double score = 0.;
			for(Plan plan :p.getPlans()){
				modes.add(getTravelMode(plan));
				score  += plan.getScore();
			}
			
			if(modeSequence2Count.containsKey(modes)){
				modeSequence2Count.put(modes, modeSequence2Count.get(modes)+1);
				modeSequence2TotalScore.put(modes, modeSequence2TotalScore.get(modes)+score);
				modeSequence2TotalExecutedScore.put(modes, modeSequence2TotalExecutedScore.get(modes)+p.getSelectedPlan().getScore());
				modeSequence2TotalBestScore.put(modes, modeSequence2TotalBestScore.get(modes)+getBestScore(p));
			} else {
				modeSequence2Count.put(modes, 1);
				modeSequence2TotalScore.put(modes, score);
				modeSequence2TotalExecutedScore.put(modes, p.getSelectedPlan().getScore());
				modeSequence2TotalBestScore.put(modes, getBestScore(p));
			}
		}
	}

	private double getBestScore(Person person) {
		double bestScore = Double.NEGATIVE_INFINITY ;
		for ( Plan plan : person.getPlans() ) {
			if ( plan.getScore() > bestScore ) {
				bestScore = plan.getScore() ;
			}
		}
		return bestScore ;
	}
	
	private String getTravelMode(Plan plan){
		String mode = "NA";
		for(PlanElement pe :plan.getPlanElements()){
			if(pe instanceof Leg){
				mode = ((Leg)pe).getMode();
			}
		}
		
		if (!mode.equals(TransportMode.car)) return "nonCar";
		else return mode;
	}
}
