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
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.Tour;

public final class LSPTourStartEvent extends Event{

	public static final String EVENT_TYPE = "freight tour started";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_CARRIER = "carrier";
	public static final String ATTRIBUTE_DRIVER = "driver";
	public static final String ATTRIBUTE_TOUR = "tour";	
	
	private Id<Carrier> carrierId;
	private Id<Person> driverId;
	private Tour tour;
	private CarrierVehicle vehicle;
	
	public LSPTourStartEvent(Id<Carrier>  carrierId, Id<Person> driverId, Tour tour, double time, CarrierVehicle vehicle) {
		super(time);
		this.carrierId = carrierId;
		this.driverId = driverId;
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

	public Id<Person> getDriverId() {
		return driverId;
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
		attr.put(ATTRIBUTE_DRIVER, this.driverId.toString());
		attr.put(ATTRIBUTE_TOUR, this.tour.toString());
		return attr;
	}
}
