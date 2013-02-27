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

import playground.ikaddoura.optimization.events.ExternalEffectWaitingTimeEvent;

/**
 * Calculates the external effect (caused by delays when boarding and alighting a public vehicle) on agents who are waiting for the public vehicle.
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
public class ExternalEffectWaitingHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler {
	private final static Logger log = Logger.getLogger(ExternalEffectWaitingHandler.class);

	private final ScenarioImpl scenario;
	private final EventsManager events;
	private final List<Id> ptDriverIDs = new ArrayList<Id>();
	private final List<Id> ptVehicleIDs = new ArrayList<Id>();
		
	private final Map<Id, Boolean> vehId2isFirstTransfer = new HashMap<Id, Boolean>();
	private final Map<Id, ExtDelayEffect> personId2extBoardingDelayEffect = new HashMap<Id, ExtDelayEffect>();
	private final Map<Id, ExtDelayEffect> personId2extAlightingDelayEffect = new HashMap<Id, ExtDelayEffect>();

	public ExternalEffectWaitingHandler(EventsManager events, ScenarioImpl scenario) {
		this.events = events;
		this.scenario = scenario;
	}
	
	@Override
	public void reset(int iteration) {
		this.ptDriverIDs.clear();
		this.ptVehicleIDs.clear();
		this.vehId2isFirstTransfer.clear();
		this.personId2extBoardingDelayEffect.clear();
		this.personId2extAlightingDelayEffect.clear();
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
		
		double transferTime = this.scenario.getVehicles().getVehicles().get(event.getVehicleId()).getType().getAccessTime();
		
		if (!ptDriverIDs.contains(event.getPersonId()) && ptVehicleIDs.contains(event.getVehicleId())){
						
			for (Id personId : this.personId2extBoardingDelayEffect.keySet()){
				ExtDelayEffect delayEffect = this.personId2extBoardingDelayEffect.get(personId);
				if (delayEffect.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					log.warn(event.getPersonId() + " enters a vehicle which was delayed by " + personId + ".");
					log.warn("Increasing the number of affected agents for " + personId + ".");
					int affectedAgents = delayEffect.getAffectedAgents();
					delayEffect.setAffectedAgents(affectedAgents + 1);
				}
			}
						
			boolean isFirstTransfer = this.vehId2isFirstTransfer.get(event.getVehicleId());
			if (isFirstTransfer){
				this.vehId2isFirstTransfer.put(event.getVehicleId(), false);
			}
			
			double delay = 0.;
			if (isFirstTransfer){
				//	Each time a public vehicle stops at a transit stop the public vehicle is delayed by 2 seconds.
				//	Assuming this time to belong to the marginal user costs of the first person entering or leaving a public vehicle.
				double extraDelay = 2.;
				delay = transferTime + extraDelay;
			} else {
				delay = transferTime;
			}
			
			ExtDelayEffect delayEffect = new ExtDelayEffect();
			delayEffect.setPersonId(event.getPersonId());
			delayEffect.setAffectedVehicle(event.getVehicleId());
			delayEffect.setTransferDelay(delay);
			delayEffect.setAffectedAgents(0);
			
			this.personId2extBoardingDelayEffect.put(event.getPersonId(), delayEffect);
		
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		
		if (ptDriverIDs.contains(event.getPersonId())){
			// end of the transit route
			// throw externalDelayEvents and remove agents that affected this bus!
			
			for (Id personId : this.personId2extBoardingDelayEffect.keySet()){
				ExtDelayEffect delayEffect = this.personId2extBoardingDelayEffect.get(personId);
				
				if (delayEffect.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					ExternalEffectWaitingTimeEvent delayWaitingEvent = new ExternalEffectWaitingTimeEvent(personId, event.getVehicleId(), event.getTime(), delayEffect.getAffectedAgents(), delayEffect.getTransferDelay());
					this.events.processEvent(delayWaitingEvent);
				}
			}
			
			for (Id personId : this.personId2extAlightingDelayEffect.keySet()){
				ExtDelayEffect delayEffect = this.personId2extAlightingDelayEffect.get(personId);
				
				if (delayEffect.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					ExternalEffectWaitingTimeEvent delayWaitingEvent = new ExternalEffectWaitingTimeEvent(personId, event.getVehicleId(), event.getTime(), delayEffect.getAffectedAgents(), delayEffect.getTransferDelay());
					this.events.processEvent(delayWaitingEvent);
				}
			}

		} else if (!ptDriverIDs.contains(event.getPersonId()) && ptVehicleIDs.contains(event.getVehicleId())){
			
			for (Id personId : this.personId2extAlightingDelayEffect.keySet()){
				ExtDelayEffect delayEffect = this.personId2extAlightingDelayEffect.get(personId);
				
				if (delayEffect.getAffectedVehicle().toString().equals(event.getVehicleId().toString())){
					int affectedAgents = delayEffect.getAffectedAgents();
					delayEffect.setAffectedAgents(affectedAgents + 1);
				}
			}
			
			boolean isFirstTransfer = this.vehId2isFirstTransfer.get(event.getVehicleId());
			if (isFirstTransfer){
				this.vehId2isFirstTransfer.put(event.getVehicleId(), false);
			}
			
			//	Each time a public vehicle stops at a transit stop the public vehicle is delayed by 2 seconds.
			//	Assuming this time to belong to the marginal user costs of the first person entering or leaving a public vehicle.
			double transferTime = this.scenario.getVehicles().getVehicles().get(event.getVehicleId()).getType().getEgressTime();
			double actualTransferTime = transferTime;
			if (isFirstTransfer){
				double extraDelay = 2.;
				actualTransferTime = transferTime + extraDelay;
			}
			
			ExtDelayEffect delayEffect = new ExtDelayEffect();
			delayEffect.setPersonId(event.getPersonId());
			delayEffect.setAffectedVehicle(event.getVehicleId());
			delayEffect.setTransferDelay(actualTransferTime);
			delayEffect.setAffectedAgents(0);
			
			this.personId2extAlightingDelayEffect.put(event.getPersonId(), delayEffect);
			
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		// Vehicle has arrived at transit stop. The following agent will be the first transfer of this vehicle.
		this.vehId2isFirstTransfer.put(event.getVehicleId(), true);
	}
	
	// #########################################################################################################
	// #########################################################################################################

}