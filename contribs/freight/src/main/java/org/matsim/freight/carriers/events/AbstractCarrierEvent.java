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

package org.matsim.freight.carriers.events;

import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.Carrier;
import org.matsim.vehicles.Vehicle;

/**
 * A general freight event contains the information (= {@link Id}) of the
 * 	- {@link Carrier}
 * 	- {@link Vehicle}
 * 	- the location (= {@link Link})
 * 	belonging to it.
 * <p>
 * 	Instead of adding it to all different freight events, this is consolidated in this abstract class
 *
 * @author Kai Martins-Turner (kturner)
 */
public abstract class AbstractCarrierEvent extends Event implements HasCarrierId, HasLinkId, HasVehicleId {

	private final Id<Carrier> carrierId;
	private final Id<Link> linkId;
	private final Id<Vehicle> vehicleId;

	public AbstractCarrierEvent(double time, Id<Carrier> carrierId, Id<Link> linkId, Id<Vehicle> vehicleId) {
		super(time);
		this.carrierId = carrierId;
		this.linkId = linkId;
		this.vehicleId = vehicleId;
	}

	/**
	 * @return id of the {@link Carrier}
	 */
	@Override public final Id<Carrier> getCarrierId() {
		return carrierId;
	}

	@Override public final Id<Link> getLinkId() {
		return linkId;
	}

	@Override public final Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	/**
	 * Adds the {@link Id<Carrier>} to the list of attributes.
	 * {@link Id<Vehicle>} and {@link Id<Link>} are handled by superclass {@link Event}
	 *
	 * @return The map of attributes
	 */
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_CARRIER_ID, carrierId.toString());
		return attr;
	}
}
