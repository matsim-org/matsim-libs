/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionEventHotImpl.java
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
package playground.benjamin.events;

import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.EventImpl;

/**
 * @author benjamin
 *
 */
public class ColdEmissionEventImpl extends EventImpl implements ColdEmissionEvent{
	private Id linkId;
	private Id vehicleId;
	private Map<String, Double> coldEmissions;
	
	public ColdEmissionEventImpl(double time, Id linkId, Id vehicleId, Map<String, Double> coldEmissions) {
		super(time);
		this.linkId = linkId;
		this.vehicleId = vehicleId;
		this.coldEmissions = coldEmissions;
	}

	@Override
	public Id getLinkId() {
		return linkId;
	}
	
	@Override
	public Id getVehicleId() {
		return vehicleId;
	}
	
	@Override
	public Map<String, Double> getColdEmissions() {
		return coldEmissions;
	}

	@Override
	public Map<String, String> getAttributes(){
		Map<String, String> attributes = super.getAttributes();
		attributes.put(ATTRIBUTE_LINK_ID, this.linkId.toString());
		attributes.put(ATTRIBUTE_VEHICLE_ID, this.vehicleId.toString());
		for(Entry<String, Double> entry : coldEmissions.entrySet()){
			String pollutant = entry.getKey();
			String value = entry.getValue().toString();
			attributes.put(pollutant, value);
		}
		return attributes;
	}

	@Override
	public String getEventType() {
		return ColdEmissionEvent.EVENT_TYPE;
	}
}