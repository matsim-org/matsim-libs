/* *********************************************************************** *
 * project: org.matsim.*
 * EstimationValidation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mfeil.MDSAM;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scoring.PlanScorer;

import playground.mfeil.JohScoringFunctionEstimationFactory;



/**
 * Re-evaluates the estimation result.
 *
 * @author mfeil
 */
public class EstimationValidation {

	private final PopulationImpl population;
	private final PlanScorer scorer;
	private static final Logger log = Logger.getLogger(EstimationValidation.class);


	public EstimationValidation(final PopulationImpl population) {
		this.population = population;
		this.scorer = new PlanScorer (new JohScoringFunctionEstimationFactory());
	}
	
	public void run(String outputFile){
		log.info("Scoring mz plans file...");
		log.info("populationOrig: "+this.population.getPersons().size());	
		List<List<Double>> sims = this.calculateSimilarity();
		
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		// First row
		stream.print("Id\tChoice\tChosenUtility\tMaxUtility\tRank\t");
		Person p = this.population.getPersons().values().iterator().next();
	
		for (int i = 0;i<p.getPlans().size();i++){
			stream.print("alt"+(i+1)+"\t");
		}
		stream.println();
		
		// Filling plans
		int counterCorrectChoice = -1;
		int counterOut = -1;
		double distance = 0;
		for (Person person : this.population.getPersons().values()) {
			counterOut++;
			
			//Id
			stream.print(person.getId()+"\t");
			
			//Choice
			int position = -1;
			for (int i=0;i<person.getPlans().size();i++){
				if (person.getPlans().get(i).equals(person.getSelectedPlan())) {
					position = i+1;
					break;
				}
			}
			stream.print(position+"\t");
			
			//Chosen utility
			stream.print((this.scorer.getScore(person.getSelectedPlan())+sims.get(counterOut).get(position-1)*(-0.621))+"\t");
			
			//MaxUtility
			double maxScore = -100000;
			int rank = 1;
			int counterIn = -1;
			for (Plan plan : person.getPlans()) {
				counterIn++;
				if (plan.getScore() == null || plan.getScore()!=-100000){
					plan.setScore(this.scorer.getScore(plan)+sims.get(counterOut).get(counterIn)*(-0.621));
					if (maxScore<plan.getScore()) maxScore = plan.getScore();
					if (plan.getScore()>person.getSelectedPlan().getScore()) rank++;
				}
			}
			if (rank==1) counterCorrectChoice++;
			else distance += rank-1;
			stream.print(maxScore+"\t"+rank+"\t");
			
			// Utilities
			for (Plan plan : person.getPlans()) {
				if (plan.getScore()!=-100000) {
					stream.print((plan.getScore())+"\t");					
				}
				else stream.print("na\t");
			}
			stream.println();
		}
		stream.println();
		
		stream.println("NoOfCorrectChoice\t"+counterCorrectChoice);
		stream.println("AveDistanceFromChoice\t"+(distance/this.population.getPersons().size()));
		
		stream.close();
		log.info("done.");
		
		
	}
	
	
	private List<List<Double>> calculateSimilarity (){
		MDSAM mdsam = new MDSAM(this.population);
		return mdsam.runPopulation();
	}
	
	public static void main(final String [] args) {
				final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
				final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
				final String populationFilename = "/home/baug/mfeil/data/largeSet/it0/output_plans_mz02.xml";
				final String outputFile = "/home/baug/mfeil/data/largeSet/it0/estimation_val093.xls";
	
				ScenarioImpl scenarioOrig = new ScenarioImpl();
				new MatsimNetworkReader(scenarioOrig.getNetwork()).readFile(networkFilename);
				new MatsimFacilitiesReader(scenarioOrig.getActivityFacilities()).readFile(facilitiesFilename);
				new MatsimPopulationReader(scenarioOrig).readFile(populationFilename);
								
				EstimationValidation ev = new EstimationValidation(scenarioOrig.getPopulation());
				ev.run(outputFile);
				log.info("Process finished.");
			}
}

