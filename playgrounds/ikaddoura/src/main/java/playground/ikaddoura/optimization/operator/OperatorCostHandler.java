/* *********************************************************************** *
 * project: org.matsim.*
 * TransitEventHandler.java
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
package playground.ikaddoura.optimization.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.vehicles.Vehicle;

/**
 * @author ikaddoura
 *
 */
public class OperatorCostHandler implements TransitDriverStartsEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler,
PersonArrivalEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	
	private Network network;
	private double vehicleKm;
	private double operatingHours_excludingSlackTimes;
	
	private Map<Id<Person>, Double> ptDriverId2firstDepartureTime = new HashMap<>();
	private Map<Id<Person>, Double> ptDriverId2lastArrivalTime = new HashMap<>();
	private Map<Id<Person>, Double> ptDriverId2lastRouteStartTime = new HashMap<>();
	
	private final List<Id<Person>> ptDriverIDs = new ArrayList<>();
	private final List<Id<Vehicle>> ptVehicleIDs = new ArrayList<>();
	
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	
	public OperatorCostHandler(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		this.ptDriverId2firstDepartureTime.clear();
		this.ptDriverId2lastArrivalTime.clear();
		this.ptDriverId2lastRouteStartTime.clear();
		this.ptDriverIDs.clear();
		this.ptVehicleIDs.clear();
		this.vehicleKm = 0.0;
		this.operatingHours_excludingSlackTimes = 0.0;
		this.delegate.reset(iteration);

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
	public void handleEvent(LinkLeaveEvent event) {
		
		if (ptDriverIDs.contains(delegate.getDriverOfVehicle(event.getVehicleId()))){
			this.vehicleKm = this.vehicleKm + network.getLinks().get(event.getLinkId()).getLength() / 1000.;
		}
	}
	
	public List<Id<Vehicle>> getVehicleIDs() {
		return this.ptVehicleIDs;
	}
	
	public double getVehicleKm() {
		return this.vehicleKm;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		
		if (ptDriverIDs.contains(event.getPersonId())){
			
			if (this.ptDriverId2firstDepartureTime.containsKey(event.getPersonId())){
				if (event.getTime() < this.ptDriverId2firstDepartureTime.get(event.getPersonId())){
					this.ptDriverId2firstDepartureTime.put(event.getPersonId(), event.getTime());
				}
				else {
					// not the first departure time of this public vehicle
				}
			}
			
			else {
				this.ptDriverId2firstDepartureTime.put(event.getPersonId(), event.getTime());
			}
			
			// for the operating times without slack times
			this.ptDriverId2lastRouteStartTime.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (ptDriverIDs.contains(event.getPersonId())){
			if (this.ptDriverId2lastArrivalTime.containsKey(event.getPersonId())){
				if (event.getTime() > this.ptDriverId2lastArrivalTime.get(event.getPersonId())){
					this.ptDriverId2lastArrivalTime.put(event.getPersonId(), event.getTime());
				}
				else {
					// not the last arrival time of this public vehicle
				}
			}
			else {
				this.ptDriverId2lastArrivalTime.put(event.getPersonId(), event.getTime());
			}
			
			// for the operating times without slack times
			double routeOperatingTime = event.getTime() - this.ptDriverId2lastRouteStartTime.get(event.getPersonId());
			this.operatingHours_excludingSlackTimes = this.operatingHours_excludingSlackTimes + routeOperatingTime;
		}
	}
	
	public double getVehicleHours_includingSlackTimes() {
		double vehicleSeconds = 0.;
		for (Id<Person> id : this.ptDriverId2firstDepartureTime.keySet()){
			vehicleSeconds = vehicleSeconds + ((this.ptDriverId2lastArrivalTime.get(id) - this.ptDriverId2firstDepartureTime.get(id)));
		}
		return vehicleSeconds / 3600.0;
	}

	public double getOperatingHours_excludingSlackTimes() {
		return operatingHours_excludingSlackTimes / 3600.0;
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);		
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		delegate.handleEvent(event);		
	}

}
