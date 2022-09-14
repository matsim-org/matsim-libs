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
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

/**
 * An event, that informs when a Freight {@link Tour} has started.
 * There are NO specific information of the tour given, because the {@link Tour} is determined by the {@link Vehicle} and its {@link Carrier}.
 *
 * @author Tilman Matteis  - creating it for the use in Logistics / LogisticServiceProviders (LSP)s
 * @author Kai Martins-Turner (kturner) - integrating and adapting it into/for the MATSim freight contrib
 */
public final class FreightTourStartEvent extends AbstractFreightEvent {

	public static final String EVENT_TYPE = "Freight tour starts";

	public FreightTourStartEvent(double time, Id<Carrier>  carrierId, Id<Link> linkId, Id<Vehicle> vehicleId) {
		super(time, carrierId, linkId, vehicleId);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		return super.getAttributes();
	}
}
