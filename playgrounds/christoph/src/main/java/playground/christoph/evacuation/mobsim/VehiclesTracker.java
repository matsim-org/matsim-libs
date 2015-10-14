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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.trafficmonitoring.TransportModeProvider;

/**
 * Class that tracks vehicles.
 * 
 * @author cdobler
 */
public class VehiclesTracker implements LinkEnterEventHandler, LinkLeaveEventHandler, 
		PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler,
		PersonEntersVehicleEventHandler {
	
	private static final Logger log = Logger.getLogger(VehiclesTracker.class);
	
	private final MobsimDataProvider mobsimDataProvider;
	private final TransportModeProvider transportModeProvider;
	
	/*
	 * Reserved capacities for passengers planned to enter. So far, we reserve seats only on 
	 * link level, meaning we assume that an agent which is picked up reserves a seat when the 
	 * picking up vehicle in on the same link.
	 */
	private final Map<Id, AtomicInteger> reservedCapacities;
	
	public VehiclesTracker(MobsimDataProvider mobsimDataProvider) {
		
		this.mobsimDataProvider = mobsimDataProvider;
		this.transportModeProvider = new TransportModeProvider();
		
		this.reservedCapacities = new ConcurrentHashMap<Id, AtomicInteger>();
	}
	
	// called from identifiers
	public int getFreeVehicleCapacity(Id vehicleId) {
		
		QVehicle vehicle = (QVehicle) this.mobsimDataProvider.getVehicle(vehicleId);
		
		int passengerCapacity = vehicle.getPassengerCapacity();
		int passengers = vehicle.getPassengers().size();
		int freeCapacity = passengerCapacity - passengers;
		
		return freeCapacity;
	}
	
	// called from identifiers
	public int getReservedVehicleCapacity(Id vehicleId) {
		AtomicInteger reserved = this.reservedCapacities.get(vehicleId);
		if (reserved == null) return 0;
		else return reserved.get();
	}
	
	// called from identifiers - this should only be done when there is free capacity left
	public void reserveSeat(Id vehicleId) {
		AtomicInteger reserved = this.reservedCapacities.get(vehicleId);
		reserved.incrementAndGet();
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.transportModeProvider.handleEvent(event);
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.transportModeProvider.handleEvent(event);
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		String transportMode = this.transportModeProvider.getTransportMode(event.getDriverId());
		boolean isDriver = transportMode != null && transportMode.equals(TransportMode.car);
		if (isDriver) {
			reservedCapacities.put(event.getVehicleId(), new AtomicInteger(0));
		}
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		String transportMode = this.transportModeProvider.getTransportMode(event.getPersonId());
		boolean isDriver = transportMode != null && transportMode.equals(TransportMode.car);
		if (!isDriver) {	// if it is not a driver, it is a passenger
			AtomicInteger reservedCapacity = reservedCapacities.get(event.getVehicleId());
			if (reservedCapacity != null) reservedCapacity.decrementAndGet();
		}
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		String transportMode = this.transportModeProvider.getTransportMode(event.getDriverId());
		boolean isDriver = transportMode != null && transportMode.equals(TransportMode.car);
		if (isDriver) {
			AtomicInteger reservedCapacity = reservedCapacities.get(event.getVehicleId());
			if (reservedCapacity != null && reservedCapacity.get() > 0) {
				log.warn("Found reserved capacity of " + reservedCapacity.get() + " for vehicle " +
						event.getVehicleId().toString() + " on link " + event.getLinkId().toString() + 
						" at time " + event.getTime() + ". Expected this to be 0.");
			}
		}
	}
	
	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.transportModeProvider.handleEvent(event);
	}
	
	@Override
	public void reset(int iteration) {		
		this.transportModeProvider.reset(iteration);
		this.reservedCapacities.clear();
	}

}