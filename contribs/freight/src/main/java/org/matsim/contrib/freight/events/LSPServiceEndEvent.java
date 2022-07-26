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
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.vehicles.Vehicle;

import static org.matsim.contrib.freight.events.FreightEventAttributes.*;

public final class LSPServiceEndEvent extends Event {

	public static final String EVENT_TYPE = "LspServiceEnds";
	private final Id<CarrierService> serviceId;
	private final Id<Link> linkId;
	private final Id<Carrier> carrierId;
	private final Id<Vehicle> vehicleId;
	private final double serviceDuration;

	public LSPServiceEndEvent(Id<Carrier> carrierId, CarrierService service, double time, CarrierVehicle vehicle) {
		super(time);
		this.serviceId = service.getId();
		this.linkId = service.getLocationLinkId();
		this.carrierId = carrierId;
		this.vehicleId = vehicle.getId();
		this.serviceDuration = service.getServiceDuration();
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Id<Carrier> getCarrierId() {
		return carrierId;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}


	public Id<CarrierService> getServiceId() {
		return serviceId;
	}

	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	public double getServiceDuration() {
		return serviceDuration;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_SERVICE, serviceId.toString());
		attr.put(ATTRIBUTE_LINK, linkId.toString());
		attr.put(ATTRIBUTE_CARRIER, carrierId.toString());
		attr.put(ATTRIBUTE_VEHICLE, vehicleId.toString());
		attr.put(ATTRIBUTE_SERVICEDURATION, String.valueOf(serviceDuration));
		return attr;
	}
}
