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
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.api.internal.HasVehicleId;
import org.matsim.vehicles.Vehicle;

import static org.matsim.contrib.freight.events.FreightEventAttributes.*;

public final class LSPTourStartEvent extends Event implements HasPersonId, HasLinkId, HasVehicleId {

	public static final String EVENT_TYPE = "LspFreightTourStarts";

	private final Id<Link> linkId;
	private final Id<Carrier> carrierId;
	private final Id<Person> personId;
//	private final Tour tour;
	private final Id<Vehicle> vehicleId;

	//TODO: Public constructor or usage via creator? kmt' jun'22
//	public LSPTourStartEvent(ActivityEndEvent event, Id<Carrier>  carrierId, Id<Vehicle> vehicleId, Tour tour) {
	public LSPTourStartEvent(ActivityEndEvent event, Id<Carrier>  carrierId, Id<Vehicle> vehicleId) {
		super(event.getTime());
		this.linkId = event.getLinkId();
		this.carrierId = carrierId;
		this.personId = event.getPersonId();
//		this.tour = tour; 		//TODO: Wo we need the "Tour"-Object here? kmt, jun'22
		this.vehicleId = vehicleId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Id<Carrier> getCarrierId() {
		return carrierId;
	}

//	public Tour getTour() {
//		return tour;
//	}

	@Override
	public Id<Link> getLinkId() {
		return this.linkId;
	}

	@Override
	public Id<Person> getPersonId() {
		return this.personId;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		// person, link, vehicle done by superclass
		attr.put(ATTRIBUTE_CARRIER, this.carrierId.toString());
//		attr.put(ATTRIBUTE_TOUR, this.tour.toString());
		return attr;
	}
}
