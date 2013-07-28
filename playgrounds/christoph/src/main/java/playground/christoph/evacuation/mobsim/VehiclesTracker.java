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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDeparture;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureWriter;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

/**
 * Class that tracks vehicles.
 * 
 * @author cdobler
 */
public class VehiclesTracker implements MobsimInitializedListener,
	LinkEnterEventHandler, LinkLeaveEventHandler, PersonLeavesVehicleEventHandler,
	AgentDepartureEventHandler, AgentArrivalEventHandler {
	
	// currently active drivers
	private final Set<Id> drivers;
	
	// vehicles
	private final Map<Id, MobsimVehicle> vehicles;
	
	// vehicles currently enroute on a given link
	private final Map<Id, List<Id>> enrouteVehiclesOnLink;
	
	/*
	 * Reserved capacities for passengers planned to enter.
	 * So far, we reserve seats only on link level, meaning
	 * we assume that an agent which is picked up reserves
	 * a seat when the picking up vehicle in on the same link.
	 */
	private final Map<Id, AtomicInteger> reservedCapacities;
	
	private final JointDepartureOrganizer jointDepartureOrganizer;
	private final JointDepartureWriter jointDepartureWriter;
	private MobsimTimer mobsimTimer;
	
	private int jointDeparturesCounter = 0;
		
	public VehiclesTracker(JointDepartureOrganizer jointDepartureOrganizer, JointDepartureWriter jointDepartureWriter) {
		
		this.jointDepartureOrganizer = jointDepartureOrganizer; 
		this.jointDepartureWriter = jointDepartureWriter;
		
		this.drivers = new HashSet<Id>();
		this.vehicles = new HashMap<Id, MobsimVehicle>();	
		this.enrouteVehiclesOnLink = new HashMap<Id, List<Id>>();
		this.reservedCapacities = new HashMap<Id, AtomicInteger>();
	}
	
	@Deprecated
	public JointDeparture createJointDeparture(Id linkId, Id vehicleId, Id driverId, 
			Collection<Id> passengerIds) {
		Id id = new IdImpl("jd" + jointDeparturesCounter++);
		JointDeparture jointDeparture = this.jointDepartureOrganizer.createJointDeparture(id, linkId, vehicleId, driverId, passengerIds);
		this.jointDepartureWriter.writeDeparture(this.mobsimTimer.getTimeOfDay(), jointDeparture);
		return jointDeparture;
	}
	
	public Id getVehicleLinkId(Id vehicleId) {
		return this.vehicles.get(vehicleId).getCurrentLink().getId();
	}
	
	public List<Id> getEnrouteVehiclesOnLink(Id linkId) {
		return this.enrouteVehiclesOnLink.get(linkId);
	}
	
	public int getFreeVehicleCapacity(Id vehicleId) {
		
		QVehicle vehicle = (QVehicle) this.vehicles.get(vehicleId);
		
		int passengerCapacity = vehicle.getPassengerCapacity();
		int passengers = vehicle.getPassengers().size();
		int freeCapacity = passengerCapacity - passengers;
		
		return freeCapacity;
	}
	
	public int getReservedVehicleCapacity(Id vehicleId) {
		AtomicInteger reserved = this.reservedCapacities.get(vehicleId);
		if (reserved == null) return 0;
		else return reserved.get();
	}
	
	public void reserveSeat(Id vehicleId) {
		AtomicInteger reserved = this.reservedCapacities.get(vehicleId);
		reserved.incrementAndGet();
	}
	
	public MobsimVehicle getVehicle(Id vehicleId) {
		return this.vehicles.get(vehicleId);
	}
	
	public MobsimDriverAgent getVehicleDriver(Id vehicleId) {
		return this.vehicles.get(vehicleId).getDriver();
	}
	
	public Collection<? extends PassengerAgent> getVehiclePassengers(Id vehicleId) {
		QVehicle vehicle = (QVehicle) this.vehicles.get(vehicleId);
		return vehicle.getPassengers();
	}
	
	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		QSim sim = (QSim) e.getQueueSimulation();
		this.mobsimTimer = sim.getSimTimer();
		
		// collect all vehicles
		for (NetsimLink netsimLink : sim.getNetsimNetwork().getNetsimLinks().values()) {
			for (MobsimVehicle mobsimVehicle : netsimLink.getAllVehicles()) {
				this.vehicles.put(mobsimVehicle.getId(), mobsimVehicle);
			}
		}
				
		// initialize some maps
		for (Link link : sim.getNetsimNetwork().getNetwork().getLinks().values()) {
			this.enrouteVehiclesOnLink.put(link.getId(), new ArrayList<Id>());
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		/*
		 * If a person leaves a vehicle, the vehicle has to be parked on
		 * the link and therefore has to be removed from the enroute list.
		 */
		MobsimVehicle vehicle = this.vehicles.get(event.getVehicleId());
		List<Id> vehicleIds = this.enrouteVehiclesOnLink.get(vehicle.getCurrentLink().getId());
		vehicleIds.remove(vehicle.getId());
	}
	
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (TransportMode.car.equals(event.getLegMode())) {
			this.drivers.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (TransportMode.car.equals(event.getLegMode())) {
			this.drivers.add(event.getPersonId());			
		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		boolean isDriver = drivers.contains(event.getPersonId());
		if (isDriver) {
			List<Id> vehicleIds = this.enrouteVehiclesOnLink.get(event.getLinkId());
			vehicleIds.add(event.getVehicleId());
			
			reservedCapacities.put(event.getVehicleId(), new AtomicInteger(0));
		}
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		boolean isDriver = drivers.contains(event.getPersonId());
		if (isDriver) {
			List<Id> vehicleIds = this.enrouteVehiclesOnLink.get(event.getLinkId());
			vehicleIds.remove(event.getVehicleId());
		}
	}
	
	@Override
	public void reset(int iteration) {		
		this.drivers.clear();
		this.vehicles.clear();		
		this.enrouteVehiclesOnLink.clear();
	}

}