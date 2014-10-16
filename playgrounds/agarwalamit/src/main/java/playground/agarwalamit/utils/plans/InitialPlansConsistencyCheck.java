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
package playground.agarwalamit.utils.plans;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */
public class InitialPlansConsistencyCheck {

	private final String initialPlansFile = "/Users/aagarwal/Desktop/ils4/agarwal/munich/input/mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4.xml.gz";
	private static final Logger log = Logger.getLogger(InitialPlansConsistencyCheck.class);

	private Map<Person, List<String>> person2ActivityType;
	private Map<Person, List<String>> person2Legs;
	private Map<UserGroup, Integer> userGroup2NumberOfPersons;
	private PersonFilter pf;
	private BufferedWriter writer;

	private void initializePlansFile (){
		writer = IOUtils.getBufferedWriter("/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/analysis/plansConsistency.txt");
		pf = new PersonFilter();
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(initialPlansFile);
		person2ActivityType = new HashMap<Person, List<String>>();
		person2Legs = new HashMap<Person, List<String>>();
		userGroup2NumberOfPersons = new HashMap<UserGroup, Integer>();

		for(UserGroup ug : UserGroup.values()){
			userGroup2NumberOfPersons.put(ug, 0);
		}

		for(Person p : sc.getPopulation().getPersons().values()){
			person2ActivityType.put(p, new ArrayList<String>());
			person2Legs.put(p, new ArrayList<String>());
			for(UserGroup ug : UserGroup.values()){
				if(pf.isPersonIdFromUserGroup(p.getId(), ug)) {
					int countSoFar = userGroup2NumberOfPersons.get(ug);
					userGroup2NumberOfPersons.put(ug, countSoFar+1);
					break;
				}
			}
		}

		for(Person p : sc.getPopulation().getPersons().values()){
			for(PlanElement pe : p.getSelectedPlan().getPlanElements()){
				if (pe instanceof Activity ) {
					List<String> acts = person2ActivityType.get(p);
					acts.add(((Activity) pe).getType());
				} else if (pe instanceof Leg ) {
					List<String> legs = person2Legs.get(p);
					legs.add(((Leg) pe).getMode());
				} else {
					throw new RuntimeException("Following plan elements is not included. "+pe.toString());
				}
			}
		}

		try {
			writer.write("UserGroup \t numberOfPersons \n");
			for(UserGroup ug : UserGroup.values()){
				writer.write(ug+"\t"+userGroup2NumberOfPersons.get(ug)+"\n");
			}
			writer.write("Total persons \t "+person2ActivityType.size()+"\n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to file. Reason "+e);
		}
	}

	private void equalityCheckFor1stAndLastActivity(){
		log.info("Consistency check for equality of first and last activity in a plan.");
		int warnCount =0;
		int urbanPersons =0;
		for(Person p:person2ActivityType.keySet()){
			List<String> acts = person2ActivityType.get(p);
			if(!acts.get(0).equals(acts.get(acts.size()-1))){
				warnCount++;
				if(pf.isPersonFromMID(p.getId())) urbanPersons++;
			}
		}
		try {
			writer.write("Number of persons not having first and last activity same \t "+warnCount+"\n");
			writer.write("Number of such persons from Urban population \t "+urbanPersons+"\n");
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to file. Reason "+e);
		}
		log.warn(warnCount+" number of persons do not have first and last activites same."
				+ "Out of them "+urbanPersons+" persons belong to urban user group."
				+ "\n The total number of person in populatino are "+person2ActivityType.size());
	}

	public static void main(String[] args) {
		InitialPlansConsistencyCheck plansCheck = new InitialPlansConsistencyCheck();
		plansCheck.initializePlansFile();
		plansCheck.equalityCheckFor1stAndLastActivity();
	}

}
