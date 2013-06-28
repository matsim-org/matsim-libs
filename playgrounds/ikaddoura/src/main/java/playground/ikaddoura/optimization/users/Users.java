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
package playground.ikaddoura.optimization.users;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;

/**
 * @author Ihab after benjamin
 *
 */
public class Users {
	private final static Logger log = Logger.getLogger(Users.class);

	private double betaLogit;
	private double marginalUtlOfMoney;
	
	private int nullScore = 0;
	private int minusScore = 0;
	private int noValidPlanScore = 0;
	private final int maxWarnCnt = 3;
	
	private double logSum;
	private Scenario scenario;


	public void calculateLogsum() {
		
		Population population = scenario.getPopulation();
		
		for(Person person : population.getPersons().values()){
			double logsumOfPerson = calculateLogsumOfPerson(person);
			this.logSum += logsumOfPerson;
		}
	}

	private double calculateLogsumOfPerson(Person person) {
		double logsumOfPerson = 0.0;
		double sumOfExpScore = 0.0;

		double bestScore = Double.NEGATIVE_INFINITY;
		
		for(Plan plan : person.getPlans()){
			if(plan.getScore() == null){
				nullScore++;
				if(nullScore <= maxWarnCnt) {
					log.warn("Score for person " + person.getId() + " is " + plan.getScore() 
							+ ". The score cannot be used for utility calculation.");
					if(nullScore == maxWarnCnt) log.warn(Gbl.FUTURE_SUPPRESSED + "\n");
				}
			} else if(plan.getScore() <= 0.0){
				minusScore++;
				if(minusScore <= maxWarnCnt) {
					log.warn("Score for person " + person.getId() + " is " + plan.getScore() 
							+ ". The score cannot be used for utility calculation.");
					if(minusScore == maxWarnCnt) log.warn(Gbl.FUTURE_SUPPRESSED + "\n");
				}
			} else{
				bestScore = getBestScore(person);
				double expScoreOfPlan = Math.exp(betaLogit * (plan.getScore() - bestScore));
				
				sumOfExpScore += expScoreOfPlan;
			}
		}
		if(sumOfExpScore == 0.0){
			noValidPlanScore++;
			if(noValidPlanScore <= maxWarnCnt) {
				log.warn("Person " + person.getId() + " has no valid plans. " +
						"This person is not considered for utility calculations.");
				if(noValidPlanScore == maxWarnCnt) log.warn(Gbl.FUTURE_SUPPRESSED + "\n");
			}
		} else{
			logsumOfPerson = (bestScore + (1. / betaLogit ) * Math.log(sumOfExpScore)) / marginalUtlOfMoney;
		}
		return logsumOfPerson;
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

	public double getLogSum() {
		return logSum;
	}
	
	public int getNoValidPlanScore() {
		return noValidPlanScore;
	}
	
	public void setParametersForExtIteration(Scenario scenario) {
		this.scenario = scenario;
		this.marginalUtlOfMoney = scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		this.betaLogit = scenario.getConfig().planCalcScore().getBrainExpBeta();
		
		this.logSum = 0.0;
		
		this.nullScore = 0;
		this.minusScore = 0;
		this.noValidPlanScore = 0;
	}
	
}
