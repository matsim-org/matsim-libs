/* *********************************************************************** *
 * project: org.matsim.*
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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.BoardingDeniedEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.handler.BoardingDeniedEventHandler;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspExperimentalConfigKey;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.scenario.ScenarioImpl;

/**
 * Calculates and throws external delay effect events that are related to capacity constraints of public vehicles.
 * 
 * @author Ihab
 *
 */
public class CapacityDelayHandler implements BoardingDeniedEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, TransitDriverStartsEventHandler {
	
	private final static Logger log = Logger.getLogger(CapacityDelayHandler.class);
	
	private final ScenarioImpl scenario;
	private final EventsManager events;
	
	private final Map<Id, List<Id>> affectedAgent2causingAgents = new HashMap<Id, List<Id>>();
	private final Map<Id, Double> affectedAgent2boardingDeniedTime = new HashMap<Id, Double>();
	private final Map<Id, Id> affectedAgent2deniedVehicle = new HashMap<Id, Id>();
	private final Map<Id, List<Id>> vehId2passengers = new HashMap<Id, List<Id>>();
	private final Map<Id, Id> vehId2lastEnteringAgent = new HashMap<Id, Id>();
	
	private final List<Id> ptVehicleIDs = new ArrayList<Id>();
	private final List<Id> ptDriverIDs = new ArrayList<Id>();
	
	private final CausingAgentsMethod causingAgentsMethod = CausingAgentsMethod.allPassengersInThePublicVehicle;
	
	public CapacityDelayHandler(EventsManager events, ScenarioImpl scenario) {
		this.events = events;
		this.scenario = scenario;
		
		log.info("Calculating external delay effects due to capacity constraints of public vehicles.");
		
		if (this.scenario.getConfig().vspExperimental().getValue(VspExperimentalConfigKey.isGeneratingBoardingDeniedEvent).equals("false")){
			throw new RuntimeException("Expecting BoardingDeniedEvents to be generated. Please set config parameter isGeneratingBoardingDeniedEvent in vspExperimental to true. Aborting...");
		}
		
	}
	
	@Override
	public void reset(int iteration) {
		this.ptVehicleIDs.clear();
		this.ptDriverIDs.clear();
		this.affectedAgent2causingAgents.clear();
		this.affectedAgent2boardingDeniedTime.clear();
		this.affectedAgent2deniedVehicle.clear();
		this.vehId2passengers.clear();
		this.vehId2lastEnteringAgent.clear();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		
		if (!this.ptVehicleIDs.contains(event.getVehicleId())){
			this.ptVehicleIDs.add(event.getVehicleId());
		}
		
		if (!this.ptDriverIDs.contains(event.getDriverId())){
			this.ptDriverIDs.add(event.getDriverId());
		}
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
				
		if (!ptDriverIDs.contains(event.getPersonId()) && ptVehicleIDs.contains(event.getVehicleId())){
			// a "normal" agent is entering a public vehicle
			
			System.out.println(event.toString());
			
			if (this.affectedAgent2causingAgents.containsKey(event.getPersonId())){
				System.out.println("Boarding agent was boarding denied before.");
				calculateExternalDelay(event.getTime(), event.getPersonId());
			}
			
			// update number of passengers in vehicle
			if (this.vehId2passengers.containsKey(event.getVehicleId())){
				List<Id> passengers = new ArrayList<Id>();
				passengers = this.vehId2passengers.get(event.getVehicleId());
				passengers.add(event.getPersonId());
				this.vehId2passengers.put(event.getVehicleId(), passengers);
			} else {
				List<Id> passengersInVeh = new ArrayList<Id>();
				passengersInVeh.add(event.getPersonId());
				this.vehId2passengers.put(event.getVehicleId(), passengersInVeh);
			}
			
			// update last entering agent
			this.vehId2lastEnteringAgent.put(event.getVehicleId(), event.getPersonId());
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		
		if (!ptDriverIDs.contains(event.getPersonId()) && ptVehicleIDs.contains(event.getVehicleId())){
			// a "normal" agent is leaving a public vehicle
			
			// update number of passengers in vehicle
			if (this.vehId2passengers.containsKey(event.getVehicleId())){
				List<Id> passengers = new ArrayList<Id>();
				passengers = this.vehId2passengers.get(event.getVehicleId());
				passengers.remove(event.getVehicleId());
				this.vehId2passengers.put(event.getVehicleId(), passengers);
			} else {
				throw new RuntimeException("A person is leaving a public vehicle without entering it before. Aborting...");
			}
		}
		
	}
	
	@Override
	public void handleEvent(BoardingDeniedEvent event) {
		System.out.println(event.toString());
		
		if (this.affectedAgent2causingAgents.containsKey(event.getPersonId())){
			calculateExternalDelay(event.getTime(), event.getPersonId());
		}
		List<Id> causingAgents = new ArrayList<Id>();
		
		if (causingAgentsMethod.equals(CausingAgentsMethod.allPassengersInThePublicVehicle)){
			causingAgents = getAllAgentsInPublicVehicle(event.getVehicleId());
		} else if (causingAgentsMethod.equals(CausingAgentsMethod.lastAgentEnteringThePublicVehicle)){
			causingAgents = getLastAgentEnteringPublicVehicle(event.getVehicleId());
		} else {
			throw new RuntimeException("Unknown method for the identfication of the causing agent(s). Aborting...");
		}
		
		System.out.println("Affected agent: " + event.getPersonId());
		System.out.println("Causing agents: " + causingAgents.toString());

		this.affectedAgent2boardingDeniedTime.put(event.getPersonId(), event.getTime());
		this.affectedAgent2causingAgents.put(event.getPersonId(), causingAgents);
		this.affectedAgent2deniedVehicle.put(event.getPersonId(), event.getVehicleId());

	}

	private List<Id> getLastAgentEnteringPublicVehicle(Id vehicleId) {
		List<Id> lastAgentEnteringPublicVehicle = new ArrayList<Id>();
		lastAgentEnteringPublicVehicle.add(this.vehId2lastEnteringAgent.get(vehicleId));
		return lastAgentEnteringPublicVehicle;
	}

	private List<Id> getAllAgentsInPublicVehicle(Id vehicleId) {
		List<Id> agentsInPublicVehicle = new ArrayList<Id>();
		agentsInPublicVehicle = this.vehId2passengers.get(vehicleId);
		return agentsInPublicVehicle;
	}

	private void calculateExternalDelay(double time, Id affectedAgentId) {
		
		double delay = time - this.affectedAgent2boardingDeniedTime.get(affectedAgentId);
		System.out.println("Delay: " + delay);
		List<Id> causingAgents = this.affectedAgent2causingAgents.get(affectedAgentId);
		System.out.println("Causing agents: " + causingAgents);
		double delayPerCausingAgent = delay / causingAgents.size();
		
		for (Id causingAgentId : causingAgents) {
			CapacityDelayEvent capacityDelayEvent = new CapacityDelayEvent(time, causingAgentId, affectedAgentId, this.affectedAgent2deniedVehicle.get(affectedAgentId), delayPerCausingAgent);
			System.out.println("Capacity delay event: " + capacityDelayEvent.toString());
			this.events.processEvent(capacityDelayEvent);
		}
				
		this.affectedAgent2causingAgents.remove(affectedAgentId);
		this.affectedAgent2boardingDeniedTime.remove(affectedAgentId);
		this.affectedAgent2deniedVehicle.remove(affectedAgentId);
	}
	
	// ######################################################################
	
	private static enum CausingAgentsMethod {
		allPassengersInThePublicVehicle,
		lastAgentEnteringThePublicVehicle
	}
	
}