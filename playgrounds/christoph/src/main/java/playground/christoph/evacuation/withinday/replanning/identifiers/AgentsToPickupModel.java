/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsToPickupModel.java
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;

import playground.christoph.evacuation.config.EvacuationConfig;

public class AgentsToPickupModel {
	
	public enum PickupDecision {IFSPACE, ALWAYS, NEVER};
	
	private final Map<Id, PickupDecision> pickupDecisions;
	private final Random random;
	
	public AgentsToPickupModel(Scenario scenario, Random random) {
		
		this.pickupDecisions = new HashMap<Id, PickupDecision>();
		this.random = random;
		
		Households households = ((ScenarioImpl) scenario).getHouseholds();
		for (Household household : households.getHouseholds().values()) {
			
			/*
			 * Check whether household has children.
			 */
			boolean hasChildren = false;
			for (Id personId : household.getMemberIds()) {
				PersonImpl person = (PersonImpl) scenario.getPopulation().getPersons().get(personId);
				if (person.getAge() < 18) {
					hasChildren = true;
					break;
				}
			}
			
			/*
			 * Calculate pickup decision for every household member and
			 * store it in the map. For persons younger than 18 years
			 * always NEVER is used.
			 */
			for (Id personId : household.getMemberIds()) {
				PersonImpl person = (PersonImpl) scenario.getPopulation().getPersons().get(personId);
				int age = person.getAge();
				boolean drivingLicense = person.hasLicense();
				String sex = person.getSex();
				
				PickupDecision pickupDecision = runModel(age, drivingLicense, sex, hasChildren);
				this.pickupDecisions.put(personId, pickupDecision);
			}
		}
	}
	
	private PickupDecision runModel(int age, boolean drivingLicense, String sex, boolean hasChildren) {
		
		int is31To60 = 0;
		int is61To70 = 0;
		int is71plus = 0;
		int children = 0;
		int license = 0;
		int isFemale = 0;
		
		// People under 18 cannot pickup other people since they have no driving license
		if (age < 18) return PickupDecision.NEVER;
		
		if (age >= 31 && age <= 60) is31To60 = 1;
		else if (age >= 61 && age <= 70) is61To70 = 1;
		else if (age >= 71) is71plus = 1;
		
		if (hasChildren) children = 1;
		
		if (drivingLicense) license = 1;
		
		if (sex.toLowerCase().equals("f")) isFemale = 1;
		
		double V1 = EvacuationConfig.pickupModelIfSpaceConst + 
				is31To60 * EvacuationConfig.pickupModelIfSpaceAge31to60 +
				is61To70 * EvacuationConfig.pickupModelIfSpaceAge61to70 +
				is71plus * EvacuationConfig.pickupModelIfSpaceAge71plus +
				children * EvacuationConfig.pickupModelIfSpaceHasChildren +
				license * EvacuationConfig.pickupModelIfSpaceHasDrivingLicence +
				isFemale * EvacuationConfig.pickupModelIfSpaceIsFemale;
		
		double V2 = EvacuationConfig.pickupModelAlwaysConst + 
				is31To60 * EvacuationConfig.pickupModelAlwaysAge31to60 +
				is61To70 * EvacuationConfig.pickupModelAlwaysAge61to70 +
				is71plus * EvacuationConfig.pickupModelAlwaysAge71plus +
				children * EvacuationConfig.pickupModelAlwaysHasChildren +
				license * EvacuationConfig.pickupModelAlwaysHasDrivingLicence +
				isFemale * EvacuationConfig.pickupModelAlwaysIsFemale;
		
		double V3 = 0;
		
		double ExpV = Math.exp(V1) + Math.exp(V2) + Math.exp(V3);
		
		double ifSpace = Math.exp(V1) / ExpV;
		double always = Math.exp(V2) / ExpV;
//		double never = Math.exp(V3) / ExpV;
		
		double rand = random.nextDouble();
		if (rand < ifSpace) return PickupDecision.IFSPACE;
		else if (rand < ifSpace + always) return PickupDecision.ALWAYS;
		else return PickupDecision.NEVER;
	}
}
