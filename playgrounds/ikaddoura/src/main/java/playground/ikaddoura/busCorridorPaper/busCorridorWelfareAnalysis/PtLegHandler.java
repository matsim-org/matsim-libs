/* *********************************************************************** *
 * project: org.matsim.*
 * InVehWaitHandler.java
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

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.pt.PtConstants;

/**
 * @author Ihab
 *
 */
public class PtLegHandler implements PersonEntersVehicleEventHandler, AgentDepartureEventHandler, AgentArrivalEventHandler, ActivityEndEventHandler, VehicleArrivesAtFacilityEventHandler {
	private final Map <Id, Double> personId2WaitingTime = new HashMap<Id, Double>();
	private final Map <Id, Double> personId2PersonEntersVehicleTime = new HashMap<Id, Double>();
	private final Map <Id, Double> personId2AgentDepartureTime = new HashMap<Id, Double>();
	private final Map <Id, Double> personId2InVehicleTime = new HashMap<Id, Double>();
	private final Map <Id, Id> busId2currentFacilityId = new HashMap<Id, Id>();
	private final Map <Id, FacilityInfo> facilityId2facilityInfos = new HashMap<Id, FacilityInfo>();

	private int numberOfWaitingTimesMoreThanHeadway;
	private int numberOfMissedVehicles;
	
	private final Map <Id, Boolean> personId2IsEgress = new HashMap<Id, Boolean>();
	
	private final double headway;
	private int waitingTimeCounter = 0;
	
	/**
	 * @param headway
	 */
	public PtLegHandler(double headway) {
		this.headway = headway;
	}

	@Override
	public void reset(int iteration) {
		personId2WaitingTime.clear();
		personId2PersonEntersVehicleTime.clear();
		personId2AgentDepartureTime.clear();
		personId2InVehicleTime.clear();
		busId2currentFacilityId.clear();
		facilityId2facilityInfos.clear();
		this.numberOfWaitingTimesMoreThanHeadway = 0;
		this.waitingTimeCounter = 0;
		this.numberOfMissedVehicles = 0;
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id personId = event.getPersonId();
		Id vehId = event.getVehicleId();
		
		if (personId.toString().contains("person") && vehId.toString().contains("bus")){
			personId2PersonEntersVehicleTime.put(personId, event.getTime());
			
			double waitingTime;
			if (personId2AgentDepartureTime.get(personId) == null){
				throw new RuntimeException("Person " + personId + " is entering vehicle " + vehId + " without having departed from an activity. Aborting...");
			} else {
				waitingTime =  event.getTime() - personId2AgentDepartureTime.get(personId);
			}
			
			// save waitingTime per person
			if (personId2WaitingTime.get(personId) == null){
				personId2WaitingTime.put(personId, waitingTime);
			} else {
				double waitingTimeSum = personId2WaitingTime.get(personId) + waitingTime;
				personId2WaitingTime.put(personId, waitingTimeSum);
			}
			
			// analyze waitingTime
//			System.out.println("Headway --------------> " + this.headway);
//			System.out.println("WaitingTime ----------> " + waitingTime);
			if (waitingTime > this.headway){
//				System.out.println("Total number of missed buses: " + this.numberOfMissedVehicles);
				this.numberOfWaitingTimesMoreThanHeadway++;
				int missed = (int) (waitingTime / this.headway);
//				System.out.println("Missed Busses --------> " + missed);
				this.numberOfMissedVehicles = this.numberOfMissedVehicles + missed;
//				System.out.println("New total number of missed buses: " + this.numberOfMissedVehicles);
			}
			
			// save waitingTime per stop
			Id currentFacilityId = this.busId2currentFacilityId.get(vehId);
//			System.out.println("Current TransitStopFacilityId of bus " + vehId + ": " + currentFacilityId);
			if (this.facilityId2facilityInfos.get(currentFacilityId) == null){
				FacilityInfo facilityInfo = new FacilityInfo();
				SortedMap<Id, Double> waitingEvent2WaitingTime = new TreeMap<Id, Double>();
				SortedMap<Id, Double> waitingEvent2DayTime = new TreeMap<Id, Double>();
				
				facilityInfo.setFacilityId(currentFacilityId);
				waitingEvent2WaitingTime.put(new IdImpl(waitingTimeCounter), waitingTime);
				facilityInfo.setWaitingEvent2WaitingTime(waitingEvent2WaitingTime);
				waitingEvent2DayTime.put(new IdImpl(waitingTimeCounter), event.getTime());
				facilityInfo.setWaitingEvent2DayTime(waitingEvent2DayTime);
				
				if (waitingTime > this.headway){
					facilityInfo.setNumberOfWaitingTimesMoreThanHeadway(1);
					int missed = (int) (waitingTime / this.headway);
					facilityInfo.setNumberOfMissedVehicles(missed);
				}
				
				this.facilityId2facilityInfos.put(currentFacilityId, facilityInfo);
				
			} else {
				FacilityInfo facilityInfo = this.facilityId2facilityInfos.get(currentFacilityId);
				SortedMap<Id, Double> waitingEvent2WaitingTime = facilityInfo.getWaitingEvent2WaitingTime();
				SortedMap<Id, Double> waitingEvent2DayTime = facilityInfo.getWaitingEvent2DayTime();
				
				waitingEvent2WaitingTime.put(new IdImpl(waitingTimeCounter), waitingTime);
				waitingEvent2DayTime.put(new IdImpl(waitingTimeCounter), event.getTime());
				facilityInfo.setWaitingEvent2WaitingTime(waitingEvent2WaitingTime);
				facilityInfo.setWaitingEvent2DayTime(waitingEvent2DayTime);
				
				if (waitingTime > this.headway){
					facilityInfo.setNumberOfWaitingTimesMoreThanHeadway(facilityInfo.getNumberOfWaitingTimesMoreThanHeadway() + 1);
					int missed = (int) (waitingTime / this.headway);
					facilityInfo.setNumberOfMissedVehicles(facilityInfo.getNumberOfMissedVehicles() + missed);
				}
			}
		
			waitingTimeCounter++;
			
		} else {
			// no person enters a bus
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		Id personId = event.getPersonId();
		
		if (event.getLegMode().toString().equals("pt")){
			personId2AgentDepartureTime.put(personId, event.getTime());
		} else {
			// not a pt Leg
		}
		
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Id personId = event.getPersonId();
		
		if (event.getLegMode().toString().equals("pt")){

			double inVehicleTime = 0.0;
			if (personId2PersonEntersVehicleTime.get(personId) == null){
				throw new RuntimeException("Person " + personId + " is arriving without having departed from an activity. Aborting...");
			} else {
				inVehicleTime = event.getTime() - personId2PersonEntersVehicleTime.get(personId);
			}
			
			if (personId2InVehicleTime.get(personId) == null) {
				personId2InVehicleTime.put(personId, inVehicleTime);
			} else {
				double inVehicleTimeSum = personId2InVehicleTime.get(personId) + inVehicleTime;
				personId2InVehicleTime.put(personId, inVehicleTimeSum);
			}			
			
		} else {
			// not a pt Leg
		}
	}

	public Map<Id, Double> getPersonId2WaitingTime() {
		return personId2WaitingTime;
	}

	public Map<Id, Double> getPersonId2InVehicleTime() {
		return personId2InVehicleTime;
	}

	public double getSumOfWaitingTimes() {
		double sumOfWaitingTimes = 0.0;
		for(Id personId : personId2WaitingTime.keySet()){
			sumOfWaitingTimes = sumOfWaitingTimes + personId2WaitingTime.get(personId);
		}
		return sumOfWaitingTimes;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().toString().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)){
			this.personId2IsEgress.put(event.getPersonId(), true);
		}
	}
	
	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		Id busId = event.getVehicleId();
		Id facilityId = event.getFacilityId();
//		System.out.println("Bus " + busId + " arrives at " + facilityId + ".");
		this.busId2currentFacilityId.put(busId, facilityId);
	}
	
	public Map<Id, Boolean> getPersonId2IsEgress() {
		return personId2IsEgress;
	}

	public int getNumberOfAgentsWaitingMoreThanHeadway() {
		return numberOfWaitingTimesMoreThanHeadway;
	}
	
	public int getNumberOfMissedVehicles() {
		return numberOfMissedVehicles;
	}

	public Map<Id, FacilityInfo> getFacilityId2facilityInfos() {
		return facilityId2facilityInfos;
	}

	
}
