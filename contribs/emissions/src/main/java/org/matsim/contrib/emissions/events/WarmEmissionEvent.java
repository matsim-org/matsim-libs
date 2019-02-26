/* *********************************************************************** *
 * project: org.matsim.*
 * WarmEmissionEventImpl.java
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
package org.matsim.contrib.emissions.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import java.util.Map;
import java.util.Map.Entry;


/**
 * @author benjamin
 *
 */
public final class WarmEmissionEvent extends Event {
    public final static String EVENT_TYPE = "warmEmissionEvent";
    public final static String ATTRIBUTE_LINK_ID = "linkId";
    public final static String ATTRIBUTE_VEHICLE_ID = "vehicleId";
    private final Id<Link> linkId;
	private final Id<Vehicle> vehicleId;
	private final Map<String, Double> warmEmissions;
	
	public WarmEmissionEvent(double time, Id<Link> linkId, Id<Vehicle> vehicleId, Map<String, Double> warmEmissions) {
        super(time);
        this.linkId = linkId;
		this.vehicleId = vehicleId;
		this.warmEmissions = warmEmissions;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}
	
	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}
	
	public Map<String, Double> getWarmEmissions() {
		return warmEmissions;
	}

	@Override
	public Map<String, String> getAttributes(){
		Map<String, String> attributes = super.getAttributes();
		attributes.put(ATTRIBUTE_LINK_ID, this.linkId.toString());
		attributes.put(ATTRIBUTE_VEHICLE_ID, this.vehicleId.toString());
		for(Entry<String, Double> entry : warmEmissions.entrySet()){
			String pollutant = entry.getKey();
			Double value = entry.getValue();
			attributes.put(pollutant, value.toString());
		}
		return attributes;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

}
