/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
 */
package playground.vsp.congestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;


/**
 * Collects all link specific informations which are required for the calculation of external congestion effects.
 * 
 * @author ikaddoura
 *
 */
public class LinkCongestionInfo {
	
	private Id<Link> linkId;
	private double freeTravelTime;
	private double marginalDelayPerLeavingVehicle_sec;
	private int storageCapacity_cars;
	private Map<Id<Person>, Double> personId2linkLeaveTime = new HashMap<Id<Person>, Double>();
	private List<Id<Person>> leavingAgents = new ArrayList<Id<Person>>();
	private Map<Id<Person>, Double> personId2freeSpeedLeaveTime = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>, Double> personId2linkEnterTime = new HashMap<Id<Person>, Double>();
	private Id<Person> lastLeavingAgent;
	
	public Id<Link> getLinkId() {
		return linkId;
	}
	public void setLinkId(Id<Link> linkId) {
		this.linkId = linkId;
	}
	public void setMarginalDelayPerLeavingVehicle(double marginalDelay_sec) {
		this.marginalDelayPerLeavingVehicle_sec = marginalDelay_sec;
	}
	public int getStorageCapacity_cars() {
		return storageCapacity_cars;
	}
	public void setStorageCapacity_cars(int storageCapacity_cars) {
		this.storageCapacity_cars = storageCapacity_cars;
	}
	public double getMarginalDelayPerLeavingVehicle_sec() {
		return marginalDelayPerLeavingVehicle_sec;
	}
	public double getFreeTravelTime() {
		return freeTravelTime;
	}
	public void setFreeTravelTime(double freeTravelTime) {
		this.freeTravelTime = freeTravelTime;
	}
	public Map<Id<Person>, Double> getPersonId2linkLeaveTime() {
		return personId2linkLeaveTime;
	}
	public List<Id<Person>> getLeavingAgents() {
		return leavingAgents;
	}
	public Map<Id<Person>, Double> getPersonId2freeSpeedLeaveTime() {
		return personId2freeSpeedLeaveTime;
	}
	public void setPersonId2freeSpeedLeaveTime(
			Map<Id<Person>, Double> personId2freeSpeedLeaveTime) {
		this.personId2freeSpeedLeaveTime = personId2freeSpeedLeaveTime;
	}
	public Id<Person> getLastLeavingAgent() {
		return lastLeavingAgent;
	}
	public void setLastLeavingAgent(Id<Person> lastLeavingAgent) {
		this.lastLeavingAgent = lastLeavingAgent;
	}
	public Map<Id<Person>, Double> getPersonId2linkEnterTime() {
		return personId2linkEnterTime;
	}
	public void setPersonId2linkEnterTime(Map<Id<Person>, Double> personId2linkEnterTime) {
		this.personId2linkEnterTime = personId2linkEnterTime;
	}
		
}
