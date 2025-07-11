/* *********************************************************************** *
 * project: org.matsim.*
 * Departure.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.pt.transitSchedule;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.ChainedDeparture;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import org.matsim.vehicles.Vehicle;

import java.util.List;


/**
 * Describes a single departure along a route in a transit line.
 *
 * @author mrieser
 */
public class DepartureImpl implements Departure {

	private final Id<Departure> id;
	private final double departureTime;
	private Id<Vehicle> vehicleId = null;
	private List<ChainedDeparture> chainedDepartures = List.of();
	private final Attributes attributes = new AttributesImpl();

	protected DepartureImpl(final Id<Departure> id, final double departureTime) {
		this.id = id;
		this.departureTime = departureTime;
	}

	@Override
	public Id<Departure> getId() {
		return this.id;
	}

	@Override
	public double getDepartureTime() {
		return this.departureTime;
	}

	@Override
	public void setVehicleId(final Id<Vehicle> vehicleId) {
		this.vehicleId = vehicleId;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return this.vehicleId;
	}

	@Override
	public Attributes getAttributes() {
		return this.attributes;
	}

	@Override
	public List<ChainedDeparture> getChainedDepartures() {
		return chainedDepartures;
	}

	@Override
	public void setChainedDepartures(final List<ChainedDeparture> chainedDepartures) {
		this.chainedDepartures = chainedDepartures;
	}

	@Override
	public String toString() {
		return "[DepartureImpl: id=" + this.id + ", depTime=" + this.departureTime + "]";
	}

}
