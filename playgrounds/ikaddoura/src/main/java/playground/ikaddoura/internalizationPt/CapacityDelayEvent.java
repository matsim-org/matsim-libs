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
package playground.ikaddoura.internalizationPt;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Event to indicate that an agent who is in a public vehicle delayed another passenger who could not board the public vehicle.
 * @author ikaddoura
 */
public final class CapacityDelayEvent extends Event {
	
	public static final String EVENT_TYPE = "CapacityDelayEvent";
	public static final String ATTRIBUTE_PERSON = "causingAgent";
	public static final String ATTRIBUTE_AFFECTED_AGENT = "affectedAgent";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_DELAY = "delay";

	private final Id<Person> causingAgentId;
	private final Id<Person> affectedAgentId;
	private final Id<Vehicle> vehicleId;
	private final double delay;

	public CapacityDelayEvent(double time, Id<Person> causingAgentId, Id<Person> affectedAgentId, Id<Vehicle> vehicleId, double delay) {
		super(time);
		this.causingAgentId = causingAgentId;
		this.affectedAgentId = affectedAgentId;
		this.vehicleId = vehicleId;
		this.delay = delay;
	}
	
	public Id<Vehicle> getVehicleId() {
		return this.vehicleId;
	}

	public double getDelay() {
		return delay;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attrs = super.getAttributes();
		attrs.put(ATTRIBUTE_PERSON, this.causingAgentId.toString());
		attrs.put(ATTRIBUTE_AFFECTED_AGENT, this.affectedAgentId.toString());
		attrs.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		attrs.put(ATTRIBUTE_DELAY, Double.toString(this.delay));
		return attrs;
	}

	public Id<Person> getCausingAgentId() {
		return causingAgentId;
	}

	public Id<Person> getAffectedAgentId() {
		return affectedAgentId;
	}

}
