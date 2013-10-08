/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdVehiclesTrackerjava
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

package playground.christoph.evacuation.vehicles;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;

/**
 * Checks how many vehicles are used by one household at a time and creates a warn
 * message if the number of available vehicles is exceeded.
 * 
 * @author cdobler
 */
public class HouseholdVehiclesTracker implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private static final Logger log = Logger.getLogger(HouseholdVehiclesTracker.class);
	
	private final Scenario scenario;
	private final Households households;
	private final Map<Id, Id> personHousehold;
	private final Map<Id, HouseholdVehiclesInfo> householdVehicles;
	private final Map<Id, AtomicInteger> activeHouseHoldVehicles;
	private final Map<Id, AtomicInteger> vehicleUsage;	// vehicleId
	private final Set<Id> activeVehicles;
	private final Set<Id> exceededHouseholds;
	
	public HouseholdVehiclesTracker(Scenario scenario, Map<Id, HouseholdVehiclesInfo> householdVehicles) {
		this.scenario = scenario;
		this.householdVehicles = householdVehicles;
		
		this.households = ((ScenarioImpl) scenario).getHouseholds();
		
		personHousehold = new HashMap<Id, Id>();
		vehicleUsage = new HashMap<Id, AtomicInteger>();
		activeHouseHoldVehicles = new HashMap<Id, AtomicInteger>();
		activeVehicles = new HashSet<Id>();
		exceededHouseholds = new HashSet<Id>();
	}
	
	public void printClosingStatistics() {
		log.info("Number of household that used more vehicles than available: " + exceededHouseholds.size());
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		int usage = vehicleUsage.get(event.getVehicleId()).decrementAndGet();
		
		// vehicle arrives
		if (usage == 0) {
			activeVehicles.remove(event.getVehicleId());
			activeHouseHoldVehicles.get(personHousehold.get(event.getPersonId())).decrementAndGet();
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		int usage = vehicleUsage.get(event.getVehicleId()).incrementAndGet();
		
		// vehicles departs
		if (usage == 1) {
			activeVehicles.add(event.getVehicleId());
			Id householdId = personHousehold.get(event.getPersonId());
			int householdActive = activeHouseHoldVehicles.get(householdId).incrementAndGet();
			int maxVehicles = householdVehicles.get(householdId).getNumVehicles();
			if (householdActive > maxVehicles) {
				exceededHouseholds.add(householdId);
				log.warn("Too many vehicles active in household " + householdId.toString() + ". " +
						householdActive + " (active) vs " + maxVehicles + " (max).");
			}
		}
	}
	
	@Override
	public void reset(int iteration) {
		vehicleUsage.clear();
		activeVehicles.clear();
		personHousehold.clear();
		exceededHouseholds.clear();
		activeHouseHoldVehicles.clear();
		
		for (Household household : households.getHouseholds().values()) {
			// create mapping person -> person's household
			for (Id personId : household.getMemberIds()) personHousehold.put(personId, household.getId());
			
			// create initial number of active vehicles in a household
			activeHouseHoldVehicles.put(household.getId(), new AtomicInteger(0));
		}
		
		// create initial usage for all vehicles
		for (Person person : scenario.getPopulation().getPersons().values()) {
			vehicleUsage.put(person.getId(), new AtomicInteger(0));
		}
	}

}
