/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.comparators.QVehicleEarliestLinkExitTimeComparator;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * 
 * Please read the docu AbstractQLane and QLinkImpl jointly. kai, nov'11
 * 
 * 
 * @author nagel
 *
 */
abstract class AbstractQLink extends QLinkInternalI {

	private static final Comparator<QVehicle> VEHICLE_EXIT_COMPARATOR = new QVehicleEarliestLinkExitTimeComparator();

	private static Logger log = Logger.getLogger(AbstractQLink.class);
	
	final Link link;

	final QNetwork network;	

	// joint implementation for Customizable
	private Map<String, Object> customAttributes = new HashMap<String, Object>();

	private final Map<Id, QVehicle> parkedVehicles = new LinkedHashMap<Id, QVehicle>(10);

	private final Map<Id, MobsimAgent> additionalAgentsOnLink = new LinkedHashMap<Id, MobsimAgent>();

	private final Map<Id, MobsimDriverAgent> driversWaitingForCars = new LinkedHashMap<Id, MobsimDriverAgent>();
	
	private final Map<Id, MobsimDriverAgent> driversWaitingForPassengers = new LinkedHashMap<Id, MobsimDriverAgent>();
	
	// vehicleId 
	private final Map<Id, Set<MobsimAgent>> passengersWaitingForCars = new LinkedHashMap<Id, Set<MobsimAgent>>();

	/**
	 * A list containing all transit vehicles that are at a stop but not
	 * blocking other traffic on the lane.
	 */
	/*package*/ final Queue<QVehicle> transitVehicleStopQueue = new PriorityQueue<QVehicle>(5, VEHICLE_EXIT_COMPARATOR);

	/**
	 * All vehicles from parkingList move to the waitingList as soon as their time
	 * has come. They are then filled into the vehQueue, depending on free space
	 * in the vehQueue
	 */
	/*package*/ final Queue<QVehicle> waitingList = new LinkedList<QVehicle>();

	/*package*/ NetElementActivator netElementActivator;

	AbstractQLink(Link link, QNetwork network) {
		this.link = link ;
		this.network = network;
		this.netElementActivator = network.simEngine;
	}

	abstract boolean doSimStep(double now);

	abstract void activateLink();

	abstract void addFromIntersection(final QVehicle veh);

	abstract QNode getToNode();

	/*package*/ final void addParkedVehicle(MobsimVehicle vehicle) {
		QVehicle qveh = (QVehicle) vehicle; // cast ok: when it gets here, it needs to be a qvehicle to work.
		this.parkedVehicles.put(qveh.getId(), qveh);
		qveh.setCurrentLink(this.link);
	}

	/*package*/ final QVehicle removeParkedVehicle(Id vehicleId) {
		return this.parkedVehicles.remove(vehicleId);
	}

	/*package*/ QVehicle getParkedVehicle(Id vehicleId) {
		return this.parkedVehicles.get(vehicleId);
	}
	
	/*package*/ final void addDepartingVehicle(MobsimVehicle mvehicle) {
		QVehicle vehicle = (QVehicle) mvehicle;
		this.waitingList.add(vehicle);
		vehicle.setCurrentLink(this.getLink());
		this.activateLink();
	}

	/*package*/ void registerAdditionalAgentOnLink(MobsimAgent planAgent) {
		this.additionalAgentsOnLink.put(planAgent.getId(), planAgent);
	}

	/*package*/ MobsimAgent unregisterAdditionalAgentOnLink(Id mobsimAgentId) {
		return this.additionalAgentsOnLink.remove(mobsimAgentId);
	}

	/*package*/ Collection<MobsimAgent> getAdditionalAgentsOnLink() {
		return Collections.unmodifiableCollection( this.additionalAgentsOnLink.values());
	}

	void clearVehicles() {
		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();

		/*
		 * Some agents might be present in multiple lists/maps.
		 * Ensure that only one stuck event per agent is created.
		 */
		Set<Id> stuckAgents = new HashSet<Id>();
		
		for (QVehicle veh : this.parkedVehicles.values()) {
			if (veh.getDriver() != null) {
				// skip transit driver which perform an activity while their vehicle is parked
				if (veh.getDriver().getState() != State.LEG) continue;

				if (stuckAgents.contains(veh.getDriver().getId())) continue;
				else stuckAgents.add(veh.getDriver().getId());
				
				
				this.network.simEngine.getMobsim().getEventsManager().processEvent(
						new AgentStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
				this.network.simEngine.getMobsim().getAgentCounter().incLost();
				this.network.simEngine.getMobsim().getAgentCounter().decLiving();
			}
			
			for (PassengerAgent passenger : veh.getPassengers()) {
				if (stuckAgents.contains(passenger.getId())) continue;
				else stuckAgents.add(passenger.getId());
				
				MobsimAgent mobsimAgent = (MobsimAgent) passenger;
				
				this.network.simEngine.getMobsim().getEventsManager().processEvent(
						new AgentStuckEvent(now, mobsimAgent.getId(), veh.getCurrentLink().getId(), mobsimAgent.getMode()));
				this.network.simEngine.getMobsim().getAgentCounter().incLost();
				this.network.simEngine.getMobsim().getAgentCounter().decLiving();
			}
		}
		this.parkedVehicles.clear();
		for (MobsimAgent driver : driversWaitingForPassengers.values()) {		
			if (stuckAgents.contains(driver.getId())) continue;
			else stuckAgents.add(driver.getId());
			
			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentStuckEvent(now, driver.getId(), driver.getCurrentLinkId(), driver.getMode()));
			this.network.simEngine.getMobsim().getAgentCounter().incLost();
			this.network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		driversWaitingForPassengers.clear();
		
		
		for (MobsimAgent driver : driversWaitingForCars.values()) {
			if (stuckAgents.contains(driver.getId())) continue;
			else stuckAgents.add(driver.getId());
			
			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentStuckEvent(now, driver.getId(), driver.getCurrentLinkId(), driver.getMode()));
			this.network.simEngine.getMobsim().getAgentCounter().incLost();
			this.network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		driversWaitingForCars.clear();
		for (Set<MobsimAgent> passengers : passengersWaitingForCars.values()) {
			for (MobsimAgent passenger : passengers) {
				if (stuckAgents.contains(passenger.getId())) continue;
				else stuckAgents.add(passenger.getId());
				
				this.network.simEngine.getMobsim().getEventsManager().processEvent(
						new AgentStuckEvent(now, passenger.getId(), passenger.getCurrentLinkId(), passenger.getMode()));
				this.network.simEngine.getMobsim().getAgentCounter().incLost();
				this.network.simEngine.getMobsim().getAgentCounter().decLiving();				
			}
		}
		this.passengersWaitingForCars.clear();
		
		for (QVehicle veh : this.waitingList) {
			if (stuckAgents.contains(veh.getDriver().getId())) continue;
			else stuckAgents.add(veh.getDriver().getId());
			
			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			this.network.simEngine.getMobsim().getAgentCounter().incLost();
			this.network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		this.waitingList.clear();
	}

	void makeVehicleAvailableToNextDriver(QVehicle veh, double now) {
		
		/*
		 * Insert waiting passengers into vehicle.
		 */
		Id vehicleId = veh.getId();
		Set<MobsimAgent> passengers = this.passengersWaitingForCars.get(vehicleId);
		if (passengers != null) {
			// Copy set of passengers since otherwise we would modify it concurrently.
			List<MobsimAgent> passengersToHandle = new ArrayList<MobsimAgent>(passengers);
			for (MobsimAgent passenger : passengersToHandle) {
				this.unregisterPassengerAgentWaitingForCar(passenger, vehicleId);
				this.insertPassengerIntoVehicle(passenger, vehicleId, now);
			}
		}
		
		/*
		 * If the next driver is already waiting for the vehicle, check whether
		 * all passengers are also there. If not, the driver is not inserted
		 * into the vehicle and the vehicle does not depart.
		 */
		MobsimDriverAgent driverWaitingForCar = driversWaitingForCars.get(veh.getId());
		if (driverWaitingForCar != null) {
			MobsimDriverAgent driverWaitingForPassengers = driversWaitingForPassengers.get(driverWaitingForCar.getId());
			if (driverWaitingForPassengers != null) return;
		}
		
		/*
		 * If there is a driver waiting for its vehicle, insert it and let
		 * the vehicle depart.
		 */
		if (driverWaitingForCar != null) {
			// set agent as driver and then let the vehicle depart
			driversWaitingForCars.remove(veh.getId());
			veh.setDriver(driverWaitingForCar);
			this.letVehicleDepart(veh, now);
		}
	}

	@Override
	final void letVehicleDepart(QVehicle vehicle, double now) {
		MobsimDriverAgent driver = vehicle.getDriver();
		if (driver == null) throw new RuntimeException("Vehicle cannot depart without a driver!");
		
		EventsManager eventsManager = network.simEngine.getMobsim().getEventsManager();
		eventsManager.processEvent(eventsManager.getFactory().createPersonEntersVehicleEvent(now, driver.getId(), vehicle.getId()));
		this.addDepartingVehicle(vehicle);
	}

	/*
	 * If the vehicle is parked at the current link, insert the passenger,
	 * create an enter event and return true. Otherwise add the agent to
	 * the waiting list and return false.
	 */
	@Override
	final boolean insertPassengerIntoVehicle(MobsimAgent passenger, Id vehicleId, double now) {
		QVehicle vehicle = this.getParkedVehicle(vehicleId);
		
		// if the vehicle is not parked at the link, mark the agent as passenger waiting for vehicle
		if (vehicle == null) {
			registerPassengerAgentWaitingForCar(passenger, vehicleId);
			return false;
		} else {
			boolean added = vehicle.addPassenger((PassengerAgent) passenger);
			if (!added) {
				log.warn("Passenger " + passenger.getId().toString() + 
				" could not be inserted into vehicle " + vehicleId.toString() +
				" since there is no free seat available!");
				return false;
			}
			
			((PassengerAgent) passenger).setVehicle(vehicle);
			EventsManager eventsManager = network.simEngine.getMobsim().getEventsManager();
			eventsManager.processEvent(eventsManager.getFactory().createPersonEntersVehicleEvent(now, passenger.getId(), vehicle.getId()));
			// TODO: allow setting passenger's currentLinkId to null
			
			return true;
		}
	}

	final boolean addTransitToBuffer(final double now, final QVehicle veh) {
		if (veh.getDriver() instanceof TransitDriverAgent) {
			TransitDriverAgent driver = (TransitDriverAgent) veh.getDriver();
			while (true) {
				TransitStopFacility stop = driver.getNextTransitStop();
				if ((stop != null) && (stop.getLinkId().equals(getLink().getId()))) {
					double delay = driver.handleTransitStop(stop, now);
					if (delay > 0.0) {
						veh.setEarliestLinkExitTime(now + delay);
						// add it to the stop queue, can do this as the waitQueue is also non-blocking anyway
						transitVehicleStopQueue.add(veh);
						return true;
					}
				} else {
					return false;
				}
			}
		}
		return false;
	}

	QVehicle getVehicle(Id vehicleId) {
		QVehicle ret = this.parkedVehicles.get(vehicleId);
		return ret;
	}

	@Override
	public final Collection<MobsimVehicle> getAllVehicles() {
		Collection<MobsimVehicle> vehicles = this.getAllNonParkedVehicles();
		vehicles.addAll(this.parkedVehicles.values());
		return vehicles;
	}

	@Override
	public final Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}

	/*package*/ void setNetElementActivator(NetElementActivator qSimEngineRunner) {
		this.netElementActivator = qSimEngineRunner;
	}

	@Override
	/*package*/ void registerDriverAgentWaitingForCar(MobsimDriverAgent agent) {
		Id vehicleId = agent.getPlannedVehicleId() ;
		driversWaitingForCars.put(vehicleId, agent);
	}

	@Override
	/*package*/ void registerDriverAgentWaitingForPassengers(MobsimDriverAgent agent) {
		driversWaitingForPassengers.put(agent.getId(), agent);
	}

	@Override
	/*package*/ MobsimAgent unregisterDriverAgentWaitingForPassengers(Id agentId) {
		return driversWaitingForPassengers.remove(agentId);
	}
	
	@Override
	/*package*/ void registerPassengerAgentWaitingForCar(MobsimAgent agent, Id vehicleId) {
		Set<MobsimAgent> passengers = passengersWaitingForCars.get(vehicleId);
		if (passengers == null) {
			passengers = new LinkedHashSet<MobsimAgent>();
			passengersWaitingForCars.put(vehicleId, passengers);
		}
		passengers.add(agent);
	}
	
	@Override
	/*package*/ MobsimAgent unregisterPassengerAgentWaitingForCar(MobsimAgent agent, Id vehicleId) {
		Set<MobsimAgent> passengers = passengersWaitingForCars.get(vehicleId);
		if (passengers != null && passengers.remove(agent)) return agent;
		else return null;
	}
	
	@Override
	/*package*/ Set<MobsimAgent> getAgentsWaitingForCar(Id vehicleId) {
		Set<MobsimAgent> set = passengersWaitingForCars.get(vehicleId);
		if (set != null) return Collections.unmodifiableSet(set);
		else return null;
	}
}
