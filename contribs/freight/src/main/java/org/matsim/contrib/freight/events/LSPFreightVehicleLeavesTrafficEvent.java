/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.vehicles.Vehicle;

public final class LSPFreightVehicleLeavesTrafficEvent extends Event{

	public static final String EVENT_TYPE = "freight vehicle leaves traffic";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";

	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_NETWORKMODE = "networkMode";
	public static final String ATTRIBUTE_DRIVER = "driver";
	public static final String ATTRIBUTE_POSITION = "relativePosition";
	public static final String ATTRIBUTE_CARRIER = "carrier";
	

	private Id<Person> driverId; 
	private Id<Link> linkId; 
	private Id<Vehicle> vehicleId;
	private CarrierVehicle carrierVehicle;
	private Id<Carrier> carrierId;
	private VehicleLeavesTrafficEvent event;
	
	public LSPFreightVehicleLeavesTrafficEvent(VehicleLeavesTrafficEvent event, Id<Carrier> carrierId, double time, Id<Person> driverId, Id<Link> linkId, Id<Vehicle> vehicleId, CarrierVehicle carrierVehicle) {
		super(time);
		this.driverId = driverId;
		this.linkId = linkId;
		this.vehicleId = vehicleId;
		this.carrierVehicle = carrierVehicle;
		this.carrierId = carrierId;
		this.event= event;
	}

	public CarrierVehicle getCarrierVehicle() {
		return carrierVehicle;
	}

	public Id<Carrier> getCarrierId() {
		return carrierId;
	}
	
	public Id<Person> getDriverId() {
		return driverId;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	@Override
	public String getEventType() {
		return "EVENT_TYPE";
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_DRIVER, this.driverId.toString());
		attr.put(ATTRIBUTE_LINK, (this.linkId == null ? null : this.linkId.toString()));
		if (this.vehicleId != null) {
			attr.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		}
		if (event.getNetworkMode() != null) {
			attr.put(ATTRIBUTE_NETWORKMODE, event.getNetworkMode());
		}
		attr.put(ATTRIBUTE_POSITION, Double.toString(event.getRelativePositionOnLink()));
		attr.put(ATTRIBUTE_CARRIER, this.carrierId.toString());
		return attr;
	}
}
