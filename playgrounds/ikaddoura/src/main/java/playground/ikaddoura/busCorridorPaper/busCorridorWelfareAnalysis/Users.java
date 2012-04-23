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
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author Ihab
 *
 */
public class Users {
	
	private final static Logger log = Logger.getLogger(Users.class);

	private double logSum;
	private String directoryExtIt;

	private Network network;
	private double marginalUtlOfMoney;

	public void calculateScore() {
		
		List<Double> nonStuckAgentScores = new ArrayList<Double>();
		
		String outputPlanFile = this.directoryExtIt + "/internalIterations/output_plans.xml.gz";		
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());		
		new MatsimPopulationReader(sc).readFile(outputPlanFile);
		Population population = sc.getPopulation();
		
		int numberOfAgentsWithOnlyInvalidPlans = 0;
		int numberOfInvalidPlans = 0;
		
		double logSumAllPersons = 0.0;
		for(Person person : population.getPersons().values()){
			double sumOfexpScore = 0.0;
			boolean allPlansInvalid = true;
			for (Plan plan : person.getPlans()){
				if (plan.getScore() <= -50){
					log.info("A plan of " + person.getId() + " is not used for LogSumUserScoring because Agent would stuck --> Score: " + plan.getScore());
					numberOfInvalidPlans++;
				} else {
					sumOfexpScore = sumOfexpScore + Math.exp(plan.getScore());
					allPlansInvalid = false;
				}
			}
			
			double logSumThisPerson = (1.0 / marginalUtlOfMoney) * Math.log(sumOfexpScore);
			
			if (allPlansInvalid){
				log.warn("All plans of " + person.getId()+" are not used for LogSum User Scoring. (LogSum for this person: " + logSumThisPerson + ")");
				numberOfAgentsWithOnlyInvalidPlans++;
			} else {
				logSumAllPersons = logSumAllPersons + logSumThisPerson;
			}
		}
		this.logSum = logSumAllPersons;
		
		if (numberOfAgentsWithOnlyInvalidPlans > 0){
			log.warn("Number of agents that are not used for LogSum User Scoring: " + numberOfAgentsWithOnlyInvalidPlans);
		}
		log.info("Number of plans that are not used for Welfare Scoring: " + numberOfInvalidPlans);
		log.info("User Score Sum (LogSum) calculated. "+this.logSum);
	}

	public double getLogSum() {
		return logSum;
	}

	public void setParametersForExtIteration(String directoryExtIt, Network network, double marginalUtilityOfMoney) {
		this.directoryExtIt = directoryExtIt;
		this.network = network;
		this.marginalUtlOfMoney = marginalUtilityOfMoney;	
	}
}
