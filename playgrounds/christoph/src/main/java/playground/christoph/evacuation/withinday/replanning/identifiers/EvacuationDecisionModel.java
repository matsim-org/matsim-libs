/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationDecisionModel.java
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

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.config.EvacuationConfig.EvacuationReason;
import playground.christoph.evacuation.config.EvacuationConfig.PreEvacuationTime;
import playground.christoph.evacuation.mobsim.HouseholdPosition;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;

public class EvacuationDecisionModel {
	
	public enum EvacuationDecision {IMMEDIATELY, LATER, NEVER};

	private final Scenario scenario;
	private final Random random;
	private final HouseholdsTracker householdsTracker;
	
	public EvacuationDecisionModel(Scenario scenario, Random random, HouseholdsTracker householdsTracker) {
		this.scenario = scenario;
		this.random = random;
		this.householdsTracker = householdsTracker;
	}
	
	public EvacuationDecision runModel(Id householdId) {
		
		Household household = ((ScenarioImpl) scenario).getHouseholds().getHouseholds().get(householdId);
		
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
		 * Check whether household is joined.
		 */
		HouseholdPosition householdPosition = this.householdsTracker.getHouseholdPosition(householdId);
		boolean isJoined = householdPosition.isHouseholdJoined();
			
		/*
		 * Calculate pickup decision for every household member and
		 * store it in the map. For persons younger than 18 years
		 * always NEVER is used.
		 */
		int immediately = 0;
		int later = 0;
		int never = 0;
		Map<Id, EvacuationDecision> evacuationDecisions = new HashMap<Id, EvacuationDecision>();
		for (Id personId : household.getMemberIds()) {
			PersonImpl person = (PersonImpl) scenario.getPopulation().getPersons().get(personId);
			int age = person.getAge();
			boolean drivingLicense = person.hasLicense();
			
			EvacuationDecision evacuationDecision = runModel(age, drivingLicense, hasChildren, isJoined);
			evacuationDecisions.put(personId, evacuationDecision);
		}
		
		// Decide based on monte carlo simulation.
		double total = immediately + later + never;
		double rand = random.nextDouble();
		if (rand < (immediately / total)) return EvacuationDecision.IMMEDIATELY;
		else if (rand < ((immediately + later) / total)) return EvacuationDecision.LATER;
		else return EvacuationDecision.NEVER;
	}
	
	private EvacuationDecision runModel(int age, boolean drivingLicense, boolean hasChildren, boolean joined) {
		
		int is31To60 = 0;
		int is61plus = 0;
		int children = 0;
		int license = 0;
		int isAtomic = 0;
		int isFire = 0;
		int isChemical = 0;
		int isTime8 = 0;
		int isTime16 = 0;
		int isJoined = 0;
			
		if (age >= 31 && age <= 60) is31To60 = 1;
		else if (age >= 61) is61plus = 1;
		
		if (hasChildren) children = 1;
		
		if (drivingLicense) license = 1;
		
		if (joined) isJoined = 1;
		
		EvacuationReason reason = EvacuationConfig.leaveModelEvacuationReason;
		if (reason == EvacuationConfig.EvacuationReason.ATOMIC) isAtomic = 1;
		else if (reason == EvacuationConfig.EvacuationReason.FIRE) isFire = 1;
		else if (reason == EvacuationConfig.EvacuationReason.CHEMICAL) isChemical = 1;
		
		/*
		 * TODO for later studies: check whether linear interpolation for the
		 * PreEvacuationTime is possible.
		 */
		PreEvacuationTime time = EvacuationConfig.leaveModelPreEvacuationTime;
		if (time == EvacuationConfig.PreEvacuationTime.TIME8) isTime8 = 1;
		else if (time == EvacuationConfig.PreEvacuationTime.TIME16) isTime16 = 1;
				
		double V1 = EvacuationConfig.leaveModelImmediatelyConst + 
				isAtomic * EvacuationConfig.leaveModelImmediatelyAtomic +
				isChemical * EvacuationConfig.leaveModelImmediatelyChemical +
				isFire * EvacuationConfig.leaveModelImmediatelyFire + 
				is31To60 * EvacuationConfig.leaveModelImmediatelyAge31to60 +
				is61plus * EvacuationConfig.leaveModelImmediatelyAge61plus +
				isTime8 * EvacuationConfig.leaveModelImmediatelyTime8 * (1 + isJoined * EvacuationConfig.leaveModelImmediatelyHouseholdUnited1) +
				isTime16 * EvacuationConfig.leaveModelImmediatelyTime16 * (1 + isJoined * EvacuationConfig.leaveModelImmediatelyHouseholdUnited2) +			
				children * EvacuationConfig.leaveModelHasChildren +
				license * EvacuationConfig.leaveModelHasDrivingLicense;
		
		double V2 = EvacuationConfig.leaveModelLaterConst + 
				isAtomic * EvacuationConfig.leaveModelLaterAtomic +
				isChemical * EvacuationConfig.leaveModelLaterChemical +
				isFire * EvacuationConfig.leaveModelLaterFire + 
				is31To60 * EvacuationConfig.leaveModelLaterAge31to60 +
				is61plus * EvacuationConfig.leaveModelLaterAge61plus +
				isTime8 * EvacuationConfig.leaveModelLaterTime8 * (1 + isJoined * EvacuationConfig.leaveModelLaterHouseholdUnited1) +
				isTime16 * EvacuationConfig.leaveModelLaterTime16 * (1 + isJoined * EvacuationConfig.leaveModelLaterHouseholdUnited2) +			
				children * EvacuationConfig.leaveModelHasChildren +
				license * EvacuationConfig.leaveModelHasDrivingLicense;
		
		double V3 = 0;
		
		double ExpV = Math.exp(V1) + Math.exp(V2) + Math.exp(V3);
		
		double immediately = Math.exp(V1) / ExpV;
		double later = Math.exp(V2) / ExpV;
//		double never = Math.exp(V3) / ExpV;
		
		double rand = random.nextDouble();
		if (rand < immediately) return EvacuationDecision.IMMEDIATELY;
		else if (rand < immediately + later) return EvacuationDecision.LATER;
		else return EvacuationDecision.NEVER;
	}
}
