/* *********************************************************************** *
 * project: org.matsim.*
 * DecisionDataGrabber.java
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

package playground.christoph.evacuation.mobsim.decisiondata;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.households.Household;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.controler.EvacuationConstants;
import playground.christoph.evacuation.mobsim.AgentPosition;
import playground.christoph.evacuation.mobsim.HouseholdPosition;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;

/**
 * Updates evacuation related data in DecisionData which is used by the decision models.
 * 
 * @author cdobler
 */
public class DecisionDataGrabber {

	private final Scenario scenario;
	private final CoordAnalyzer coordAnalyzer;
	private final HouseholdsTracker householdsTracker;
	private final ObjectAttributes householdObjectAttributes;
	
	public DecisionDataGrabber(Scenario scenario, CoordAnalyzer coordAnalyzer, 
			HouseholdsTracker householdsTracker, ObjectAttributes householdObjectAttributes) {
		this.scenario = scenario;
		this.coordAnalyzer = coordAnalyzer;
		this.householdsTracker = householdsTracker;
		this.householdObjectAttributes = householdObjectAttributes;
	}
		
	/*
	 * Initial data collection.
	 */
	public void grabDecisionData(DecisionDataProvider decisionDataProvider) {
		
		decisionDataProvider.reset();
		
		// create empty decision data objects
		createDecisionData(decisionDataProvider);
		
		// collect decision data and fill data objects
		collectDecisionData(decisionDataProvider);
	}
	
	private void createDecisionData(DecisionDataProvider decisionDataProvider) {
		
		for (Household household : ((ScenarioImpl) scenario).getHouseholds().getHouseholds().values()) {

			Id householdId = household.getId();
			
			HouseholdDecisionData hdd = new HouseholdDecisionData(householdId);
			decisionDataProvider.addHouseholdDecisionData(householdId, hdd);
			
			for (Id personId : household.getMemberIds()) {
				PersonDecisionData pdd = new PersonDecisionData(personId);
				pdd.setHouseholdId(householdId);
				decisionDataProvider.addPersonDecisionData(personId, pdd);
			}
		}
	}
	
	private void collectDecisionData(DecisionDataProvider decisionDataProvider) {
		
		for (Household household : ((ScenarioImpl) scenario).getHouseholds().getHouseholds().values()) {
			
			// Check whether household has children and identify possible parents (= oldest male and female household members).
			boolean hasChildren = false;
			Id oldestMaleId = null;
			Id oldestFemaleId = null;
			int oldestMaleAge = 0;
			int oldestFemaleAge = 0;
			for (Id personId : household.getMemberIds()) {
				Person person = scenario.getPopulation().getPersons().get(personId);
				if (PersonUtils.getAge(person) < 18) {
					hasChildren = true;
				} else {
					if (PersonUtils.getSex(person).equals("f")) {
						if (PersonUtils.getAge(person) > oldestFemaleAge) {
							oldestFemaleAge = PersonUtils.getAge(person);
							oldestFemaleId = person.getId();
						}
					} else if (PersonUtils.getSex(person).equals("m")) {
						if (PersonUtils.getAge(person) > oldestMaleAge) {
							oldestMaleAge = PersonUtils.getAge(person);
							oldestMaleId = person.getId();
						}
					} else {
						throw new RuntimeException("Unknown gender type was found: " + PersonUtils.getSex(person));
					}
				}
			}
			// define oldest male and female household members as parents if the household contains children
			if (hasChildren) {
				if (oldestMaleId != null) decisionDataProvider.getPersonDecisionData(oldestMaleId).setChildren(true);
				if (oldestFemaleId != null) decisionDataProvider.getPersonDecisionData(oldestFemaleId).setChildren(true);
			}
			
			HouseholdDecisionData hdd = decisionDataProvider.getHouseholdDecisionData(household.getId());			

			String homeFacilityIdString = this.householdObjectAttributes.getAttribute(household.getId().toString(), 
					EvacuationConstants.HOUSEHOLD_HOMEFACILITYID).toString();
			Id<ActivityFacility> homeFacilityId = Id.create(homeFacilityIdString, ActivityFacility.class);
			ActivityFacility homeFacility = this.scenario.getActivityFacilities().getFacilities().get(homeFacilityId);
			
			HouseholdPosition householdPosition = this.householdsTracker.getHouseholdPosition(household.getId());
			hdd.setHouseholdPosition(householdPosition);
			
			hdd.setChildren(hasChildren);
			hdd.setHomeFacilityId(homeFacilityId);
			hdd.setHomeLinkId(homeFacility.getLinkId());
			hdd.setHomeFacilityIsAffected(this.coordAnalyzer.isFacilityAffected(homeFacility));
			
			for (Id personId : household.getMemberIds()) {
				PersonDecisionData pdd = decisionDataProvider.getPersonDecisionData(personId);
				AgentPosition agentPosition = this.householdsTracker.getAgentPosition(personId);
				pdd.setAgentPosition(agentPosition);
			}
		}
	}
}