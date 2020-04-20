/* *********************************************************************** *
 * project: org.matsim.*
 * WelfareCalculator.java
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
package playground.vsp.analysis.modules.userBenefits;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;

/**
 * @author ikaddoura, benjamin
 *
 */
public class UserBenefitsCalculator {
	private static final Logger logger = Logger.getLogger(UserBenefitsCalculator.class);

	private final double betaLogit;
	private final double marginalUtlOfMoney;
	private final WelfareMeasure welfareMeasure;
	private int nullScore = 0;
	private int minusScore = 0;
	private int personsWithoutValidPlanScore = 0;
	private final int maxWarnCnt = 3;
	private final boolean considerAllPlans;
	private final Map<Id<Person>, Double> personId2Utility = new HashMap<Id<Person>, Double>();
	private final Map<Id<Person>, Double> personId2MonetizedUtility = new HashMap<Id<Person>, Double>();

	private final Set<Id<Person>> stuckingAgents;

	public UserBenefitsCalculator(Config config, WelfareMeasure wm, boolean considerAllPlans) {
		PlanCalcScoreConfigGroup pcs = config.planCalcScore();
		this.betaLogit = pcs.getBrainExpBeta();
		this.marginalUtlOfMoney = pcs.getMarginalUtilityOfMoney();
		this.welfareMeasure = wm;
		this.considerAllPlans = considerAllPlans;
		
		logger.warn("There is no list of person IDs provided to be excluded from the user benefit calculation.");
		this.stuckingAgents = null;
		
		if (considerAllPlans) {
			logger.warn("All plans are considered for the calculation of user benefits. For an economic interpretation invalid plans (score <= 0.0 or score == null) should not be considered.");
		} else {
			logger.warn("Invalid plans that will not be considered for the calculation of user benefits are defined to have either a null score or a negative score.");
		}
	}
	
	public UserBenefitsCalculator(Config config, Set<Id<Person>> stuckingAgents) {
		logger.info("Providing the IDs of agents that are stucking in the final iteration (selected plans).");

		PlanCalcScoreConfigGroup pcs = config.planCalcScore();
		this.betaLogit = pcs.getBrainExpBeta();
		this.marginalUtlOfMoney = pcs.getMarginalUtilityOfMoney();
		this.stuckingAgents = stuckingAgents;

		logger.info("A list of person IDs of stucking agents is provided. These persons will be excluded from the user benefit calculation."
				+ "All other plans will be considered.");
		this.considerAllPlans = false;
		
		logger.info("The welfare measure is set to " + WelfareMeasure.SELECTED.toString() + ".");
		this.welfareMeasure = WelfareMeasure.SELECTED;
	}

	public void reset() {
		nullScore = 0;
		minusScore = 0;
		personsWithoutValidPlanScore = 0;
	}

	public double calculateUtility_utils(Population pop){
		double sumOfUtility_utils = 0.0;
		logger.info("Starting user benefits calculation...");
		logger.info("Benefits will be computed in units of UTILITY.");
		logger.info("The welfare measure is " + this.welfareMeasure + ".");
		for(Person person : pop.getPersons().values()){
			double utilityOfPerson_utils = calculateUtilityOfPerson_utils(person);
			this.personId2Utility.put(person.getId(), utilityOfPerson_utils);
			sumOfUtility_utils += utilityOfPerson_utils;
		}
		logger.info("Finished user benefits calculation...");
		return sumOfUtility_utils;
	}
	
	public double calculateUtility_money(Population pop) {
		double sumOfUtility_money = 0.0;
		logger.info("Starting user benefits calculation...");
		logger.info("Benefits will be computed in units of MONEY.");
		logger.info("The welfare measure is " + this.welfareMeasure + ".");
		for(Person person : pop.getPersons().values()){
			double utilityOfPerson_utils = calculateUtilityOfPerson_utils(person);
			this.personId2Utility.put(person.getId(), utilityOfPerson_utils);
			double utilityOfPerson_money = convertToMoney(utilityOfPerson_utils);
			this.personId2MonetizedUtility.put(person.getId(), utilityOfPerson_money);
			sumOfUtility_money += utilityOfPerson_money;
		}
		logger.info("Finished user benefits calculation...");
		return sumOfUtility_money;
	}

	public double calculateUtilityOfPerson_utils(Person person) {
		double utilityOfPerson_utils = 0.0;
		double sumOfExpScore = 0.0;
		double bestScore = Double.NEGATIVE_INFINITY;
		
		if(this.welfareMeasure.equals(WelfareMeasure.LOGSUM)){
			for(Plan plan : person.getPlans()){
				boolean shouldBeConsidered;
				if(this.considerAllPlans) {
					shouldBeConsidered = true;
				} else {
					shouldBeConsidered = testScore(plan, person.getId());
				}
				if(shouldBeConsidered){
					/* Benjamins version: */
//					double expScoreOfPlan = Math.exp(betaLogit * plan.getScore());
					/* Kais version: */
					bestScore = getBestScore(person);
					double expScoreOfPlan = Math.exp(betaLogit * (plan.getScore() - bestScore));
					
					sumOfExpScore += expScoreOfPlan;
				} else {
					// plan is not considered
				}
			}
			if(sumOfExpScore == 0.0){
				personsWithoutValidPlanScore++;
				if(personsWithoutValidPlanScore <= maxWarnCnt) {
					logger.warn("Person " + person.getId() + " has no valid plans. " +
							"This person's utility is " + utilityOfPerson_utils);
					if(personsWithoutValidPlanScore == maxWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED + "\n");
				}
			} else{
				/* Benjamins version: */
				// utilityOfPerson_utils = (1. / (betaLogit)) * Math.log(sumOfExpScore);
				/* Kais version: */
				utilityOfPerson_utils = (bestScore + (1. / betaLogit ) * Math.log(sumOfExpScore));
			}

		} else if(this.welfareMeasure.equals(WelfareMeasure.SELECTED)){
			Plan selectedPlan = person.getSelectedPlan();
			boolean shouldBeConsidered;
			if(this.considerAllPlans) {
				shouldBeConsidered = true;
			} else {
				shouldBeConsidered = testScore(selectedPlan, person.getId());
			}
			if(shouldBeConsidered){
				utilityOfPerson_utils = selectedPlan.getScore();
			} else {
				personsWithoutValidPlanScore++;
				if(personsWithoutValidPlanScore <= maxWarnCnt) {
					logger.warn("Person " + person.getId() + " has no valid selected plan. " +
							"This person's utility is " + utilityOfPerson_utils);
					if(personsWithoutValidPlanScore == maxWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED + "\n");
				}
			}
		}
		return utilityOfPerson_utils;
	}

	private boolean testScore(Plan plan, Id<Person> personId) {
		if (this.stuckingAgents == null){
			if(plan.getScore() == null){
				nullScore++;
				if(nullScore <= maxWarnCnt) {
					logger.warn("Score for person " + personId + " is " + plan.getScore() 
							+ ". A null score cannot be used for utility calculation.");
					if(nullScore == maxWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED + "\n");
				}
				return false;
			} else if(plan.getScore() <= 0.0){
				minusScore++;
				if(minusScore <= maxWarnCnt) {
					logger.warn("Score for person " + personId + " is " + plan.getScore() 
							+ ". A score <= 0.0 cannot be used for utility calculation.");
					if(minusScore == maxWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED + "\n");
				}
				return false;
			} else return true;
		} else {
			if (plan.getScore() == null){
				throw new RuntimeException("Score for person " + personId + " is " + plan.getScore() 
						+ ". A null score cannot be used for utility calculation. This person has to be excluded.");
			} else {
				if (this.stuckingAgents.contains(personId)) {
					minusScore++;
					if(minusScore <= maxWarnCnt) {
						logger.warn("Person " + personId + " is stucking and therefore cannot be used for utility calculation.");
						if(minusScore == maxWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED + "\n");
					}

					return false;
				} else return true;
			}

		}
	}

	private double convertToMoney(double logsumOfPerson) {
		return logsumOfPerson / marginalUtlOfMoney;
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

	public int getPersonsWithoutValidPlanCnt() {
		if (this.considerAllPlans) {
			logger.warn("All plans are forced to be considered. Asking for the number of persons without a valid plan doesn't make sense.");
			return Integer.MIN_VALUE;
		} else {
			return personsWithoutValidPlanScore;
		}
	}
	
	public int getInvalidPlans() {
		if (this.considerAllPlans) {
			logger.warn("All plans are forced to be considered. Asking for the number of invalid plans doesn't make sense.");
			return Integer.MIN_VALUE;
		} else {
			int invalidPlans = this.minusScore + this.nullScore;
			return invalidPlans;
		}
	}
	
	public Map<Id<Person>, Double> getPersonId2Utility() {
		return personId2Utility;
	}
	
	public Map<Id<Person>, Double> getPersonId2MonetizedUtility() {
		return personId2MonetizedUtility;
	}
}
