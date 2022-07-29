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
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.Tour;

public final class LSPTourEndEvent extends Event {

	public static final String EVENT_TYPE = "LspFreightTourEnded";
	public static final String ATTRIBUTE_VEHICLE = FreightEventAttributes.ATTRIBUTE_VEHICLE_ID;
	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_CARRIER = "carrier";
	public static final String ATTRIBUTE_TOUR = "tour";	
	
	
	private final Id<Carrier> carrierId;
	private final Tour tour;
	private final CarrierVehicle vehicle;
	
	public LSPTourEndEvent(Id<Carrier>  carrierId, Tour tour, double time, CarrierVehicle vehicle) {
		super(time);
		this.carrierId = carrierId;
		this.tour = tour;
		this.vehicle = vehicle;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Id<Carrier> getCarrierId() {
		return carrierId;
	}

	public Tour getTour() {
		return tour;
	}
	
	public CarrierVehicle getVehicle() {
		return vehicle;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_VEHICLE, this.vehicle.getId().toString() );
		attr.put(ATTRIBUTE_LINK, this.tour.getStartLinkId().toString());
		attr.put(ATTRIBUTE_CARRIER, this.carrierId.toString());
		attr.put(ATTRIBUTE_TOUR, this.tour.toString());
		return attr;
	}
}
