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
 * @author ikaddoura, amit
 *
 */
public final class LinkCongestionInfo {
	
	private Id<Link> linkId;
	private double freeTravelTime;
	private double marginalDelayPerLeavingVehicle_sec;
	private Map<Id<Person>, Double> personId2linkLeaveTime = new HashMap<Id<Person>, Double>();
	private List<Id<Person>> leavingAgents = new ArrayList<Id<Person>>();
	private Map<Id<Person>, Double> personId2freeSpeedLeaveTime = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>, Double> personId2linkEnterTime = new HashMap<Id<Person>, Double>();
	private Id<Person> lastLeavingAgent;

	private List<Id<Person>> enteringAgents = new ArrayList<Id<Person>>();
	private Map<Id<Person>, Double> personId2DelaysToPayFor = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>, Id<Link>> personId2CausingLinkId = new HashMap<>();
	private double storageCapacityCars;
	private Id<Person> lastEnteredAgent;
	private double lastLeaveTime;
	
	private List<Id<Link>> spillBackCausingLinks = new ArrayList<Id<Link>>();
	private List<Id<Person>> agentsCausingFlowDelays = new ArrayList<Id<Person>>();
	
	public Id<Link> getLinkId() {
		return linkId;
	}
	public void setLinkId(Id<Link> linkId) {
		this.linkId = linkId;
	}
	public void setMarginalDelayPerLeavingVehicle(double marginalDelay_sec) {
		this.marginalDelayPerLeavingVehicle_sec = marginalDelay_sec;
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
	public double getLastLeaveTime() {
		return lastLeaveTime;
	}

	public void setLastLeaveTime(double lastLeaveTime) {
		this.lastLeaveTime = lastLeaveTime;
	}

	public Id<Person> getLastEnteredAgent() {
		return lastEnteredAgent;
	}

	public void setLastEnteredAgent(Id<Person> lastEnteredAgent) {
		this.lastEnteredAgent = lastEnteredAgent;
	}

	public List<Id<Person>> getEnteringAgents() {
		return enteringAgents;
	}

	public Map<Id<Person> , Double> getPersonId2DelaysToPayFor(){
		return personId2DelaysToPayFor;
	}

	public Map<Id<Person>, Id<Link>> getPersonId2CausingLinkId(){
		return personId2CausingLinkId;
	}

	public double getStorageCapacityCars() {
		return storageCapacityCars;
	}

	public void setStorageCapacityCars(double storageCapacityCars) {
		this.storageCapacityCars = storageCapacityCars;
	}

	public boolean isLinkFree(Id<Person> nowLeavingAgent, double time){
		boolean isLinkFree = false;
		if(lastLeavingAgent == null) return true;
		
		double freeSpeedLeaveTimeOfLastLeftAgent = this.personId2freeSpeedLeaveTime.get(lastLeavingAgent);
		double freeSpeedLeaveTimeOfNowAgent = this.personId2freeSpeedLeaveTime.get(nowLeavingAgent);
		double timeHeadway = freeSpeedLeaveTimeOfNowAgent -  freeSpeedLeaveTimeOfLastLeftAgent;
		double minTimeHeadway = this.marginalDelayPerLeavingVehicle_sec;
		
		if (timeHeadway < minTimeHeadway) isLinkFree = false;
		else isLinkFree = true;
		
		double earliestLeaveTime = Math.floor(lastLeaveTime+ marginalDelayPerLeavingVehicle_sec) +1;
		if(time > earliestLeaveTime){
			isLinkFree = true;
		} else isLinkFree = false;
		
		return isLinkFree;
	}
	
	public List<Id<Link>> getSpillBackCausingLinks() {
		return spillBackCausingLinks;
	}
	public List<Id<Person>> getAgentsCausingFlowDelays() {
		return agentsCausingFlowDelays;
	}
		
}
