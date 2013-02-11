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
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

/**
 * Calculates a fare equal to the marginal costs of causing delays when boarding / alighting a vehicle.
 * 
 * @author Ihab
 *
 */
public class MarginalCostFareHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, AgentWaitingForPtEventHandler, TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler {
	private final static Logger log = Logger.getLogger(MarginalCostFareHandler.class);

	private final EventsManager events;
	private final Map<Id, Integer> vehId2passengers = new HashMap<Id, Integer>();
	private final Map<Id, Integer> stopId2waitingPassengers = new HashMap<Id, Integer>();
	private final List<Id> ptDriverIDs = new ArrayList<Id>();
	private final List<Id> ptVehicleIDs = new ArrayList<Id>();
	private final ScenarioImpl scenario;
	private final double vtts_inVehicle;
	private final double vtts_waiting;
	private final Map<Id, Id> vehicleId2currentStopId = new HashMap<Id, Id>();
	private final Map<Id, Id> vehicleId2lineId = new HashMap<Id, Id>();
	private final Map<Id, Id> vehicleId2routeId = new HashMap<Id, Id>();

	public MarginalCostFareHandler(EventsManager events, ScenarioImpl scenario) {
		this.events = events;
		this.scenario = scenario;
		
		this.vtts_inVehicle = (this.scenario.getConfig().planCalcScore().getTravelingPt_utils_hr() - this.scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
//		this.vtts_inVehicle = this.scenario.getConfig().planCalcScore().getTravelingPt_utils_hr() / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney(); // without opportunity costs of time
		log.info("VTTS_inVehicleTime: " + vtts_inVehicle);
		this.vtts_waiting = (this.scenario.getConfig().planCalcScore().getMarginalUtlOfWaitingPt_utils_hr() - this.scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		log.info("VTTS_waiting: " + vtts_waiting);
	}
	
	@Override
	public void reset(int iteration) {
		this.vehId2passengers.clear();
		this.ptDriverIDs.clear();
		this.ptVehicleIDs.clear();
		this.stopId2waitingPassengers.clear();
		this.vehicleId2currentStopId.clear();
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
		
		this.vehicleId2routeId.put(event.getVehicleId(), event.getTransitRouteId());
		this.vehicleId2lineId.put(event.getVehicleId(), event.getTransitLineId());
		
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id personId = event.getPersonId();
		Id vehId = event.getVehicleId();
				
		if (!ptDriverIDs.contains(personId) && ptVehicleIDs.contains(vehId)){
									
			Id stopId = this.vehicleId2currentStopId.get(vehId);
//			System.out.println("Bus " + vehId + " is currently at stop " + stopId);

			// update number of passengers waiting at stops before calculating fare
			if (this.stopId2waitingPassengers.containsKey(stopId)){
				int waitingPassengers = this.stopId2waitingPassengers.get(stopId);
				this.stopId2waitingPassengers.put(stopId, waitingPassengers - 1);
			} else {
				throw new RuntimeException("Person enters vehicle without waiting for it. Aborting...");
			}
			
			double costs = calculateEnteringDelayCosts(vehId, stopId);
			AgentMoneyEvent moneyEvent = new AgentMoneyEvent(event.getTime(), event.getPersonId(), costs);
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
				
		if (!ptDriverIDs.contains(personId) && ptVehicleIDs.contains(vehId)){
						
			Id stopId = this.vehicleId2currentStopId.get(vehId);
//			System.out.println("Bus " + vehId + " is currently at stop " + stopId);
			
			// update number of passengers in vehicle before calculating fare
			if (this.vehId2passengers.containsKey(vehId)){
				int passengersInVeh = this.vehId2passengers.get(vehId);
				this.vehId2passengers.put(vehId, passengersInVeh - 1);
			} else {
				throw new RuntimeException("A person leaves a vehicle without entering it before. Aborting...");
			}
			
			double marginalCosts = calculateLeavingDelayCosts(vehId, stopId);
			AgentMoneyEvent moneyEvent = new AgentMoneyEvent(event.getTime(), event.getPersonId(), marginalCosts);
			this.events.processEvent(moneyEvent);
			
		}
	}
	
	private double calculateEnteringDelayCosts(Id vehId, Id stopId) {
		
		double transferTimePerAgent = this.scenario.getVehicles().getVehicles().get(vehId).getType().getAccessTime();
		
		double delayCostsInVeh = calculateDelaysInVeh(vehId, transferTimePerAgent) * this.vtts_inVehicle;
		double delaysCostsWaiting = calculateDelaysWaiting(vehId, stopId, transferTimePerAgent) * this.vtts_waiting;

//		System.out.println("DelayCostsInVeh: " + delayCostsInVeh);
//		System.out.println("DelayCostsWaiting: " + delaysCostsWaiting);

		double totalCosts = delayCostsInVeh + delaysCostsWaiting; // total delays

		return totalCosts;
	}

	private double calculateLeavingDelayCosts(Id vehId, Id stopId) {
		
		double transferTimePerAgent = this.scenario.getVehicles().getVehicles().get(vehId).getType().getEgressTime();
		
		double delayCostsInVeh = calculateDelaysInVeh(vehId, transferTimePerAgent) * this.vtts_inVehicle;
		double delaysCostsWaiting = calculateDelaysWaiting(vehId, stopId, transferTimePerAgent) * this.vtts_waiting;

//		System.out.println("DelayCostsInVeh: " + delayCostsInVeh);
//		System.out.println("DelayCostsWaiting: " + delaysCostsWaiting);

		double totalCosts = delayCostsInVeh + delaysCostsWaiting; // total delays

		return totalCosts;
	}
	
	private double calculateDelaysInVeh(Id vehId, double transferTime_sec) {
		double delaysInVeh_sec = 0.;
		if (this.vehId2passengers.containsKey(vehId)) {
//			System.out.println("Agents in bus " + vehId + ": " + this.vehId2passengers.get(vehId));
			delaysInVeh_sec = this.vehId2passengers.get(vehId) * transferTime_sec;
		} else {
//			System.out.println("Agents in bus " + vehId + ": 0");
		}
		return delaysInVeh_sec / 3600.;
	}

	private double calculateDelaysWaiting(Id vehId, Id stopId, double transferTime_sec) {
		
		// get all stops ahead and the current stopId
		boolean stopIsAhead = false;
		List<Id> relevantStopIDs = new ArrayList<Id>();
		TransitRoute transitRoute = this.scenario.getTransitSchedule().getTransitLines().get(this.vehicleId2lineId.get(vehId)).getRoutes().get(this.vehicleId2routeId.get(vehId));
		for (TransitRouteStop stop : transitRoute.getStops()){
			
			if (stop.getStopFacility().getId().equals(stopId)){
				stopIsAhead = true;
			}
			
			if (stopIsAhead){
				relevantStopIDs.add(stop.getStopFacility().getId());
			}
		}
//		System.out.println("Relevant transit stops (this one plus all transit stops ahead): " + relevantStopIDs);
		
		// sum up the number of waiting passengers at these stops
		int numberOfPassengersWaitingAhead = 0;
		for (Id id : relevantStopIDs){
			if (this.stopId2waitingPassengers.containsKey(id)){
//				System.out.println("StopID: " + id + " --> waiting agents: " + this.stopId2waitingPassengers.get(id));
				numberOfPassengersWaitingAhead = numberOfPassengersWaitingAhead + this.stopId2waitingPassengers.get(id);
			} else {
				// no one waiting at this stop
			}
		}
		
//		System.out.println("Agents waiting at this transit stop and all transit stops ahead: " + numberOfPassengersWaitingAhead);
		
		double delaysWaiting_sec = numberOfPassengersWaitingAhead * transferTime_sec;
		return delaysWaiting_sec / 3600.;
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
		this.vehicleId2currentStopId.put(event.getVehicleId(), event.getFacilityId());
	
		if (event.getDelay() > 1200.) {
			log.warn("Bus is more than 1200 seconds behind the schedule. More than the number of agents waiting along this transit route will be affected by delays.");
			log.warn("Can't garantee right marginal cost calculation, e.g. increase the pausenzeit...");
		}
	}

}