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
package playground.ikaddoura.optimization.externalDelayEffects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.scenario.ScenarioImpl;


/**
 * Throws WatingDelayEvents to indicate that an agent entering or leaving a public vehicle delayed passengers waiting for that public vehicle.
 * 
 * External effects to be considered in future:
 * TODO: Capacity constraints.
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
public class WaitingDelayHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler {
	private final static Logger log = Logger.getLogger(WaitingDelayHandler.class);

	// extra delay for a bus to arrive and depart at a transit stop if there is at least one transfer
	// (before and after agents are leaving and/or entering a public vehicle)
	private final double extraDelay = 1.0;
	
	private final ScenarioImpl scenario;
	private final EventsManager events;
	private final List<Id> ptDriverIDs = new ArrayList<Id>();
	private final List<Id> ptVehicleIDs = new ArrayList<Id>();
		
	private final Map<Id, Boolean> vehId2isFirstTransfer = new HashMap<Id, Boolean>();
	private final List<ExtEffectWaitingDelay> boardingDelayEffects = new ArrayList<ExtEffectWaitingDelay>();
	private final List<ExtEffectWaitingDelay> alightingDelayEffects = new ArrayList<ExtEffectWaitingDelay>();

	public WaitingDelayHandler(EventsManager events, ScenarioImpl scenario) {
		this.events = events;
		this.scenario = scenario;
	}
	
	@Override
	public void reset(int iteration) {
		this.ptDriverIDs.clear();
		this.ptVehicleIDs.clear();
		this.vehId2isFirstTransfer.clear();
		this.boardingDelayEffects.clear();
		this.alightingDelayEffects.clear();
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
			
			for (ExtEffectWaitingDelay delay : this.boardingDelayEffects){
				
				if (delay.getPersonId().toString().equals(event.getPersonId().toString()) && !delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					log.warn("Agent is already being tracked. Starting parallel personTracking for different vehicles. " +
							"That means the bus which was previously delayed by that agent has not yet arrived the end of the transit route. " +
							"Must have been a very short activity... ");
				
				} else if (delay.getPersonId().toString().equals(event.getPersonId().toString()) && delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					throw new RuntimeException("Person and public vehicle are already being tracked. That means, an agent " +
							"enters a vehicle which was previously delayed by himself. Depending on the scenario this can happen if there are circle transit routes. " +
							"Handling of this specific occurence is not implemented. ");
				}
			}
			
			// update the number of affected agents
			for (ExtEffectWaitingDelay delay : this.boardingDelayEffects){
				if (delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					int affectedAgents = delay.getAffectedAgents();
					delay.setAffectedAgents(affectedAgents + 1);
				}
			}
			for (ExtEffectWaitingDelay delay : this.alightingDelayEffects){
				if (delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					int affectedAgents = delay.getAffectedAgents();
					delay.setAffectedAgents(affectedAgents + 1);
				}
			}

			// start tracking the delay effect induced by that person entering the public vehicle
			double transferTime = this.scenario.getVehicles().getVehicles().get(event.getVehicleId()).getType().getAccessTime();
			// assuming the first agent to cause an extra delay before bus arrives
			ExtEffectWaitingDelay delayEffect = startTrackingDelayEffect(event.getVehicleId(), event.getPersonId(), transferTime, this.extraDelay);
			this.boardingDelayEffects.add(delayEffect);
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		
		if (ptDriverIDs.contains(event.getPersonId())){
			// the transit vehicle driver leaves the vehicle at the end of the transit route
			
			// throw waitingDelayEvents induced by agents entering a public vehicle and stop tracking delay effect of that person and public vehicle
			for (Iterator<ExtEffectWaitingDelay> iterator = this.boardingDelayEffects.iterator(); iterator.hasNext();){
				ExtEffectWaitingDelay delay = iterator.next();
				
				if (delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					WaitingDelayEvent delayWaitingEvent = new WaitingDelayEvent(delay.getPersonId(), delay.getAffectedVehicle(), event.getTime(), delay.getAffectedAgents(), delay.getTransferDelay());
					this.events.processEvent(delayWaitingEvent);
					iterator.remove();
				}
			}
			
			// throw waitingDelayEvents induced by agents leaving a public vehicle and stop tracking delay effect of that person and public vehicle
			for (Iterator<ExtEffectWaitingDelay> iterator = this.alightingDelayEffects.iterator(); iterator.hasNext();){
				ExtEffectWaitingDelay delay = iterator.next();
				
				if (delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					WaitingDelayEvent delayWaitingEvent = new WaitingDelayEvent(delay.getPersonId(), delay.getAffectedVehicle(), event.getTime(), delay.getAffectedAgents(), delay.getTransferDelay());
					this.events.processEvent(delayWaitingEvent);
					iterator.remove();
				}
			}

						
		} else if (!ptDriverIDs.contains(event.getPersonId()) && ptVehicleIDs.contains(event.getVehicleId())){
			
			for (ExtEffectWaitingDelay delay : this.alightingDelayEffects){
				if (delay.getPersonId().toString().equals(event.getPersonId().toString()) && !delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					log.warn("Agent is already being tracked. Starting parallel personTracking for different vehicles. " +
							"That means the bus which was previously delayed by that agent has not yet arrived the end of the transit route. " +
							"Must have been a very short activity... ");
				
				} else if (delay.getPersonId().toString().equals(event.getPersonId().toString()) && delay.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					throw new RuntimeException("Person and public vehicle are already being tracked. That means, an agent " +
							"leaves a vehicle which was previously delayed by himself. Depending on the scenario this can happen if there are circle transit routes. " +
							"Handling of this specific occurence is not implemented. ");
				}
			}
			
			// start tracking the delay effect induced by that person leaving the public vehicle
			double transferTime = this.scenario.getVehicles().getVehicles().get(event.getVehicleId()).getType().getEgressTime();
			// assuming the first agent to cause an extra delay before bus arrives at stop AND after bus leaves the stop
			ExtEffectWaitingDelay delayEffect = startTrackingDelayEffect(event.getVehicleId(), event.getPersonId(), transferTime, this.extraDelay * 2);
			this.alightingDelayEffects.add(delayEffect);
		}
	}
	
	private ExtEffectWaitingDelay startTrackingDelayEffect(Id vehicleId, Id personId, double transferTime, double extraDelay) {
		
		boolean isFirstTransfer = this.vehId2isFirstTransfer.get(vehicleId);
		if (isFirstTransfer){
			this.vehId2isFirstTransfer.put(vehicleId, false);
		}
		
		double actualTransferTime = transferTime;
		if (isFirstTransfer){
			actualTransferTime = transferTime + extraDelay;
		} else {
			actualTransferTime = transferTime;
		}
		
		ExtEffectWaitingDelay delayEffect = new ExtEffectWaitingDelay();
		delayEffect.setPersonId(personId);
		delayEffect.setAffectedVehicle(vehicleId);
		delayEffect.setTransferDelay(actualTransferTime);
		delayEffect.setAffectedAgents(0);
		
		return delayEffect;
		
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		// Vehicle has arrived at transit stop. The following agent will be the first transfer of this vehicle.
		this.vehId2isFirstTransfer.put(event.getVehicleId(), true);
	}

}