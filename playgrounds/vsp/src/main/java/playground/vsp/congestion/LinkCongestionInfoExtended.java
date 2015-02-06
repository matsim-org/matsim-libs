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
package playground.vsp.congestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * @author amit
 */
public class LinkCongestionInfoExtended extends LinkCongestionInfo{

	private List<Id<Person>> enteringAgents = new ArrayList<Id<Person>>();
	private Map<Id<Person>, Double> personId2DelaysToPayFor = new HashMap<Id<Person>, Double>();
	private Map<Id<Person>, Id<Link>> personId2CausingLinkId = new HashMap<>();
	private double storageCapacityCars;
	private Id<Person> lastEnteredAgent;
	private double lastLeaveTime;

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

	public boolean isLinkFree(double time){
		double earliestLeaveTime = Math.floor(lastLeaveTime+super.getMarginalDelayPerLeavingVehicle_sec())+1;
		if(time> earliestLeaveTime){
			return true;
		} else return false; 
	}
}
