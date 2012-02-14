/* *********************************************************************** *
 * project: org.matsim.*
 * VehiclesTracker.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.ptproject.qsim.InternalInterface;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.ptproject.qsim.interfaces.MobsimEngine;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

/**
 * Class that tracks vehicles and agents that travel as passengers within them.
 * 
 * Moreover, vehicle enter and leave events are created for ride_passenger trips.
 * 
 * @author cdobler
 */
public class VehiclesTracker implements SimulationInitializedListener, MobsimEngine,
	PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, AgentArrivalEventHandler,
	LinkEnterEventHandler, LinkLeaveEventHandler {
	
	private static final Logger log = Logger.getLogger(VehiclesTracker.class);
	
	private final EventsManager eventsManager;
	
	// mapping driver -> vehicle
	private final Map<Id, Id> driverVehicleMap;
	
	// mapping vehicle -> driver
	private final Map<Id, Id> vehicleDriverMap;

	// mapping passenger -> vehicle
	private final Map<Id, Id> passengerVehicleMap;
	
	// mapping vehicle -> list of its passengers (excluding the driver)
	private final Map<Id, List<Id>> vehiclePassengerMap;
		
	// vehicles currently enroute
	private final Set<Id> enrouteVehicles;
	
	// vehicles currently enroute on a given link
	private final Map<Id, List<Id>> enrouteVehiclesOnLink;
	
	// vehicles currently parked <vehicleId, linkId>
	private final Map<Id, Id> parkedVehicles;
	
	// available capacity in the vehicles
	private final Map<Id, AtomicInteger> vehicleCapacities;
	
	// agents that should be picked up by a defined vehicle
	private final Map<Id, Id> plannedPickupVehicles;
	
	private final Map<Id, MobsimAgent> agents;
	private InternalInterface internalInterface;
	
	public VehiclesTracker(EventsManager eventsManager) {
		this.eventsManager = eventsManager;

		this.driverVehicleMap = new HashMap<Id, Id>();
		this.passengerVehicleMap = new HashMap<Id, Id>();
		
		this.vehicleDriverMap = new HashMap<Id, Id>();
		this.vehiclePassengerMap = new HashMap<Id, List<Id>>();

		this.enrouteVehicles = new HashSet<Id>();
		this.enrouteVehiclesOnLink = new HashMap<Id, List<Id>>();
		
		this.parkedVehicles = new HashMap<Id, Id>();
		this.plannedPickupVehicles = new HashMap<Id, Id>();

		this.agents = new HashMap<Id, MobsimAgent>();
		this.vehicleCapacities = new HashMap<Id, AtomicInteger>();		
	}
	
	public Id getVehicleDestination(Id vehicleId) {
		Id driverId = vehicleDriverMap.get(vehicleId);
		if (driverId == null) {
			log.info(this.enrouteVehicles.contains(vehicleId));
			List<Id> vehicleIds = this.getEnrouteVehiclesOnLink(new IdImpl("113352"));
			log.info(vehicleIds.contains(vehicleIds));
			return null;
		}
		
		MobsimAgent driver = agents.get(driverId);
		if (driver == null) {
			return null;
		}
		
		Leg currentLeg = ((ExperimentalBasicWithindayAgent) driver).getCurrentLeg();
		if (currentLeg == null) {
			return null;
		}
		else return currentLeg.getRoute().getEndLinkId();
	}

	public Map<Id, Id> getPlannedPickupVehicles() {
		return this.plannedPickupVehicles;
	}
	
	public void addPlannedPickupVehicle(Id personId, Id vehicleId) {
		this.plannedPickupVehicles.put(personId, vehicleId);
	}
	
	public boolean isVehicleEnroute(Id vehicleId) {
		return enrouteVehicles.contains(vehicleId);
	}
	
	public Set<Id> getEnrouteVehicles() {
		return this.enrouteVehicles;
	}
	
	public List<Id> getEnrouteVehiclesOnLink(Id linkId) {
		return this.enrouteVehiclesOnLink.get(linkId);
	}
	
	public int getFreeVehicleCapacity(Id vehicleId) {
		return this.vehicleCapacities.get(vehicleId).get();
	}
	
	/*
	 * Before an agent can start a passenger_ride trip,
	 * it has to be registered as a passenger in a vehicle.
	 */
	public void addPassengerToVehicle(Id passengerId, Id vehicleId) {
		this.passengerVehicleMap.put(passengerId, vehicleId);
		this.vehiclePassengerMap.get(vehicleId).add(passengerId);
	}
	
	public Map<Id, Id> getParkedVehicles() {
		return this.parkedVehicles;
	}
	
	public Id getVehicleLinkId(Id vehicleId) {
		if (vehicleId == null) return null;
		
		Id driverId = vehicleDriverMap.get(vehicleId);
		if (driverId == null) return null;
		
		MobsimAgent driver = agents.get(driverId);
		if (driver == null) return null;
		
		return driver.getCurrentLinkId();
	}
	
	public Id getPassengerLinkId(Id passengerId) {
		Id vehicleId = passengerVehicleMap.get(passengerId);
		return this.getVehicleLinkId(vehicleId);
	}
	
	/*
	 * Returns the vehicle's Id that transports a given passenger.
	 */
	public Id getPassengersVehicle(Id passengerId) {
		return passengerVehicleMap.get(passengerId);
	}
		
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		QSim sim = (QSim) e.getQueueSimulation();
		
		agents.clear();
		for (MobsimAgent agent : (sim).getAgents()) {
			agents.put(agent.getId(), agent);
		}
		
		/*
		 * Get the initial coordinates of all vehicles used by agents.
		 */
		ActivityFacilities facilities = ((ScenarioImpl) sim.getScenario()).getActivityFacilities();
		ActivityFacility facility = null;
		for (Person person : sim.getScenario().getPopulation().getPersons().values()) {
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Activity) {
					Id facilityId = ((Activity) planElement).getFacilityId();
					facility = facilities.getFacilities().get(facilityId);
				} else if (planElement instanceof Leg) {
					/*
					 * If its a car leg, then we assume that the car is located
					 * at the coordinate of the previous activity.
					 */
					Leg leg = (Leg) planElement;
					if (leg.getMode().equals(TransportMode.car)) {
						NetworkRoute route = (NetworkRoute) leg.getRoute();
						parkedVehicles.put(route.getVehicleId(), facility.getLinkId());
						break;
					}
				}
			}
		}
		
		/*
		 * Get the initial capacities of all vehicles used by agents.
		 */
		this.vehicleCapacities.clear();
		Vehicles vehicles = ((ScenarioImpl) sim.getScenario()).getVehicles();
		for (Vehicle vehicle : vehicles.getVehicles().values()) {
			int capacity = vehicle.getType().getCapacity().getSeats();
			this.vehicleCapacities.put(vehicle.getId(), new AtomicInteger(capacity));
		}
		
		/* 
		 * Initialize some maps 
		 */
		for (Link link : sim.getNetsimNetwork().getNetwork().getLinks().values()) {
			this.enrouteVehiclesOnLink.put(link.getId(), new ArrayList<Id>());
		}
		
		for (Id vehicleId : this.parkedVehicles.keySet()) vehiclePassengerMap.put(vehicleId, new ArrayList<Id>());
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.vehicleCapacities.get(event.getVehicleId()).decrementAndGet();
		
		boolean isPassenger = passengerVehicleMap.containsKey(event.getPersonId());
		if (!isPassenger) {
			driverVehicleMap.put(event.getPersonId(), event.getVehicleId());
			vehicleDriverMap.put(event.getVehicleId(), event.getPersonId());
		}
		
		boolean isDriver = driverVehicleMap.containsKey(event.getPersonId());
		if (isDriver) {
			if (event.getVehicleId().toString().equals("2110188_veh1")) {
				log.info("Entering vehicle..." + event.getTime());
			}
			this.driverVehicleMap.put(event.getPersonId(), event.getVehicleId());
			this.enrouteVehicles.add(event.getVehicleId());
			this.parkedVehicles.remove(event.getVehicleId());
			
			Id linkId = this.agents.get(event.getPersonId()).getCurrentLinkId();
			List<Id> vehicleIds = this.enrouteVehiclesOnLink.get(linkId);
			vehicleIds.add(event.getVehicleId());			
//			log.info("Add person " + event.getPersonId().toString() + " using vehicle " + event.getVehicleId().toString() + " to link " + linkId.toString());
		} 
		
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
			if (foundId == null) {
				log.warn("No vehicle found for agent (" + event.getPersonId() + "). Expected vehicle (" + expectedId.toString() + ").");
			} else if (!foundId.equals(expectedId)) {
				log.warn("Found vehicle (" + foundId.toString() + ") does not match expected vehicle (" + expectedId.toString() + ").");
			}
		}
	}
		
	/*
	 *  The driver has left the vehicle. Replicate this event for all passengers.
	 */
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		this.vehicleCapacities.get(event.getVehicleId()).incrementAndGet();
		
		boolean isDriver = driverVehicleMap.containsKey(event.getPersonId());
		if (isDriver) {
			if (event.getVehicleId().toString().equals("2110188_veh1")) {
				log.info("Leaving vehicle..." + event.getTime());
			}
			Id linkId = this.agents.get(event.getPersonId()).getCurrentLinkId();

			this.enrouteVehicles.remove(event.getVehicleId());
			this.parkedVehicles.put(event.getVehicleId(), linkId);
			
			List<Id> vehicleIds = this.enrouteVehiclesOnLink.get(linkId);
//			log.info("Remove person " + event.getPersonId().toString() + " using vehicle " + event.getVehicleId().toString() + " from link " + linkId.toString());
			if(!vehicleIds.remove(event.getVehicleId())) {
				log.warn("Tried to remove vehicle from enrouteVehiclesOnLink map but failed!");
				log.warn("Vehicle " + event.getVehicleId());
				log.warn("Link " + linkId);
			}
			
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
//		if (!checkTime(event)) return;
		
		// If it is the vehicles driver, there is an entry in the driverVehicleMap
		Id vehicleId = driverVehicleMap.remove(event.getPersonId());
		if (vehicleId != null) {
			vehicleDriverMap.remove(vehicleId);
			List<Id> passengers = vehiclePassengerMap.get(vehicleId);
			
			if (passengers != null) {
				for (Id passengerId : passengers) {
					/*
					 * The AgentArrivalEvent for the passenger is created 
					 * within the endLegAndAssumeControl method. Moreover,
					 * the currently performed leg of the agent is ended.
					 */
//					MobsimAgent passenger = enroutePassengers.remove(passengerId);
					MobsimAgent passenger = this.agents.get(passengerId);
					passenger.notifyTeleportToLink(event.getLinkId());	// use drivers position
					passenger.endLegAndAssumeControl(event.getTime());	
					this.internalInterface.arrangeNextAgentState(passenger);

					// remove passenger from map
					passengerVehicleMap.remove(event.getPersonId());
				}
				// remove passengers from list
				passengers.clear();
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		boolean isDriver = driverVehicleMap.containsKey(event.getPersonId());
		if (isDriver) {
//		log.info("Add person " + event.getPersonId().toString() + " using vehicle " + event.getVehicleId().toString() + " to link " + event.getLinkId().toString());
			List<Id> vehicleIds = this.enrouteVehiclesOnLink.get(event.getLinkId());
			vehicleIds.add(event.getVehicleId());		
		}
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		boolean isDriver = driverVehicleMap.containsKey(event.getPersonId());
		if (isDriver) {
//		log.info("Remove person " + event.getPersonId().toString() + " using vehicle " + event.getVehicleId().toString() + " from link " + event.getLinkId().toString());
			List<Id> vehicleIds = this.enrouteVehiclesOnLink.get(event.getLinkId());
			vehicleIds.add(event.getVehicleId());
		}
	}
	
	@Override
	public void reset(int iteration) {
		this.driverVehicleMap.clear();
		this.passengerVehicleMap.clear();
		this.vehicleDriverMap.clear();
		this.vehiclePassengerMap.clear();
//		this.enroutePassengers.clear();
		this.enrouteVehicles.clear();
		this.enrouteVehiclesOnLink.clear();
		this.parkedVehicles.clear();
		this.plannedPickupVehicles.clear();
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
