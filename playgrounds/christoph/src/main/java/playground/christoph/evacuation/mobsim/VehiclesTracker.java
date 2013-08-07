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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.withinday.mobsim.MobsimDataProvider;

/**
 * Class that tracks vehicles.
 * 
 * @author cdobler
 */
public class VehiclesTracker implements LinkEnterEventHandler, LinkLeaveEventHandler, 
		AgentDepartureEventHandler, AgentArrivalEventHandler {
	
	private static final Logger log = Logger.getLogger(VehiclesTracker.class);
	
	private final MobsimDataProvider mobsimDataProvider;
	
	// currently active drivers
	private final Set<Id> drivers;
	
	/*
	 * Reserved capacities for passengers planned to enter. So far, we reserve seats only on 
	 * link level, meaning we assume that an agent which is picked up reserves a seat when the 
	 * picking up vehicle in on the same link.
	 */
	private final Map<Id, AtomicInteger> reservedCapacities;
	
	public VehiclesTracker(MobsimDataProvider mobsimDataProvider) {
		
		this.mobsimDataProvider = mobsimDataProvider;
		
		this.drivers = new HashSet<Id>();
		this.reservedCapacities = new ConcurrentHashMap<Id, AtomicInteger>();
	}
	
	public int getFreeVehicleCapacity(Id vehicleId) {
		
		QVehicle vehicle = (QVehicle) this.mobsimDataProvider.getVehicle(vehicleId);
		
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
			reservedCapacities.put(event.getVehicleId(), new AtomicInteger(0));
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		boolean isDriver = drivers.contains(event.getPersonId());
		if (isDriver) {
			AtomicInteger reservedCapacity = reservedCapacities.get(event.getVehicleId());
			if (reservedCapacity != null && reservedCapacity.get() > 0) {
				log.warn("Found reserved capacity of " + reservedCapacity.get() + " for vehicle " +
						event.getVehicleId().toString() + ". Expected this to be 0.");
			}
		}
	}
	
	@Override
	public void reset(int iteration) {		
		this.drivers.clear();
		this.reservedCapacities.clear();
	}

}