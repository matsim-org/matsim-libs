/* *********************************************************************** *
 * project: org.matsim.*
 * DecisionDataProvider.java
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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;

/**
 * Provides data which is used by the evacuation decision models.
 * 
 * @author cdobler
 */
public class DecisionDataProvider {
	
	private final Map<Id, PersonDecisionData> personDecisionData;
	private final Map<Id, HouseholdDecisionData> householdDecisionData;
	
	public DecisionDataProvider() {
		this.personDecisionData = new ConcurrentHashMap<Id, PersonDecisionData>();
		this.householdDecisionData = new ConcurrentHashMap<Id, HouseholdDecisionData>();
	}
	
	public PersonDecisionData getPersonDecisionData(Id personId) {
		return this.personDecisionData.get(personId);
	}
	
	public Collection<PersonDecisionData> getPersonDecisionData() {
		return this.personDecisionData.values();
	}
	
	public HouseholdDecisionData getHouseholdDecisionData(Id householdId) {
		return this.householdDecisionData.get(householdId);
	}
	
	public Collection<HouseholdDecisionData> getHouseholdDecisionData() {
		return this.householdDecisionData.values();
	}
	
	/*package*/ void addPersonDecisionData(Id personId, PersonDecisionData pdd) {
		this.personDecisionData.put(personId, pdd);
	}
	
	/*package*/ void addHouseholdDecisionData(Id householdId, HouseholdDecisionData hdd) {
		this.householdDecisionData.put(householdId, hdd);
	}
	
	public void reset() {
		this.personDecisionData.clear();
		this.householdDecisionData.clear();
	}
}
