/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
 * @author ikaddoura
 * 
 */
package playground.vsp.analysis.modules.userBenefits;

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

/**
 * This module calculates the logsum for each user and the sum of all user logsums in monetary units.
 * Furthermore, it analyzes users with no valid plan, that are not considered for the logsum calculation.
 * 
 * @author ikaddoura, benjamin
 *
 */
public class UserBenefitsAnalyzer extends AbstractAnalysisModule{
	private final static Logger log = Logger.getLogger(UserBenefitsAnalyzer.class);
	private MutableScenario scenario;
	private UserBenefitsCalculator userWelfareCalculator;
	
	private double allUsersLogSum;
	private int personWithNoValidPlanCnt;
	private Map<Id<Person>, Double> personId2Logsum;

	public UserBenefitsAnalyzer() {
		super(UserBenefitsAnalyzer.class.getSimpleName());
	}
	
	public void init(MutableScenario scenario) {
		this.scenario = scenario;
		
		this.userWelfareCalculator = new UserBenefitsCalculator(this.scenario.getConfig(), WelfareMeasure.LOGSUM, false);
		this.userWelfareCalculator.reset();
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		// nothing to return
		return new LinkedList<EventHandler>();
	}

	@Override
	public void preProcessData() {
		this.allUsersLogSum = this.userWelfareCalculator.calculateUtility_money(this.scenario.getPopulation());
		this.personWithNoValidPlanCnt = this.userWelfareCalculator.getPersonsWithoutValidPlanCnt();
		log.warn("users with no valid plan (all scores ``== null'' or ``<= 0.0''): " + personWithNoValidPlanCnt);
		this.personId2Logsum = this.userWelfareCalculator.getPersonId2MonetizedUtility();
	}

	@Override
	public void postProcessData() {
		// nothing to do
	}

	@Override
	public void writeResults(String outputFolder) {
		String fileName = outputFolder + "userBenefits.txt";
		File file = new File(fileName);
				
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("monetary user benefits (all users logsum): " + this.allUsersLogSum);
			bw.newLine();
			bw.write("users with no valid plan (all scores ``== null'' or ``<= 0.0''): " + this.personWithNoValidPlanCnt);
			bw.newLine();
			
			bw.newLine();
			bw.write("userID \t monetary user logsum");
			bw.newLine();
			
			for (Id<Person> id : this.personId2Logsum.keySet()){
				String row = id + "\t" + this.personId2Logsum.get(id);
				bw.write(row);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public double getAllUsersLogSum() {
		return allUsersLogSum;
	}

	public Map<Id<Person>, Double> getPersonId2Logsum() {
		return personId2Logsum;
	}

	
	
}
