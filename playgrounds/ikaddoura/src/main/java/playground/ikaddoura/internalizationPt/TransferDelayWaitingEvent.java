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
 * Event to indicate that an agent entering or leaving a public vehicle delayed passengers waiting for that public vehicle.
 * @author ikaddoura
 */
public final class TransferDelayWaitingEvent extends Event {
	
	public static final String EVENT_TYPE = "TransferDelayWaitingEvent";
	public static final String ATTRIBUTE_PERSON = "causingAgent";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_AFFECTED_AGENTS = "numberOfAffectedAgents";
	public static final String ATTRIBUTE_DELAY = "delay";
	
	private final Id<Vehicle> vehicleId;
	private final Id<Person> personId;
	private final double affectedAgentUnits;
	private final double delay;

	public TransferDelayWaitingEvent(Id<Person> personId, Id<Vehicle> vehicleId, double time, double delayedPassengers, double externalDelay) {
		super(time);
		this.vehicleId = vehicleId;
		this.personId = personId;
		this.affectedAgentUnits = delayedPassengers;
		this.delay = externalDelay;
	}

	public Id<Person> getCausingAgent() {
		return this.personId;
	}
	
	public Id<Vehicle> getVehicleId() {
		return this.vehicleId;
	}

	public double getAffectedAgentUnits() {
		return this.affectedAgentUnits;
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
		attrs.put(ATTRIBUTE_PERSON, this.personId.toString());
		attrs.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		attrs.put(ATTRIBUTE_AFFECTED_AGENTS, Double.toString(this.affectedAgentUnits));
		attrs.put(ATTRIBUTE_DELAY, Double.toString(this.delay));
		return attrs;
	}
	
}
