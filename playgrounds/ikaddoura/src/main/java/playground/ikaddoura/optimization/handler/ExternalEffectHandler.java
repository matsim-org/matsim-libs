/* *********************************************************************** *
 * project: org.matsim.*
 * MoneyThrowEventHandler.java
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

/**
 * 
 */
package playground.ikaddoura.optimization.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import playground.ikaddoura.optimization.events.ExternalEffectInVehicleTimeEvent;
import playground.ikaddoura.optimization.events.ExternalEffectWaitingTimeEvent;

/**
 * Calculates the external effects caused by delays when boarding and alighting a public vehicle.
 * So far two types of delays are considered: An agent boarding or alighting delays all passengers...
 * ...who are in the pt vehicle.
 * ...who are right now waiting behind that agent or at bus stops ahead the transit route up until the next pt vehicle.
 * 
 * External effects to be considered in future:
 * TODO: Consider capacity constraints.
 * 
 * Assumptions for the current version:
 * 1) The scheduled dwell time at transit stops is 0sec. TODO: Get dwell time from schedule and account for dwell times >0sec.
 * 2) The door operation mode of public vehicles is serial. TODO: Adjust for parallel door operation mode.
 * 3) Public vehicles start with no delay. The slack time at the end of a transit route has to be larger than the max. delay of public vehicles.
 * 4) Transit stops belong to single transit routes. Transit routes do not intersect or overlay.
 * 5) Agents board the first arriving public vehicle. The vehicle capacity has to be larger than the max. number of passengers.
 * 
 * Note: Whenever a pt vehicle stops at a transit stop due to at least one agent boarding or alighting,
 * the pt vehicle will be delayed by additional 2 sec. That is, the delay of the first transfer is equal to
 * the transfer time per agent plus exactly these 2 sec. All following passengers only cause delays according to
 * their transfer times.
 * 
 * @author Ihab
 *
 */
public class ExternalEffectHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, AgentWaitingForPtEventHandler, TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {
	private final static Logger log = Logger.getLogger(ExternalEffectHandler.class);

	private final EventsManager events;
	private final Map<Id, Integer> vehId2passengers = new HashMap<Id, Integer>();
	private final Map<Id, Integer> stopId2waitingPassengers = new HashMap<Id, Integer>();
	private final List<Id> ptDriverIDs = new ArrayList<Id>();
	private final List<Id> ptVehicleIDs = new ArrayList<Id>();
	private final ScenarioImpl scenario;
	private final Map<Id, Id> vehicleId2stopIdLastArrival = new HashMap<Id, Id>();
	private final Map<Id, Id> vehicleId2stopIdLastDeparture = new HashMap<Id, Id>();
	private final Map<Id, Id> vehicleId2lineId = new HashMap<Id, Id>();
	private final Map<Id, Id> vehicleId2routeId = new HashMap<Id, Id>();
	private final Map<Id, Boolean> vehId2isFirstTransfer = new HashMap<Id, Boolean>();

	public ExternalEffectHandler(EventsManager events, ScenarioImpl scenario) {
		this.events = events;
		this.scenario = scenario;
	}
	
	@Override
	public void reset(int iteration) {
		this.vehId2passengers.clear();
		this.ptDriverIDs.clear();
		this.ptVehicleIDs.clear();
		this.stopId2waitingPassengers.clear();
		this.vehicleId2stopIdLastArrival.clear();
		this.vehicleId2routeId.clear();
		this.vehicleId2lineId.clear();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
				
		if (!this.ptDriverIDs.contains(event.getDriverId())){
			this.ptDriverIDs.add(event.getDriverId());
		}
		
		if (!this.ptVehicleIDs.contains(event.getVehicleId())){
			this.ptVehicleIDs.add(event.getVehicleId());
		}
		
		// reset the positions at the beginning of each cycle
		this.vehicleId2stopIdLastArrival.remove(event.getVehicleId());
		this.vehicleId2stopIdLastDeparture.remove(event.getVehicleId());
		
		this.vehicleId2routeId.put(event.getVehicleId(), event.getTransitRouteId());
		this.vehicleId2lineId.put(event.getVehicleId(), event.getTransitLineId());
		
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id personId = event.getPersonId();
		Id vehId = event.getVehicleId();
		double time = event.getTime();
				
		if (!ptDriverIDs.contains(personId) && ptVehicleIDs.contains(vehId)){
							
			Id currentStopId = this.vehicleId2stopIdLastArrival.get(vehId);
			
			// update number of passengers waiting at stops before calculating fare
			if (this.stopId2waitingPassengers.containsKey(currentStopId)){
				int waitingPassengers = this.stopId2waitingPassengers.get(currentStopId);
				this.stopId2waitingPassengers.put(currentStopId, waitingPassengers - 1);
			} else {
				throw new RuntimeException("Person enters vehicle without waiting for it. Aborting...");
			}
			
			calculateExternalEffects(vehId, personId, this.scenario.getVehicles().getVehicles().get(vehId).getType().getAccessTime(), time);
			
			// update number of passengers in vehicle after calculating fare
			if (this.vehId2passengers.containsKey(vehId)){
				int passengersInVeh = this.vehId2passengers.get(vehId);
				this.vehId2passengers.put(vehId, passengersInVeh + 1);
			} else {
				this.vehId2passengers.put(vehId, 1);
			}
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		Id personId = event.getPersonId();
		Id vehId = event.getVehicleId();
		double time = event.getTime();
		
		if (ptDriverIDs.contains(personId)){
			
			// A pt driver leaves the bus means the end of a transit route is reached and the vehicle is not in the system.
			// Therefore resetting the vehicle position.
			this.vehicleId2stopIdLastDeparture.remove(vehId);
			this.vehicleId2stopIdLastArrival.remove(vehId);
		
		} else if (!ptDriverIDs.contains(personId) && ptVehicleIDs.contains(vehId)){							
			
			// update number of passengers in vehicle before calculating fare
			int passengersInVeh = this.vehId2passengers.get(vehId);
			this.vehId2passengers.put(vehId, passengersInVeh - 1);
			calculateExternalEffects(vehId, personId, this.scenario.getVehicles().getVehicles().get(vehId).getType().getEgressTime(), time);
		}
	}

	@Override
	public void handleEvent(AgentWaitingForPtEvent event) {
		if (this.stopId2waitingPassengers.containsKey(event.getWaitingAtStopId())){
			int waitingPassengers = this.stopId2waitingPassengers.get(event.getWaitingAtStopId());
			this.stopId2waitingPassengers.put(event.getWaitingAtStopId(), waitingPassengers + 1);
		} else {
			this.stopId2waitingPassengers.put(event.getWaitingAtStopId(), 1);
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.vehicleId2stopIdLastArrival.put(event.getVehicleId(), event.getFacilityId());
		// Vehicle has arrived at transit stop. The following agent will be the first transfer of this vehicle.
		this.vehId2isFirstTransfer.put(event.getVehicleId(), true);
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		this.vehicleId2stopIdLastDeparture.put(event.getVehicleId(), event.getFacilityId());
	}
	
	// #########################################################################################################
	// #########################################################################################################

	private void calculateExternalEffects(Id vehId, Id personId, double transferTime, double time) {
		
		Id currentStopId = this.vehicleId2stopIdLastArrival.get(vehId);

		boolean isFirstTransfer = this.vehId2isFirstTransfer.get(vehId);
		if (isFirstTransfer){
			this.vehId2isFirstTransfer.put(vehId, false);
		}
		
		//	Calculate in-vehicle delay effect
		int delayedPassengers_inVeh = calcDelayedPassengersInVeh(vehId);
		double delay_inVeh = calcDelayInVeh(delayedPassengers_inVeh, transferTime, isFirstTransfer);
		
		//	Calculate waiting delay effect
		int delayedPassengers_waiting = calcDelayedPassengersWaiting(vehId, currentStopId);
		double delay_waiting = calcDelayWaiting(delayedPassengers_waiting, transferTime, isFirstTransfer);
					
		ExternalEffectWaitingTimeEvent delayWaitingEvent = new ExternalEffectWaitingTimeEvent(personId, vehId, time, delayedPassengers_waiting, delay_waiting, isFirstTransfer);
		this.events.processEvent(delayWaitingEvent);
		ExternalEffectInVehicleTimeEvent delayInVehicleEvent = new ExternalEffectInVehicleTimeEvent(personId, vehId, time, delayedPassengers_inVeh, delay_inVeh, isFirstTransfer);
		this.events.processEvent(delayInVehicleEvent);			
	}
	
	private int calcDelayedPassengersInVeh(Id vehId) {
		int delayedPassengersInVeh = 0;
		if (this.vehId2passengers.containsKey(vehId)) {
			delayedPassengersInVeh = this.vehId2passengers.get(vehId);
		}
		return delayedPassengersInVeh;
	}
	
	private double calcDelayInVeh(int delayedPassengers_inVeh, double transferTime_sec, boolean isFirstTransfer) {
		
		double delaysInVeh_sec = 0.;
		
		if (isFirstTransfer){
			//	Each time a public vehicle stops at a transit stop the public vehicle is delayed by 2 seconds.
			//	Assuming this time to belong to the marginal user costs of the first person entering or leaving a public vehicle.
			double extraDelay = 2.;
			delaysInVeh_sec = delayedPassengers_inVeh * (transferTime_sec + extraDelay);
		
		} else {
			delaysInVeh_sec = delayedPassengers_inVeh * transferTime_sec;
		}
		
		return delaysInVeh_sec;
	}
	
	private int calcDelayedPassengersWaiting(Id vehId, Id currentStopId) {
		
		// get all possible relevant stopIds beginning with the current stopId
		List<Id> relevantStopIDsFromHere = getRelevantStopIDsFromHere(vehId, currentStopId);
		// get last relevant stopId
		Id lastRelevantStopId = getLastRelevantStopId(vehId, relevantStopIDsFromHere);
		// get all relevant stopIds between this bus and the next bus (including the current stop)
		List<Id> relevantStopIDs = getAllRelevantStopIDs(relevantStopIDsFromHere, lastRelevantStopId);
				
		// sum up the number of waiting passengers at these stops
		int numberOfPassengersWaitingInRelevantArea = 0;
		for (Id id : relevantStopIDs){
			if (this.stopId2waitingPassengers.containsKey(id)){
				numberOfPassengersWaitingInRelevantArea = numberOfPassengersWaitingInRelevantArea + this.stopId2waitingPassengers.get(id);
			}
		}
		return numberOfPassengersWaitingInRelevantArea;
	}
	
	private double calcDelayWaiting(int delayedPassengers_waiting, double transferTime_sec, boolean isFirstTransfer) {
		double delaysWaiting_sec = 0.;
		
		if (isFirstTransfer){
			//	Each time a public vehicle stops at a transit stop the public vehicle is delayed by 2 seconds.
			//	Assuming this time to belong to the marginal user costs of the first person entering or leaving a public vehicle.
			double extraDelay = 2.;
			delaysWaiting_sec = delayedPassengers_waiting * (transferTime_sec + extraDelay);
		} else {
			delaysWaiting_sec = delayedPassengers_waiting * transferTime_sec;
		}
		
		return delaysWaiting_sec;		
	}

	private List<Id> getAllRelevantStopIDs(List<Id> relevantStopIDsFromHere, Id lastRelevantStopId) {
		List<Id> relevantStopIDs = new ArrayList<Id>();

		boolean isRelevant = true;
		for (Id id : relevantStopIDsFromHere){
			if (isRelevant){
				relevantStopIDs.add(id);
			}
			if (id.equals(lastRelevantStopId)){
				isRelevant = false;
			}
		}
		return relevantStopIDs;
	}

	private Id getLastRelevantStopId(Id vehId, List<Id> relevantStopIDsFromHere) {
		
		Id lastRelevantStopId = null;
		int lowestIndex = Integer.MAX_VALUE;

		// get last departure stop Id for each vehicle
		for (Id vehicleId : this.vehicleId2stopIdLastDeparture.keySet()){
			if (!vehicleId.equals(vehId)){
				// not the vehicle that is currently delayed
				Id transitStopId = vehicleId2stopIdLastDeparture.get(vehicleId);
				
				for (Id id : relevantStopIDsFromHere){
					if (id.equals(transitStopId)){
						int stopIndex = relevantStopIDsFromHere.indexOf(id);
						if (stopIndex < lowestIndex){							
							lowestIndex = stopIndex;
							lastRelevantStopId = id;
						}
					} else {
					}
				}
			}
		}
		if (lastRelevantStopId == null){
			lastRelevantStopId = relevantStopIDsFromHere.get(relevantStopIDsFromHere.size() - 1); // last stop
		}
		return lastRelevantStopId;
	}

	private List<Id> getRelevantStopIDsFromHere(Id vehId, Id stopId) {
		boolean stopIsAhead = false;
		List<Id> relevantStopIDsFromHere = new ArrayList<Id>();
		TransitRoute transitRoute = this.scenario.getTransitSchedule().getTransitLines().get(this.vehicleId2lineId.get(vehId)).getRoutes().get(this.vehicleId2routeId.get(vehId));
		for (TransitRouteStop stop : transitRoute.getStops()){
			
			if (stop.getStopFacility().getId().equals(stopId)){
				stopIsAhead = true;
			}
			
			if (stopIsAhead){
				relevantStopIDsFromHere.add(stop.getStopFacility().getId());
			}
		}
		return relevantStopIDsFromHere;
	}

}