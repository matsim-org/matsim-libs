/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerTracker.java
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

package playground.christoph.evacuation.mobsim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.InternalInterface;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.MobsimEngine;
import org.matsim.ptproject.qsim.interfaces.Netsim;

import playground.christoph.evacuation.config.EvacuationConfig;

/**
 * Class that tracks agents that travel as passengers within a car.
 * 
 * @author cdobler
 */
public class PassengerTracker implements SimulationInitializedListener, MobsimEngine,
	PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, AgentArrivalEventHandler {
	
	private static final Logger log = Logger.getLogger(PassengerTracker.class);
	
	private final EventsManager eventsManager;
	
	// mapping driver -> vehicle
	private final Map<Id, Id> driverVehicleMap;

	// mapping passenger -> vehicle
	private final Map<Id, Id> passengerVehicleMap;
	
	// agents currently traveling as passengers
	private final Map<Id, MobsimAgent> enroutePassengers;
	
	// mapping vehicle -> driver
	private final Map<Id, Id> vehicleDriverMap;
	
	// mapping vehicle -> list of its passengers (excluding the driver)
	private final Map<Id, List<Id>> vehiclePassengerMap;
	
	private Map<Id, MobsimAgent> agents;
	private InternalInterface internalInterface;
	
	public PassengerTracker(EventsManager eventsManager) {
		this.eventsManager = eventsManager;

		this.driverVehicleMap = new HashMap<Id, Id>();
		this.passengerVehicleMap = new HashMap<Id, Id>();
		
		this.vehicleDriverMap = new HashMap<Id, Id>();
		this.vehiclePassengerMap = new HashMap<Id, List<Id>>();

		this.enroutePassengers = new HashMap<Id, MobsimAgent>();
	}
	
	public void addVehicleAllocation(Id vehicleId, Id driverId, List<Id> passengerIds) {
		driverVehicleMap.put(driverId, vehicleId);
		for (Id passengerId : passengerIds) passengerVehicleMap.put(passengerId, vehicleId);
		
		vehicleDriverMap.put(vehicleId, driverId);
		vehiclePassengerMap.put(vehicleId, passengerIds);
	}
	
	public Id getPassengerLinkId(Id passengerId) {
		Id vehicleId = passengerVehicleMap.get(passengerId);
		if (vehicleId == null) return null;
		
		Id driverId = vehicleDriverMap.get(vehicleId);
		if (driverId == null) return null;
		
		MobsimAgent driver = agents.get(driverId);
		if (driver == null) return null;
		
		return driver.getCurrentLinkId();
	}

	/**
	 * If the given event occurred after the evacuation has started,
	 * true is returned. Otherwise false.
	 * @param e Event
	 * @return
	 */
	private boolean checkTime(Event e) {
		if (e.getTime() < EvacuationConfig.evacuationTime) return false;
		else return true;
	}
	
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		agents = new HashMap<Id, MobsimAgent>();
		for (MobsimAgent agent : ((QSim) e.getQueueSimulation()).getAgents()) {
			agents.put(agent.getId(), agent);
		}
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!checkTime(event)) return;
		
		boolean isDriver = driverVehicleMap.containsKey(event.getPersonId());
		if (isDriver) this.driverVehicleMap.put(event.getPersonId(), event.getVehicleId());
		
		// consistency checks
		Id expectedId;
		Id foundId;
		if (isDriver) {
			expectedId = event.getPersonId();
			foundId = vehicleDriverMap.get(event.getVehicleId());
			if (!foundId.equals(expectedId)) {
				log.warn("Found driver (" + foundId.toString() + ") does not match expected driver (" + expectedId.toString() + ").");
			}
			
			expectedId = event.getVehicleId();
			foundId = driverVehicleMap.get(event.getPersonId());
			if (!foundId.equals(expectedId)) {
				log.warn("Found vehicle (" + foundId.toString() + ") does not match expected vehicle (" + expectedId.toString() + ").");
			}
		} else {
			expectedId = event.getPersonId();
			List<Id> foundIds = vehiclePassengerMap.get(event.getVehicleId()); 
			if (!foundIds.contains(expectedId)) {
				log.warn("Passenger (" + expectedId.toString() + ") is not included in vehicles passenger list.");
			}
			
			expectedId = event.getVehicleId();
			foundId = passengerVehicleMap.get(event.getPersonId());
			if (!foundId.equals(expectedId)) {
				log.warn("Found vehicle (" + foundId.toString() + ") does not match expected vehicle (" + expectedId.toString() + ").");
			}
		}
	}
		
	/*
	 *  The driver has left the vehicle. Replicate this event for all passengers.
	 */
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (!checkTime(event)) return;
		
		boolean isDriver = driverVehicleMap.containsKey(event.getPersonId());
		if (isDriver) {
			List<Id> passengers = vehiclePassengerMap.get(event.getVehicleId());
			if (passengers != null) {
				for (Id passengerId : passengers) {
					Event e = eventsManager.getFactory().createPersonLeavesVehicleEvent(event.getTime(), passengerId, event.getVehicleId());
					eventsManager.processEvent(e);
				}
			}	
		}
	}
	
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (!checkTime(event)) return;
		
		Id vehicleId = driverVehicleMap.remove(event.getPersonId());
		if (vehicleId != null) {
			vehicleDriverMap.remove(vehicleId);
			List<Id> passengers = vehiclePassengerMap.remove(vehicleId);
			
			if (passengers != null) {
				for (Id passengerId : passengers) {
					/*
					 * The AgentArrivalEvent for the passenger is created 
					 * within the endLegAndAssumeControl method. Moreover,
					 * the currently performed leg of the agent is ended.
					 */
					MobsimAgent passenger = enroutePassengers.remove(passengerId);
					passenger.notifyTeleportToLink(event.getLinkId());	// use drivers position
					passenger.endLegAndAssumeControl(event.getTime());	
					this.internalInterface.arrangeNextAgentState(passenger);

					// remove passenger from map
					passengerVehicleMap.remove(event.getPersonId());
				}
			}
		}
	}
	
	@Override
	public void reset(int iteration) {
		this.driverVehicleMap.clear();
		this.passengerVehicleMap.clear();
		this.vehicleDriverMap.clear();
		this.vehiclePassengerMap.clear();
		this.enroutePassengers.clear();
	}

	public void addEnrouteAgent(MobsimAgent agent) {
		this.enroutePassengers.put(agent.getId(), agent);
	}
	
	/*
	 * Returns the vehicle's Id that transports a given passenger.
	 */
	public Id getPassengersVehicle(Id passengerId) {
		return passengerVehicleMap.get(passengerId);
	}
	
	@Override
	public void doSimStep(double time) {
		// TODO Auto-generated method stub
	}

	@Override
	public Netsim getMobsim() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onPrepareSim() {
		// TODO Auto-generated method stub
	}

	@Override
	public void afterSim() {
		// TODO Auto-generated method stub	
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

}
