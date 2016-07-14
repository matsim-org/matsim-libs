/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.noise.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * @author lkroeger, ikaddoura
 *
 */

public final class NoiseEventCaused extends Event {

	public final static String EVENT_TYPE = "noiseEventCaused";

	public final static String ATTRIBUTE_EMERGENCE_TIME = "emergenceTime";
	public final static String ATTRIBUTE_LINK_ID = "linkId";
	public final static String ATTRIBUTE_VEHICLE_ID = "causingVehicleId";
	public final static String ATTRIBUTE_AGENT_ID = "causingAgentId";
	public final static String ATTRIBUTE_AMOUNT_DOUBLE = "amount";
	
	private final double emergenceTime;
	private final Id<Person> causingAgentId;
	private final Id<Vehicle> causingVehicleId;
	private final double amount;
	private final Id<Link> linkId;
	
	public NoiseEventCaused(double time, double emergenceTime, Id<Person> causingAgentId , Id<Vehicle> causingVehicleId , double amount , Id<Link> linkId) {
		super(time);
		this.emergenceTime = emergenceTime;
		this.causingAgentId = causingAgentId;
		this.causingVehicleId = causingVehicleId;
		this.amount = amount;
		this.linkId = linkId;
	}
	
	public double getEmergenceTime() {
		return emergenceTime;
	}
	
	public Id<Link> getLinkId() {
		return linkId;
	}
	
	public Id<Vehicle> getCausingVehicleId() {
		return causingVehicleId;
	}
	
	public Id<Person> getCausingAgentId() {
		return causingAgentId;
	}
	
	public double getAmount() {
		return amount;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attrs = super.getAttributes();
		attrs.put(ATTRIBUTE_EMERGENCE_TIME, Double.toString(this.emergenceTime));
		attrs.put(ATTRIBUTE_AGENT_ID, this.causingAgentId.toString());
		attrs.put(ATTRIBUTE_VEHICLE_ID, this.causingVehicleId.toString());
		attrs.put(ATTRIBUTE_AMOUNT_DOUBLE, Double.toString(this.amount));
		attrs.put(ATTRIBUTE_LINK_ID , this.linkId.toString());
		return attrs;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
}