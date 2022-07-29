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
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.vehicles.Vehicle;

import static org.matsim.contrib.freight.events.FreightEventAttributes.*;

public final class LSPServiceEndEvent extends AbstractFreightEvent{

	public static final String EVENT_TYPE = "Freight service ends";
	private final Id<CarrierService> serviceId;
	private final double serviceDuration;

	public LSPServiceEndEvent(double time, Id<Carrier> carrierId, CarrierService service, Id<Vehicle> vehicleId) {
		super(time, carrierId, service.getLocationLinkId(), vehicleId);
		this.serviceId = service.getId();
		this.serviceDuration = service.getServiceDuration();
	}

	@Override public String getEventType() {
		return EVENT_TYPE;
	}

	public Id<CarrierService> getServiceId() {
		return serviceId;
	}

	public double getServiceDuration() {
		return serviceDuration;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_SERVICE_ID, serviceId.toString());
		attr.put(ATTRIBUTE_SERVICE_DURATION, String.valueOf(serviceDuration));
		return attr;
	}
}
