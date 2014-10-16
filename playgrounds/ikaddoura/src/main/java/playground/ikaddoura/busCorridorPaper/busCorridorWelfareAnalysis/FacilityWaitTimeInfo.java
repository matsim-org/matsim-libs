/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityInfo.java
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

/**
 * 
 */
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author Ihab
 *
 */
public class FacilityWaitTimeInfo {
	
	private Id<TransitStopFacility> facilityId;
	private int numberOfWaitingTimesMoreThanHeadway;
	private int numberOfMissedVehicles;
	private SortedMap<Id<Event>, Double> waitingEvent2WaitingTime = new TreeMap<>();
	private SortedMap<Id<Event>, Double> waitingEvent2DayTime = new TreeMap<>();
	private SortedMap<Id<Event>, Id<Person>> waitingEvent2PersonId = new TreeMap<>();
	
	public Id<TransitStopFacility> getFacilityId() {
		return facilityId;
	}
	
	public void setFacilityId(Id<TransitStopFacility> facilityId) {
		this.facilityId = facilityId;
	}
	
	public SortedMap<Id<Event>, Double> getWaitingEvent2WaitingTime() {
		return waitingEvent2WaitingTime;
	}
	
	public void setWaitingEvent2WaitingTime(
			SortedMap<Id<Event>, Double> waitingEvent2WaitingTime) {
		this.waitingEvent2WaitingTime = waitingEvent2WaitingTime;
	}
	
	public SortedMap<Id<Event>, Double> getWaitingEvent2DayTime() {
		return waitingEvent2DayTime;
	}
	
	public void setWaitingEvent2DayTime(SortedMap<Id<Event>, Double> waitingEvent2DayTime) {
		this.waitingEvent2DayTime = waitingEvent2DayTime;
	}
	
	public Double getSumOfWaitingTimes() {
		Double sumOfWaitingTimes = 0.;
		for (Double waitingTime : this.waitingEvent2WaitingTime.values()){
			sumOfWaitingTimes = sumOfWaitingTimes + waitingTime;
		}
		return sumOfWaitingTimes;
	}
	
	public Double getAvgWaitingTime() {
		return this.getSumOfWaitingTimes() / this.waitingEvent2WaitingTime.size();
	}

	public void setNumberOfWaitingTimesMoreThanHeadway(
			int numberOfWaitingTimesMoreThanHeadway) {
		this.numberOfWaitingTimesMoreThanHeadway = numberOfWaitingTimesMoreThanHeadway;
	}

	public int getNumberOfWaitingTimesMoreThanHeadway() {
		return numberOfWaitingTimesMoreThanHeadway;
	}

	public void setNumberOfMissedVehicles(int numberOfMissedVehicles) {
		this.numberOfMissedVehicles = numberOfMissedVehicles;
	}

	public int getNumberOfMissedVehicles() {
		return numberOfMissedVehicles;
	}

	public void setWaitingEvent2PersonId(SortedMap<Id<Event>, Id<Person>> waitingEvent2PersonId) {
		this.waitingEvent2PersonId = waitingEvent2PersonId;
	}

	public SortedMap<Id<Event>, Id<Person>> getWaitingEvent2PersonId() {
		return waitingEvent2PersonId;
	}
	
}


