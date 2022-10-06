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
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.vehicles.Vehicle;

import static org.matsim.contrib.freight.events.FreightEventAttributes.ATTRIBUTE_TOUR_ID;

/**
 * An event, that informs when a Freight {@link Tour} has ended.
 * There are NO specific information of the tour given, because the {@link Tour} is determined by the {@link Vehicle} and its {@link Carrier}.
 *
 * @author Tilman Matteis  - creating it for the use in Logistics / LogisticServiceProviders (LSP)s
 * @author Kai Martins-Turner (kturner) - integrating and adapting it into/for the MATSim freight contrib
 */
public final class FreightTourEndEvent extends AbstractFreightEvent {

	public static final String EVENT_TYPE = "Freight tour ends";

	private final Id<Tour> tourId;

	public FreightTourEndEvent(double time, Id<Carrier>  carrierId, Id<Link> linkId, Id<Vehicle> vehicleId, Id<Tour> tourId) {
		super(time, carrierId, linkId, vehicleId);
		this.tourId = tourId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Id<Tour> getTourId() {
		return tourId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_TOUR_ID, this.tourId.toString());
		return attr;
	}
}
