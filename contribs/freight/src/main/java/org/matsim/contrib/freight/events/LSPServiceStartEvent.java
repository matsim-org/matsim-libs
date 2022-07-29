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
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

import static org.matsim.contrib.freight.events.FreightEventAttributes.*;

public final class LSPServiceStartEvent extends Event implements HasLinkId, HasVehicleId, HasCarrierId {

	public static final String EVENT_TYPE = "LspServiceStarts";

	private final Id<CarrierService> serviceId;

	private final Id<Link> linkId;
	private final Id<Carrier> carrierId;
	private final Id<Vehicle> vehicleId;
	private final double serviceDuration;
	private final int capacityDemand;

	public LSPServiceStartEvent(Id<Carrier> carrierId, CarrierService service, double time, Id<Vehicle> vehicleId) {
		super(time);
		this.serviceId = service.getId();
		this.linkId = service.getLocationLinkId();
		this.carrierId = carrierId;
		this.vehicleId = vehicleId;
		this.serviceDuration = service.getServiceDuration();
		this.capacityDemand = service.getCapacityDemand();
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Id<CarrierService> getServiceId() {
		return serviceId;
	}

	@Override public Id<Link> getLinkId() {
		return linkId;
	}

	@Override public Id<Carrier> getCarrierId() {
		return carrierId;
	}

	@Override public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	public double getServiceDuration() {
		return serviceDuration;
	}

	public int getCapacityDemand() {
		return this.capacityDemand;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_SERVICE_ID, serviceId.toString());
		attr.put(ATTRIBUTE_CARRIER_ID, carrierId.toString());
		attr.put(ATTRIBUTE_SERVICE_DURATION, String.valueOf(serviceDuration));
		attr.put(ATTRIBUTE_CAPACITYDEMAND, String.valueOf(capacityDemand));
		return attr;
	}
}
