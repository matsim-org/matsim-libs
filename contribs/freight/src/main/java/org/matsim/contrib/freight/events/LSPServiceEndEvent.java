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
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;

public final class LSPServiceEndEvent extends Event {

	public static final String ATTRIBUTE_PERSON = "driver";
	public static final String EVENT_TYPE = "LspServiceEnds";
	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_ACTTYPE = "actType";
	public static final String ATTRIBUTE_SERVICE = "service";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_CARRIER = "carrier";
	
	private final CarrierService service;
	private final Id<Carrier> carrierId;
	private final Id<Person> driverId;
	private final CarrierVehicle vehicle;
	private final ActivityEndEvent event;
	
	public LSPServiceEndEvent(ActivityEndEvent event, Id<Carrier> carrierId, Id<Person> driverId, CarrierService service, double time, CarrierVehicle vehicle) {
		super(time);
		this.service = service;
		this.driverId = driverId;
		this.carrierId = carrierId;
		this.vehicle = vehicle;
		this.event = event;
	}

	public Id<Carrier> getCarrierId() {
		return carrierId;
	}

	public Id<Person> getDriverId() {
		return driverId;
	}

	public CarrierService getService() {
		return service;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public CarrierVehicle getVehicle() {
		return vehicle;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_PERSON, this.driverId.toString());
		if (service.getLocationLinkId() != null) {
			attr.put(ATTRIBUTE_LINK, service.getLocationLinkId().toString());
		}
		attr.put(ATTRIBUTE_ACTTYPE, event.getActType());
		attr.put(ATTRIBUTE_SERVICE, service.getId().toString());
		attr.put(ATTRIBUTE_VEHICLE, vehicle.getId().toString() );
		attr.put(ATTRIBUTE_CARRIER, carrierId.toString());
		return attr;
	}
}
