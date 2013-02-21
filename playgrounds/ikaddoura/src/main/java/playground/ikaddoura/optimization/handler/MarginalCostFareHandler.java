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
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
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

/**
 * Calculates a fare equal to the marginal user costs due to delays when boarding and alighting a public vehicle.
 * So far two types of delays are considered only: An agent boarding or alighting delays all passengers...
 * 1) ...who are in the pt vehicle.
 * 2) ...who are right now waiting behind that agent or at bus stops ahead up until the next pt vehicle.
 * 
 * Note: Whenever a pt vehicle stops at a transit stop due to at least one agent boarding or alighting,
 * the pt vehicle will be delayed by additional 2 sec. That is, the delay of the first transfer is equal to
 * the transfer time per agent plus exactly these 2 sec. All following passengers only cause delays according to
 * their transfer times.
 * 
 * @author Ihab
 *
 */
public class MarginalCostFareHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, AgentWaitingForPtEventHandler, TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {
	private final static Logger log = Logger.getLogger(MarginalCostFareHandler.class);

	private final EventsManager events;
	private final Map<Id, Integer> vehId2passengers = new HashMap<Id, Integer>();
	private final Map<Id, Integer> stopId2waitingPassengers = new HashMap<Id, Integer>();
	private final List<Id> ptDriverIDs = new ArrayList<Id>();
	private final List<Id> ptVehicleIDs = new ArrayList<Id>();
	private final ScenarioImpl scenario;
	private final double vtts_inVehicle;
	private final double vtts_waiting;
	private final Map<Id, Id> vehicleId2stopIdLastArrival = new HashMap<Id, Id>();
	private final Map<Id, Id> vehicleId2stopIdLastDeparture = new HashMap<Id, Id>();
	private final Map<Id, Id> vehicleId2lineId = new HashMap<Id, Id>();
	private final Map<Id, Id> vehicleId2routeId = new HashMap<Id, Id>();
	private final Map<Id, Boolean> vehId2isFirstTransfer = new HashMap<Id, Boolean>();

	public MarginalCostFareHandler(EventsManager events, ScenarioImpl scenario) {
		this.events = events;
		this.scenario = scenario;
		
		this.vtts_inVehicle = (this.scenario.getConfig().planCalcScore().getTravelingPt_utils_hr() - this.scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();		
//		this.vtts_inVehicle = this.scenario.getConfig().planCalcScore().getTravelingPt_utils_hr() / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney(); // without opportunity costs of time
	
		this.vtts_waiting = (this.scenario.getConfig().planCalcScore().getMarginalUtlOfWaitingPt_utils_hr() - this.scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();

		log.info("VTTS_inVehicleTime: " + vtts_inVehicle);
		log.info("VTTS_waiting: " + vtts_waiting);
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
				
		if (!ptDriverIDs.contains(personId) && ptVehicleIDs.contains(vehId)){
							
			Id currentStopId = this.vehicleId2stopIdLastArrival.get(vehId);
//			System.out.println("**** ENTERING ***** Bus " + vehId + " is currently at stop " + currentStopId);

			// update number of passengers waiting at stops before calculating fare
			if (this.stopId2waitingPassengers.containsKey(currentStopId)){
				int waitingPassengers = this.stopId2waitingPassengers.get(currentStopId);
				this.stopId2waitingPassengers.put(currentStopId, waitingPassengers - 1);
			} else {
				throw new RuntimeException("Person enters vehicle without waiting for it. Aborting...");
			}
			
			boolean isFirstTransfer = this.vehId2isFirstTransfer.get(vehId);
			if (isFirstTransfer){
				this.vehId2isFirstTransfer.put(vehId, false);
			}
//			System.out.println("isFirstTransfer: " + isFirstTransfer);
			double marginalCosts = calculateEnteringDelayCosts(vehId, currentStopId, isFirstTransfer);
			AgentMoneyEvent moneyEvent = new AgentMoneyEvent(event.getTime(), event.getPersonId(), marginalCosts);
			this.events.processEvent(moneyEvent);
			
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
		
		if (ptDriverIDs.contains(personId)){
			// A pt driver leaves the bus means the end of a transit route is reached and the vehicle is not in the system.
			// Therefore resetting the vehicle position.
			this.vehicleId2stopIdLastDeparture.remove(vehId);
			this.vehicleId2stopIdLastArrival.remove(vehId);
		}
				
		if (!ptDriverIDs.contains(personId) && ptVehicleIDs.contains(vehId)){
						
			Id currentStopId = this.vehicleId2stopIdLastArrival.get(vehId);
//			System.out.println("**** LEAVING ***** Bus " + vehId + " is currently at stop " + currentStopId);
			
			// update number of passengers in vehicle before calculating fare
			if (this.vehId2passengers.containsKey(vehId)){
				int passengersInVeh = this.vehId2passengers.get(vehId);
				this.vehId2passengers.put(vehId, passengersInVeh - 1);
			} else {
				throw new RuntimeException("A person leaves a vehicle without entering it before. Aborting...");
			}

			boolean isFirstTransfer = this.vehId2isFirstTransfer.get(vehId);
			if (isFirstTransfer){
				this.vehId2isFirstTransfer.put(vehId, false);
			}
			
			double marginalCosts = calculateLeavingDelayCosts(vehId, currentStopId, isFirstTransfer);
			AgentMoneyEvent moneyEvent = new AgentMoneyEvent(event.getTime(), event.getPersonId(), marginalCosts);
			this.events.processEvent(moneyEvent);
			
		}
	}
	
	private double calculateEnteringDelayCosts(Id vehId, Id stopId, boolean isFirstTransfer) {
		
		double transferTimePerAgent = this.scenario.getVehicles().getVehicles().get(vehId).getType().getAccessTime();
		
		double delayCostsInVeh = calculateDelaysInVeh(vehId, transferTimePerAgent, isFirstTransfer) * this.vtts_inVehicle;
		double delaysCostsWaiting = calculateDelaysWaiting(vehId, stopId, transferTimePerAgent, isFirstTransfer) * this.vtts_waiting;

//		System.out.println("DelayCostsInVeh: " + delayCostsInVeh);
//		System.out.println("DelayCostsWaiting: " + delaysCostsWaiting);

		double totalCosts = delayCostsInVeh + delaysCostsWaiting; // total delays

		return totalCosts;
	}

	private double calculateLeavingDelayCosts(Id vehId, Id stopId, boolean isFirstTransfer) {
		
		double transferTimePerAgent = this.scenario.getVehicles().getVehicles().get(vehId).getType().getEgressTime();
		
		double delayCostsInVeh = calculateDelaysInVeh(vehId, transferTimePerAgent, isFirstTransfer) * this.vtts_inVehicle;
		double delaysCostsWaiting = calculateDelaysWaiting(vehId, stopId, transferTimePerAgent, isFirstTransfer) * this.vtts_waiting;

//		System.out.println("DelayCostsInVeh: " + delayCostsInVeh);
//		System.out.println("DelayCostsWaiting: " + delaysCostsWaiting);

		double totalCosts = delayCostsInVeh + delaysCostsWaiting; // total delays

		return totalCosts;
	}
	
	private double calculateDelaysInVeh(Id vehId, double transferTime_sec, boolean isFirstTransfer) {
		double delaysInVeh_sec = 0.;
		
		if (this.vehId2passengers.containsKey(vehId)) {
			if (isFirstTransfer){
//				Each time a public vehicle stops at a transit stop the public vehicle is delayed by 2 seconds.
//				Assuming this time to belong to the marginal user costs of the first person entering or leaving a public vehicle.
				double extraDelay = 2.;
				delaysInVeh_sec = this.vehId2passengers.get(vehId) * (transferTime_sec + extraDelay);
			} else {
//				System.out.println("Agents in bus " + vehId + ": " + this.vehId2passengers.get(vehId));
				delaysInVeh_sec = this.vehId2passengers.get(vehId) * transferTime_sec;
			}
		} else {
//			System.out.println("Agents in bus " + vehId + ": 0");
		}
		return delaysInVeh_sec / 3600.;
	}

	private double calculateDelaysWaiting(Id vehId, Id stopId, double transferTime_sec, boolean isFirstTransfer) {
		double delaysWaiting_sec = 0.;
		
		// get all possible relevant stopIds beginning with the current stopId
		List<Id> relevantStopIDsFromHere = getRelevantStopIDsFromHere(vehId, stopId);

		// get last relevant stopId
		Id lastRelevantStopId = getLastRelevantStopId(vehId, relevantStopIDsFromHere);
		
		// get all relevant stopIds
		List<Id> relevantStopIDs = getAllRelevantStopIDs(relevantStopIDsFromHere, lastRelevantStopId);
		
//		System.out.println("All transit stops between this bus and the next bus (including the current stop): " + relevantStopIDs);
		
		// sum up the number of waiting passengers at these stops
		int numberOfPassengersWaitingInRelevantArea = 0;
		for (Id id : relevantStopIDs){
			if (this.stopId2waitingPassengers.containsKey(id)){
//				System.out.println("StopID: " + id + " --> waiting agents: " + this.stopId2waitingPassengers.get(id));
				numberOfPassengersWaitingInRelevantArea = numberOfPassengersWaitingInRelevantArea + this.stopId2waitingPassengers.get(id);
			} else {
				// no one waiting at this stop
			}
		}
		
//		System.out.println("Number of agents waiting at these stops: " + numberOfPassengersWaitingInRelevantArea);
		
		if (isFirstTransfer){
//			Each time a public vehicle stops at a transit stop the public vehicle is delayed by 2 seconds.
//			Assuming this time to belong to the marginal user costs of the first person entering or leaving a public vehicle.
			double extraDelay = 2.;
			delaysWaiting_sec = numberOfPassengersWaitingInRelevantArea * (transferTime_sec + extraDelay);
		} else {
//			System.out.println("Agents in bus " + vehId + ": " + this.vehId2passengers.get(vehId));
			delaysWaiting_sec = numberOfPassengersWaitingInRelevantArea * transferTime_sec;
		}
		
		return delaysWaiting_sec / 3600.;
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
//				System.out.println("Checking if vehicle " + vehicleId + " is the next vehicle ahead...");
				Id transitStopId = vehicleId2stopIdLastDeparture.get(vehicleId);
				
				for (Id id : relevantStopIDsFromHere){
					if (id.equals(transitStopId)){
//						System.out.println("This vehicle is at a stop along the route. (StopId: " + id + ") Proceeding...");
						int stopIndex = relevantStopIDsFromHere.indexOf(id);
//						System.out.println("StopId index in list: " + stopIndex);
						if (stopIndex < lowestIndex){							
							lowestIndex = stopIndex;
							lastRelevantStopId = id;
						}
					} else {
//						System.out.println("StopId " + id + " is behind the currently delayed bus and therefore not relevant.");
					}
				}
			}
		}
		if (lastRelevantStopId == null){
			// That means there is no bus between the currently delayed one and the end of the route, thus all stops are relevant.
			lastRelevantStopId = relevantStopIDsFromHere.get(relevantStopIDsFromHere.size() - 1); // last stop
		}
//		System.out.println("Last Relevant StopID: " + lastRelevantStopId);
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
	
		if (event.getDelay() > 1200.) {
			log.warn("Bus is more than 1200 seconds behind the schedule. More than the number of agents waiting along this transit route will be affected by delays.");
			log.warn("Can't garantee right marginal cost calculation.");
		}
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		this.vehicleId2stopIdLastDeparture.put(event.getVehicleId(), event.getFacilityId());
		
		if (event.getDelay() > 1200.) {
			log.warn("Bus is more than 1200 seconds behind the schedule. More than the number of agents waiting along this transit route will be affected by delays.");
			log.warn("Can't garantee right marginal cost calculation.");
		}
	}

}