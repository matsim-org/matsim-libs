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
package playground.ikaddoura.internalizationPt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.vehicles.Vehicle;


/**
 * Throws WatingDelayEvents to indicate that an agent entering or leaving a public vehicle delayed passengers waiting for that public vehicle.
 * 
 * External effects to be considered in future:
 * TODO: Capacity constraints.
 * 
 * IMPORTANT Assumptions:
 * 1) The scheduled dwell time at transit stops is 0sec. TODO: Get dwell time from schedule and account for dwell times >0sec.
 * 2) The door operation mode of public vehicles is serial. TODO: Adjust for parallel door operation mode.
 * 3) Public vehicles start with no delay. The slack time at the end of a transit route has to be larger than the max. delay of public vehicles.
 * 4) Agents board the first arriving public vehicle. The vehicle capacity has to be larger than the max. number of passengers.
 * 
 * Note: Whenever a pt vehicle stops at a transit stop due to at least one agent boarding or alighting,
 * the pt vehicle will be delayed by additional 2 sec, e.g. 1 sec before and 1 sec after agents are entering and leaving.
 * These seconds can be interpret as "doors opening" and "doors closing" time.
 * It is assumed that these extra delay are caused by ALL agents entering and leaving the vehicle at the current transit stop.
 * The delay per person is calculated by dividing the sum of "door opening" and "door closing" time by the total number of transfering agents.
 * Another possibility (NOT implemented): Let the first transfering agent pay for these extra delay. But this may cause weird competition
 * not to be the first boarding or alighting agent... 
 * 
 * @author Ihab
 *
 */
public class TransferDelayWaitingHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, AgentWaitingForPtEventHandler {
//	private final static Logger log = Logger.getLogger(TransferDelayWaitingHandler.class);

	// extra delay for a bus before and after agents are entering or leaving a public vehicle
	// currently there is an extra delay of 1 sec before and 1 sec after agents are entering or leaving
	private final double doorOpeningTime = 1.0;
	private final double doorClosingTime = 1.0;
	private final Map<Id<Vehicle>, List<Id<Person>>> vehId2agentsTransferingAtThisStop = new HashMap<>();
	
	private final MutableScenario scenario;
	private final EventsManager events;
	private final List<Id<Person>> ptDriverIDs = new ArrayList<>();
	private final List<Id<Vehicle>> ptVehicleIDs = new ArrayList<>();
		
	private final List<TransferDelayWaiting> boardingDelayEffects = new ArrayList<TransferDelayWaiting>();
	private final List<TransferDelayWaiting> alightingDelayEffects = new ArrayList<TransferDelayWaiting>();
	private final List<TransferDelayWaiting> extraDelayEffects = new ArrayList<TransferDelayWaiting>();
	
	private final Map<Id<Person>, Double> personId2startWaitingForPt = new HashMap<>();
	private final Map<Id<Vehicle>, Double> vehicleId2delay = new HashMap<>();
	private final Map<Id<Vehicle>, Boolean> vehicleId2isFirstTransfer = new HashMap<>();

	public TransferDelayWaitingHandler(EventsManager events, MutableScenario scenario) {
		this.events = events;
		this.scenario = scenario;
	}
	
	@Override
	public void reset(int iteration) {
		this.ptDriverIDs.clear();
		this.ptVehicleIDs.clear();
		this.boardingDelayEffects.clear();
		this.alightingDelayEffects.clear();
		this.extraDelayEffects.clear();
		this.vehId2agentsTransferingAtThisStop.clear();
		this.personId2startWaitingForPt.clear();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
				
		if (!this.ptDriverIDs.contains(event.getDriverId())){
			this.ptDriverIDs.add(event.getDriverId());
		}
		
		if (!this.ptVehicleIDs.contains(event.getVehicleId())){
			this.ptVehicleIDs.add(event.getVehicleId());
		}
		
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
				
		if (!ptDriverIDs.contains(event.getPersonId()) && ptVehicleIDs.contains(event.getVehicleId())){
			
			for (TransferDelayWaiting delay : this.boardingDelayEffects){
				
				if (delay.getPersonId().toString().equals(event.getPersonId().toString()) && !delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					// Agent is already being tracked. Starting parallel personTracking for different vehicles.
					// That means the bus which was previously delayed by that agent has not yet arrived the end of the transit route.
					// Must have been a very short activity...
				
				} else if (delay.getPersonId().toString().equals(event.getPersonId().toString()) && delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					throw new RuntimeException("Person and public vehicle are already being tracked. That means, an agent " +
							"enters a vehicle which was previously delayed by himself. Depending on the scenario this can happen if there are circle transit routes. " +
							"Handling of this specific occurence is not implemented. ");
				}
			}
			
			
//			double waitingTime = event.getTime() - this.personId2startWaitingForPt.get(event.getDriverId()) - 1.0; // TODO!
			
			double waitingTime = event.getTime() - this.personId2startWaitingForPt.get(event.getPersonId());
			double vehicleDelay = this.vehicleId2delay.get(event.getVehicleId());
			
//			System.out.println("++++ AgentId: " + event.getDriverId());
//			System.out.println("WaitingTime: " + waitingTime);
//			System.out.println("VehicleDelay: " + vehicleDelay);
			
			double affectedAgentUnits = 0;
			if (vehicleDelay <= waitingTime){
				// standard case
				affectedAgentUnits = 1.0;
				
			} else {
				// waitingTime below vehicleDelay
				// calculate the affectedAgentUnits
				affectedAgentUnits = waitingTime / vehicleDelay;
				
//				System.out.println("++++ AgentId: " + event.getDriverId());
//				System.out.println("WaitingTime: " + waitingTime);
//				System.out.println("VehicleDelay: " + vehicleDelay);
//				System.out.println("affectedAgentUnits: " + affectedAgentUnits);

			}
			
			// the person entering the busX right now was delayed by other agents boarding or alighting before.
			// Therefore, go through all agents that are currently being tracked, check who was delaying that busX before and update the number of affected agent units.
			for (TransferDelayWaiting delay : this.boardingDelayEffects){
				if (delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					double affectedAgentUnitsSoFar = delay.getAffectedAgents();
					delay.setAffectedAgents(affectedAgentUnitsSoFar + affectedAgentUnits);
				}
			}
			for (TransferDelayWaiting delay : this.alightingDelayEffects){
				if (delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					double affectedAgentUnitsSoFar = delay.getAffectedAgents();
					delay.setAffectedAgents(affectedAgentUnitsSoFar + affectedAgentUnits);
				}
			}
			for (TransferDelayWaiting delay : this.extraDelayEffects){
				if (delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					double affectedAgentUnitsSoFar = delay.getAffectedAgents();
					delay.setAffectedAgents(affectedAgentUnitsSoFar + affectedAgentUnits);
				}
			}
						
			// remember who was boarding and alighting at this stop.
			List<Id<Person>> agentsTransferingAtThisStop;
			if (this.vehId2agentsTransferingAtThisStop.get(event.getVehicleId()) == null){
				agentsTransferingAtThisStop = new ArrayList<>();
			} else {
				// TODO: AddAll
				agentsTransferingAtThisStop = this.vehId2agentsTransferingAtThisStop.get(event.getVehicleId());
			}
			agentsTransferingAtThisStop.add(event.getPersonId());
			this.vehId2agentsTransferingAtThisStop.put(event.getVehicleId(), agentsTransferingAtThisStop);

			// start tracking the delay effect induced by that person entering the public vehicle
			double transferTime = this.scenario.getTransitVehicles().getVehicles().get(event.getVehicleId()).getType().getAccessTime();
			TransferDelayWaiting delayEffect = startTrackingDelayEffect(event.getVehicleId(), event.getPersonId(), transferTime);
			this.boardingDelayEffects.add(delayEffect);
			
			// update the vehicle delay
			double delay = this.vehicleId2delay.get(event.getVehicleId()) + this.scenario.getTransitVehicles().getVehicles().get(event.getVehicleId()).getType().getAccessTime();
			if (this.vehicleId2isFirstTransfer.get(event.getVehicleId())) {
				delay = delay + this.doorOpeningTime;
				this.vehicleId2isFirstTransfer.put(event.getVehicleId(), false);
			}
			this.vehicleId2delay.put(event.getVehicleId(), delay);
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		
		if (ptDriverIDs.contains(event.getPersonId())){
			// the transit vehicle driver leaves the vehicle at the end of the transit route
			
			// throw waitingDelayEvents induced by agents entering a public vehicle and stop tracking delay effect of that person and public vehicle
			for (Iterator<TransferDelayWaiting> iterator = this.boardingDelayEffects.iterator(); iterator.hasNext();){
				TransferDelayWaiting delay = iterator.next();
				
				if (delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					TransferDelayWaitingEvent delayWaitingEvent = new TransferDelayWaitingEvent(delay.getPersonId(), delay.getAffectedVehicle(), event.getTime(), delay.getAffectedAgents(), delay.getTransferDelay());
					this.events.processEvent(delayWaitingEvent);
					iterator.remove();
				}
			}
			
			// throw waitingDelayEvents induced by agents leaving a public vehicle and stop tracking delay effect of that person and public vehicle
			for (Iterator<TransferDelayWaiting> iterator = this.alightingDelayEffects.iterator(); iterator.hasNext();){
				TransferDelayWaiting delay = iterator.next();
				
				if (delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					TransferDelayWaitingEvent delayWaitingEvent = new TransferDelayWaitingEvent(delay.getPersonId(), delay.getAffectedVehicle(), event.getTime(), delay.getAffectedAgents(), delay.getTransferDelay());
					this.events.processEvent(delayWaitingEvent);
					iterator.remove();
				}
			}
			
			// throw waitingDelayEvents induced by agents boardings or alighting a public vehicle and stop tracking delay effect of that person and public vehicle
			for (Iterator<TransferDelayWaiting> iterator = this.extraDelayEffects.iterator(); iterator.hasNext();){
				TransferDelayWaiting delay = iterator.next();
				
				if (delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					TransferDelayWaitingEvent delayWaitingEvent = new TransferDelayWaitingEvent(delay.getPersonId(), delay.getAffectedVehicle(), event.getTime(), delay.getAffectedAgents(), delay.getTransferDelay());
					this.events.processEvent(delayWaitingEvent);
					iterator.remove();
				}
			}

						
		} else if (!ptDriverIDs.contains(event.getPersonId()) && ptVehicleIDs.contains(event.getVehicleId())){
			
			for (TransferDelayWaiting delay : this.alightingDelayEffects){
				if (delay.getPersonId().toString().equals(event.getPersonId().toString()) && !delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					// log.info("Agent is already being tracked. Starting parallel personTracking for different vehicles. " +
							// "That means the bus which was previously delayed by that agent has not yet arrived the end of the transit route. " +
							// "Must have been a very short activity... ");
				
				} else if (delay.getPersonId().toString().equals(event.getPersonId().toString()) && delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					throw new RuntimeException("Person and public vehicle are already being tracked. That means, an agent " +
							"leaves a vehicle which was previously delayed by himself. Depending on the scenario this can happen if there are circle transit routes. " +
							"Handling of this specific occurence is not implemented. ");
				}
			}
						
			// remember who was boarding and alighting at this stop.
			List<Id<Person>> agentsTransferingAtThisStop;
			if (this.vehId2agentsTransferingAtThisStop.get(event.getVehicleId()) == null){
				agentsTransferingAtThisStop = new ArrayList<>();
			} else {
				// TODO: AddAll
				agentsTransferingAtThisStop = this.vehId2agentsTransferingAtThisStop.get(event.getVehicleId());
			}
			agentsTransferingAtThisStop.add(event.getPersonId());
			this.vehId2agentsTransferingAtThisStop.put(event.getVehicleId(), agentsTransferingAtThisStop);
			
			// start tracking the delay effect induced by that person leaving the public vehicle
			double transferTime = this.scenario.getTransitVehicles().getVehicles().get(event.getVehicleId()).getType().getEgressTime();
			TransferDelayWaiting delayEffect = startTrackingDelayEffect(event.getVehicleId(), event.getPersonId(), transferTime);
			this.alightingDelayEffects.add(delayEffect);
			
			// update the vehicle delay
			double delay = this.vehicleId2delay.get(event.getVehicleId()) + this.scenario.getTransitVehicles().getVehicles().get(event.getVehicleId()).getType().getEgressTime();
			if (this.vehicleId2isFirstTransfer.get(event.getVehicleId())) {
				delay = delay + this.doorOpeningTime;
				this.vehicleId2isFirstTransfer.put(event.getVehicleId(), false);
			}
			this.vehicleId2delay.put(event.getVehicleId(), delay);
		}
	}
	
	private TransferDelayWaiting startTrackingDelayEffect(Id<Vehicle> vehicleId, Id<Person> personId, double delay) {
		
		TransferDelayWaiting delayEffect = new TransferDelayWaiting();
		delayEffect.setPersonId(personId);
		delayEffect.setAffectedVehicle(vehicleId);
		delayEffect.setDelay(delay);
		delayEffect.setAffectedAgents(0);
		
		return delayEffect;
		
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.vehId2agentsTransferingAtThisStop.remove(event.getVehicleId());
		this.vehicleId2delay.put(event.getVehicleId(), event.getDelay());
		this.vehicleId2isFirstTransfer.put(event.getVehicleId(), true);
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		
		// start tracking the extra delays for "opening" and "closing" the doors
		if (!(this.vehId2agentsTransferingAtThisStop.get(event.getVehicleId()) == null)){

			List<Id<Person>> agentsTransferingAtThisStop = this.vehId2agentsTransferingAtThisStop.get(event.getVehicleId());
			if (!(agentsTransferingAtThisStop.size() == 0)){
				double delayPerPerson = (this.doorClosingTime + this.doorOpeningTime) / agentsTransferingAtThisStop.size();
				
				for (Id<Person> personId : agentsTransferingAtThisStop){
					// start tracking for each person
					TransferDelayWaiting delayEffect = startTrackingDelayEffect(event.getVehicleId(), personId, delayPerPerson);
					this.extraDelayEffects.add(delayEffect);	
				}
			}
		}	
	}

	@Override
	public void handleEvent(AgentWaitingForPtEvent event) {
		this.personId2startWaitingForPt.put(event.getPersonId(), event.getTime());
	}

}