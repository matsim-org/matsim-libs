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
package playground.ikaddoura.optimization.users;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.BoardingDeniedEvent;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.BoardingDeniedEventHandler;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;

/**
 * @author Ihab
 *
 */
public class WaitingTimeHandler implements PersonEntersVehicleEventHandler, AgentDepartureEventHandler, AgentArrivalEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, BoardingDeniedEventHandler {
	private final static Logger log = Logger.getLogger(WaitingTimeHandler.class);

	private final List <Double> waitingTimes = new ArrayList<Double>();
	private final List <Double> waitingTimesMissed = new ArrayList<Double>();
	private final List <Double> waitingTimesNotMissed = new ArrayList<Double>();
	private Map <Id, List<Double>> personId2waitingTimes = new HashMap<Id, List<Double>>();

	private final Map <Id, Double> personId2PersonEntersVehicleTime = new HashMap<Id, Double>();
	private final Map <Id, Double> personId2AgentDepartureTime = new HashMap<Id, Double>();
	private final Map <Id, Double> personId2InVehicleTime = new HashMap<Id, Double>();
	private final Map <Id, Id> busId2currentFacilityId = new HashMap<Id, Id>();

	private int numberOfMissedVehicles;
	private int boardingDeniedEvents;
		
	private final double headway;
	
	private double maxArriveDelay = 0.;
	private double maxDepartDelay = 0.;
	
	public double getMaxArriveDelay() {
		return maxArriveDelay;
	}

	public double getMaxDepartDelay() {
		return maxDepartDelay;
	}

	public WaitingTimeHandler(double headway) {
		this.headway = headway;
	}

	@Override
	public void reset(int iteration) {
		waitingTimes.clear();
		waitingTimesMissed.clear();
		waitingTimesNotMissed.clear();
		personId2waitingTimes.clear();
		personId2PersonEntersVehicleTime.clear();
		personId2AgentDepartureTime.clear();
		personId2InVehicleTime.clear();
		busId2currentFacilityId.clear();
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
			
			waitingTimes.add(waitingTime);
			
			// save waitingTimes per person
			if (personId2waitingTimes.get(personId) == null){
				List<Double> waitingTimes = new ArrayList<Double>();
				waitingTimes.add(waitingTime);
				personId2waitingTimes.put(personId, waitingTimes);
			} else {
				List<Double> waitingTimes = personId2waitingTimes.get(personId);
				waitingTimes.add(waitingTime);
				personId2waitingTimes.put(personId, waitingTimes);
			}
						

			// analyze waitingTime and save waitingTime per person differentiated for agents who missed and who didn't miss a bus
			if (waitingTime > this.headway){
				
				waitingTimesMissed.add(waitingTime);
							
				int missed = (int) (waitingTime / this.headway);
//				System.out.println("Missed Busses: " + missed);
				this.numberOfMissedVehicles = this.numberOfMissedVehicles + missed;
			}
			
			if (waitingTime <= this.headway){
				waitingTimesNotMissed.add(waitingTime);
			}
					
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

	public List <Double> getWaitingTimes() {
		return waitingTimes;
	}

	public Map<Id, Double> getPersonId2InVehicleTime() {
		return personId2InVehicleTime;
	}
	
	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		Id busId = event.getVehicleId();
		Id facilityId = event.getFacilityId();
		
		if (event.getDelay()!=0){
			if(event.getDelay()>maxArriveDelay){
				maxArriveDelay = event.getDelay();
			}
		}		
		this.busId2currentFacilityId.put(busId, facilityId);
	}
	
	public int getNumberOfMissedVehicles() {
		return numberOfMissedVehicles;
	}

	public List <Double> getWaitingTimesMissed() {
		return waitingTimesMissed;
	}

	public List <Double> getWaitingTimesNotMissed() {
		return waitingTimesNotMissed;
	}

	public Map <Id, List<Double>> getPersonId2waitingTimes() {
		return personId2waitingTimes;
	}

	
	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {

		if (event.getDelay()!=0){
			if(event.getDelay()>maxDepartDelay){
				maxDepartDelay = event.getDelay();
			}
		}
	}

	@Override
	public void handleEvent(BoardingDeniedEvent event) {
		this.boardingDeniedEvents++;
	}

	public int getBoardingDeniedEvents() {
		return boardingDeniedEvents;
	}

}
