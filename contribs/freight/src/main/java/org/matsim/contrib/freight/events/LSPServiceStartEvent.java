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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;

import java.util.Map;

public final class LSPServiceStartEvent extends Event{

	public static final String EVENT_TYPE = "LspServiceStarts";

	private final CarrierService service;
	private final Id<Carrier> carrierId;
	private final CarrierVehicle vehicle;
	private final ActivityStartEvent event;
	
	public LSPServiceStartEvent(ActivityStartEvent event, Id<Carrier> carrierId, CarrierService service, double time, CarrierVehicle vehicle) {
		super(time);
		this.carrierId = carrierId;
		this.service = service;
		this.vehicle = vehicle;
		this.event = event;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public CarrierService getService() {
		return service;
	}

	public Id<Carrier> getCarrierId() {
		return carrierId;
	}

	public CarrierVehicle getVehicle() {
		return vehicle;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		if (service.getLocationLinkId() != null) {
			attr.put(FreightEventAttributes.ATTRIBUTE_LINK, service.getLocationLinkId().toString());
		}
		attr.put(FreightEventAttributes.ATTRIBUTE_ACTTYPE, event.getActType());
		attr.put(FreightEventAttributes.ATTRIBUTE_SERVICE, service.getId().toString());
		attr.put(FreightEventAttributes.ATTRIBUTE_VEHICLE, vehicle.getId().toString() );
		attr.put(FreightEventAttributes.ATTRIBUTE_CARRIER, carrierId.toString());
		return attr;
	}
	
}
