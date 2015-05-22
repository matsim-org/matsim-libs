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
 * Event to indicate that an agent entering or leaving a public vehicle delayed passengers in the public vehicle.
 * @author ikaddoura
 */
public final class TransferDelayInVehicleEvent extends Event {
	
	public static final String EVENT_TYPE = "TransferDelayInVehicleEvent";
	public static final String ATTRIBUTE_PERSON = "causingAgent";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_AFFECTED_AGENTS = "numberOfAffectedAgents";
	public static final String ATTRIBUTE_DELAY = "delay";
	
	private final Id<Vehicle> vehicleId;
	private final Id<Person> personId;
	private final int affectedAgents;
	private final double delay;

	public TransferDelayInVehicleEvent(Id<Person> personId, Id<Vehicle> vehicleId, double time, int delayedPassengers, double externalDelay) {
		super(time);
		this.vehicleId = vehicleId;
		this.personId = personId;
		this.affectedAgents = delayedPassengers;
		this.delay = externalDelay;
	}

	public Id<Person> getCausingAgent() {
		return this.personId;
	}
	
	public Id<Vehicle> getVehicleId() {
		return this.vehicleId;
	}

	public int getAffectedAgents() {
		return this.affectedAgents;
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
		attrs.put(ATTRIBUTE_AFFECTED_AGENTS, Integer.toString(this.affectedAgents));
		attrs.put(ATTRIBUTE_DELAY, Double.toString(this.delay));
		return attrs;
	}

}
