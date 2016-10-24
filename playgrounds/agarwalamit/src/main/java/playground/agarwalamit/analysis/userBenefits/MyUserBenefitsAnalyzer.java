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
package playground.agarwalamit.analysis.userBenefits;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.analysis.modules.AbstractAnalysisModule;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsAnalyzer;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

/**
 * This module calculates the logsum for each user and the sum of all user logsums in monetary units.
 * Furthermore, it analyzes users with no valid plan, that are not considered for the logsum calculation.
 * 
 * @author amit after ikaddoura, benjamin
 *
 */
public class MyUserBenefitsAnalyzer extends AbstractAnalysisModule{
	
	private static final Logger LOG = Logger.getLogger(UserBenefitsAnalyzer.class);
	private MutableScenario scenario;
	private UserBenefitsCalculator userWelfareCalculator;
	
	private double allUsersLogSum;
	private int personWithNoValidPlanCnt;
	private Map<Id<Person>, Double> personId2UserWelfare;
	private Map<Id<Person>, Double> personId2MonetarizedUserWelfare;
	private WelfareMeasure welfareMeasure;
	
	public MyUserBenefitsAnalyzer() {
		super(MyUserBenefitsAnalyzer.class.getSimpleName());
	}
	
	/**
	 * @param scenario
	 * @param welfareMeasure user welfare or logsum
	 * @param considerAllPlans include plans with negative or null score or not
	 */
	public void init(final MutableScenario scenario, final WelfareMeasure welfareMeasure, final boolean considerAllPlans) {
		this.scenario = scenario;
		this.welfareMeasure = welfareMeasure;
		this.userWelfareCalculator = new UserBenefitsCalculator(this.scenario.getConfig(), this.welfareMeasure, considerAllPlans);
		this.userWelfareCalculator.reset();
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		return new LinkedList<>();
	}

	@Override
	public void preProcessData() {
		this.allUsersLogSum = this.userWelfareCalculator.calculateUtility_money(this.scenario.getPopulation());
		this.personWithNoValidPlanCnt = this.userWelfareCalculator.getPersonsWithoutValidPlanCnt();
		
		LOG.warn("users with no valid plan (all scores ``== null'' or ``<= 0.0''): " + this.personWithNoValidPlanCnt);
		
		this.personId2MonetarizedUserWelfare = this.userWelfareCalculator.getPersonId2MonetizedUtility();
		this.personId2UserWelfare = this.userWelfareCalculator.getPersonId2Utility();
	}

	@Override
	public void postProcessData() {
	}

	@Override
	public void writeResults(final String outputFolder) {
		String fileName = outputFolder + "userBenefits"+this.welfareMeasure+".txt";
		File file = new File(fileName);
				
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("monetary user benefits (all users logsum): " + this.allUsersLogSum);
			bw.newLine();
			bw.write("users with no valid plan (all scores ``== null'' or ``<= 0.0''): " + this.personWithNoValidPlanCnt);
			bw.newLine();
			
			bw.newLine();
			bw.write("userID \t userWelfare_utils \t monetary user logsum");
			bw.newLine();
			
			for (Id<Person> id : this.personId2UserWelfare.keySet()){
				String row = id + "\t" + this.personId2UserWelfare.get(id)+"\t"+this.personId2MonetarizedUserWelfare.get(id);
				bw.write(row);
				bw.newLine();
			}
			
			bw.close();
			LOG.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public double getTotalUserWelfareMoney() {
		return this.allUsersLogSum;
	}

	public Map<Id<Person>, Double> getPersonId2UserWelfareUtils() {
		return personId2UserWelfare;
	}
	
	public Map<Id<Person>, Double> getPersonId2MonetarizedUserWelfare(){
		return personId2MonetarizedUserWelfare;
	}
}

