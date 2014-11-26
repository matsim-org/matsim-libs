/* *********************************************************************** *
 * project: org.matsim.*
 * UserGroupUtils.java
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
package playground.benjamin.scenarios.munich.analysis.filter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;

import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

/**
 * @author benjamin
 *
 */
public class UserGroupUtils {
	private static final Logger logger = Logger.getLogger(UserGroupUtils.class);
	
	public PersonFilter personFilter = new PersonFilter();

	public UserGroupUtils() {
		this.personFilter = new PersonFilter();
	}
	
	public <T> SortedMap<UserGroup, SortedMap<String, Double>> getEmissionsPerGroup(Map<Id<T>, SortedMap<String, Double>> person2TotalEmissions) {
		SortedMap<UserGroup, SortedMap<String, Double>> userGroup2Emissions = new TreeMap<UserGroup, SortedMap<String, Double>>();
		
		for(UserGroup userGroup : UserGroup.values()){
			SortedMap<String, Double> totalEmissions = new TreeMap<String,Double>();
			for(Id<T> personId : person2TotalEmissions.keySet()){
				SortedMap<String, Double> individualEmissions = person2TotalEmissions.get(personId);
				if(personFilter.isPersonIdFromUserGroup(personId, userGroup)){
					double sumOfPollutant;
					for(String pollutant : individualEmissions.keySet()){
						if(totalEmissions.containsKey(pollutant)){
							sumOfPollutant = totalEmissions.get(pollutant) + individualEmissions.get(pollutant);
						} else {
							sumOfPollutant = individualEmissions.get(pollutant);
						}
						totalEmissions.put(pollutant, sumOfPollutant);
					}
				}
			}
			userGroup2Emissions.put(userGroup, totalEmissions);
		}
		return userGroup2Emissions;
	}

	public Map<UserGroup, Double> getSizePerGroup(Population pop) {
		Map<UserGroup, Double> userGroup2Size = new HashMap<UserGroup, Double>();
		
		for(UserGroup userGroup : UserGroup.values()){
			double groupSize = 0.0;
			
			for(Id<Person> personId : pop.getPersons().keySet()){
				if(personFilter.isPersonIdFromUserGroup(personId, userGroup)){
					groupSize++;
				}
			}
			userGroup2Size.put(userGroup, groupSize);
		}
		return userGroup2Size;
	}

	public Map<UserGroup, Double> getNrOfTollPayersPerGroup(Map<Id, Double> personId2Toll) {
		Map<UserGroup, Double> userGroup2TollPayers = new HashMap<UserGroup, Double>();

		for(UserGroup userGroup : UserGroup.values()){
			double groupSize = 0.0;

			for(Id personId : personId2Toll.keySet()){
				if(personFilter.isPersonIdFromUserGroup(personId, userGroup)){
					groupSize++;
				}
			}
			userGroup2TollPayers.put(userGroup, groupSize);
		}
		return userGroup2TollPayers;
	}

	public Map<UserGroup, Double> getUserLogsumPerGroup(Scenario scenario) {
		Map<UserGroup, Double> userGroup2Logsum = new HashMap<UserGroup, Double>();

		Config config = scenario.getConfig();
		UserBenefitsCalculator ubc = new UserBenefitsCalculator(config, WelfareMeasure.LOGSUM, false);
		
		for(UserGroup userGroup : UserGroup.values()){
			Population userGroupPop = personFilter.getPopulation(scenario.getPopulation(), userGroup);
			double userWelfareOfGroup = ubc.calculateUtility_money(userGroupPop);
			int personWithNoValidPlanCnt = ubc.getPersonsWithoutValidPlanCnt();
			logger.warn("users with no valid plan (all scores ``== null'' or ``<= 0.0'') in group " + userGroup + " : " + personWithNoValidPlanCnt);
			userGroup2Logsum.put(userGroup, userWelfareOfGroup);
		}
		return userGroup2Logsum;
	}

	public Map<UserGroup, Double> getTollPaymentsPerGroup(Map<Id, Double> personId2Toll) {
		Map<UserGroup, Double> userGroup2TollPayments = new HashMap<UserGroup, Double>();

		for(UserGroup userGroup : UserGroup.values()){
			double tollPayments = 0.0;

			for(Id personId : personId2Toll.keySet()){
				if(personFilter.isPersonIdFromUserGroup(personId, userGroup)){
					tollPayments += personId2Toll.get(personId);
				}
			}
			userGroup2TollPayments.put(userGroup, tollPayments);
		}
		return userGroup2TollPayments;
	}

	public Set<UserGroup> getUserGroups(Population pop){
	return getSizePerGroup(pop).keySet();
	}

}
