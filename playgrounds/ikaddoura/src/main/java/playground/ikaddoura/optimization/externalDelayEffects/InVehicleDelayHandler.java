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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.scenario.ScenarioImpl;



/**
 * Throws InVehicleDelayEvents to indicate that an agent entering or leaving a public vehicle delayed passengers being in that public vehicle.
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
public class InVehicleDelayHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {
	private final static Logger log = Logger.getLogger(InVehicleDelayHandler.class);

	private final double extraDelay = 1.0;
	private final ScenarioImpl scenario;
	private final EventsManager events;
	private final List<Id> ptDriverIDs = new ArrayList<Id>();
	private final List<Id> ptVehicleIDs = new ArrayList<Id>();
	private final Map<Id, Integer> vehId2passengers = new HashMap<Id, Integer>();
	
	private final Map<Id, Id> vehId2firstTransferingAgent = new HashMap<Id, Id>();
	private final Map<Id, Id> vehId2lastTransferingAgent = new HashMap<Id, Id>();
	
	private final Map<Id, Integer> vehId2firstTransferAffectedAgents = new HashMap<Id, Integer>();
	private final Map<Id, Integer> vehId2lastTransferAffectedAgents = new HashMap<Id, Integer>();
	
	public InVehicleDelayHandler(EventsManager events, ScenarioImpl scenario) {
		this.events = events;
		this.scenario = scenario;
	}
	
	@Override
	public void reset(int iteration) {
		this.vehId2passengers.clear();
		this.ptDriverIDs.clear();
		this.ptVehicleIDs.clear();
		this.vehId2firstTransferingAgent.clear();
		this.vehId2lastTransferingAgent.clear();
		this.vehId2firstTransferAffectedAgents.clear();
		this.vehId2lastTransferAffectedAgents.clear();
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
													
			double delay = this.scenario.getVehicles().getVehicles().get(event.getVehicleId()).getType().getAccessTime();
			int delayedPassengers_inVeh = calcDelayedPassengersInVeh(event.getVehicleId());
			InVehicleDelayEvent delayInVehicleEvent = new InVehicleDelayEvent(event.getPersonId(), event.getVehicleId(), event.getTime(), delayedPassengers_inVeh, delay);
			this.events.processEvent(delayInVehicleEvent);
			
			// remember first and last transfering agent
			if (this.vehId2firstTransferingAgent.get(event.getVehicleId()) == null){
				// No agent was leaving the vehicle before. Thus, this agent is the first transfering agent.
				this.vehId2firstTransferingAgent.put(event.getVehicleId(), event.getPersonId());
				this.vehId2firstTransferAffectedAgents.put(event.getVehicleId(), delayedPassengers_inVeh);
				
			}
			this.vehId2lastTransferingAgent.put(event.getVehicleId(), event.getPersonId());
			this.vehId2lastTransferAffectedAgents.put(event.getVehicleId(), delayedPassengers_inVeh);
			
			// update number of passengers in vehicle after calculating the external effect
			if (this.vehId2passengers.containsKey(event.getVehicleId())){
				int passengersInVeh = this.vehId2passengers.get(event.getVehicleId());
				this.vehId2passengers.put(event.getVehicleId(), passengersInVeh + 1);
			} else {
				this.vehId2passengers.put(event.getVehicleId(), 1);
			}
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		
		if (!ptDriverIDs.contains(event.getPersonId()) && ptVehicleIDs.contains(event.getVehicleId())){		
						
			// update number of passengers in vehicle before throwing delay event
			int passengersInVeh = this.vehId2passengers.get(event.getVehicleId());
			this.vehId2passengers.put(event.getVehicleId(), passengersInVeh - 1);
			
			double delay = this.scenario.getVehicles().getVehicles().get(event.getVehicleId()).getType().getEgressTime();
			int delayedPassengers_inVeh = calcDelayedPassengersInVeh(event.getVehicleId());
			
			// throw delay event
			InVehicleDelayEvent delayInVehicleEvent = new InVehicleDelayEvent(event.getPersonId(), event.getVehicleId(), event.getTime(), delayedPassengers_inVeh, delay);
			this.events.processEvent(delayInVehicleEvent);
			
			// remember first and last transfering agent and the number of affected agents
			if (this.vehId2firstTransferingAgent.get(event.getVehicleId()) == null){
				this.vehId2firstTransferingAgent.put(event.getVehicleId(), event.getPersonId());
				this.vehId2firstTransferAffectedAgents.put(event.getVehicleId(), delayedPassengers_inVeh);
			}
			this.vehId2lastTransferingAgent.put(event.getVehicleId(), event.getPersonId());
			this.vehId2lastTransferAffectedAgents.put(event.getVehicleId(), delayedPassengers_inVeh);
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		// Vehicle has arrived at transit stop. Reset all first / last transfer informations for that vehicle.
		this.vehId2firstTransferingAgent.remove(event.getVehicleId());
		this.vehId2lastTransferingAgent.remove(event.getVehicleId());
		this.vehId2firstTransferAffectedAgents.remove(event.getVehicleId());
		this.vehId2lastTransferAffectedAgents.remove(event.getVehicleId());	
	}
	
	private int calcDelayedPassengersInVeh(Id vehId) {
		int delayedPassengersInVeh = 0;
		if (this.vehId2passengers.containsKey(vehId)) {
			delayedPassengersInVeh = this.vehId2passengers.get(vehId);
		}
		return delayedPassengersInVeh;
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		// vehicle departs at facility.
		
		if (!(this.vehId2firstTransferingAgent.get(event.getVehicleId()) == null)){
			// throw extra delay event for first transfering agent.
			InVehicleDelayEvent delayInVehicleEvent = new InVehicleDelayEvent(this.vehId2firstTransferingAgent.get(event.getVehicleId()), event.getVehicleId(), event.getTime(), this.vehId2firstTransferAffectedAgents.get(event.getVehicleId()), this.extraDelay);
			this.events.processEvent(delayInVehicleEvent);
		}
		
		if (!(this.vehId2lastTransferingAgent.get(event.getVehicleId()) == null)){
			// throw extra delay event for last transfering agent.
			InVehicleDelayEvent delayInVehicleEvent = new InVehicleDelayEvent(this.vehId2lastTransferingAgent.get(event.getVehicleId()), event.getVehicleId(), event.getTime(), this.vehId2lastTransferAffectedAgents.get(event.getVehicleId()), this.extraDelay);
			this.events.processEvent(delayInVehicleEvent);
		}
	}
	
}