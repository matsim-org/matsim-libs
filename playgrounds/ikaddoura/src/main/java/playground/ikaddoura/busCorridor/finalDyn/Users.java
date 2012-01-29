/* *********************************************************************** *
 * project: org.matsim.*
 * Users.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.busCorridor.finalDyn;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.ConfigUtils;

/**
 * @author Ihab
 *
 */
public class Users {
	
	private final static Logger log = Logger.getLogger(Users.class);

	private double logSum;
	private double avgExecScore;
	private int numberOfPtLegs;
	private int numberOfCarLegs;
	private int numberOfWalkLegs;
	private String directoryExtIt;
	private String networkFile;
	private final double MONEY_UTILS;

	public Users(String directoryExtIt, String networkFile, double MONEY_UTILS) {
		this.directoryExtIt = directoryExtIt;
		this.networkFile = networkFile;
		this.MONEY_UTILS = MONEY_UTILS;
	}

	public void calculateScore() {
		
		List<Double> execScores = new ArrayList<Double>();
		
		String outputPlanFile = this.directoryExtIt+"/internalIterations/output_plans.xml.gz";		
		
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(outputPlanFile);
		config.network().setInputFile(this.networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();

		double logSumAllPersons = 0.0;
		for(Person person : population.getPersons().values()){
			double expScore = 0.0;
			for (Plan plan : person.getPlans()){
				if (plan.getScore()>-100000){
					expScore = expScore + Math.exp(plan.getScore());
				}
				else {
					log.info("A plan of "+person.getId()+" is not used for LogSumUserScoring because of Agent would stuck --> Score: "+plan.getScore());
				}
			}
			
			double score = person.getSelectedPlan().getScore();
			if (score > -100000){
				execScores.add(score);
			}
			else {
				log.info("A plan of "+person.getId()+" is not used for AvgExecUserScoring because of Agent would stuck --> Score: "+score);
			}
			
			double logSumThisPerson = (1/MONEY_UTILS) * Math.log(expScore);
			
			if (logSumThisPerson<-100000){
				log.warn("All plans of "+person.getId()+" are not used for LogSum User Scoring. (LogSum for this person: "+logSumThisPerson+")");
			}
			else {
				logSumAllPersons = logSumThisPerson + logSumAllPersons;
			}
		}
		
		double execScoreSum = 0.0;
		for (Double score : execScores){
			execScoreSum = execScoreSum + score;
		}
		
		this.setAvgExecScore(execScoreSum/execScores.size());
		this.setLogSum(logSumAllPersons);
		
		log.info("Average User Score calculated. "+this.avgExecScore);
		log.info("User Score Sum (LogSum) calculated. "+this.logSum);
	}

	public int getNumberOfPtLegs() {
		return numberOfPtLegs;
	}

	public int getNumberOfCarLegs() {
		return numberOfCarLegs;
	}
	
	public int getNumberOfWalkLegs() {
		return numberOfWalkLegs;
	}
	
	public void setNumberOfPtLegs(int numberOfPtLegs) {
		this.numberOfPtLegs = numberOfPtLegs;
	}

	public void setNumberOfCarLegs(int numberOfCarLegs) {
		this.numberOfCarLegs = numberOfCarLegs;
	}

	public void setNumberOfWalkLegs(int numberOfWalkLegs) {
		this.numberOfWalkLegs = numberOfWalkLegs;
	}

	public void setLogSum(double logSum) {
		this.logSum = logSum;
	}

	public double getLogSum() {
		return logSum;
	}

	public void setAvgExecScore(double avgExecScore) {
		this.avgExecScore = avgExecScore;
	}

	public double getAvgExecScore() {
		return avgExecScore;
	}
}
