package org.matsim.contrib.ev.withinday.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasPersonId;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Generated when a charging process is aborted, for instance, because the agent
 * has already tried to many chargers that were all occupied.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class AbortChargingProcessEvent extends Event implements HasPersonId, HasVehicleId {
	static public final String EVENT_TYPE = "abort charging process";

	private final Id<Person> personId;
	private final Id<Vehicle> vehicleId;

	public AbortChargingProcessEvent(double time, Id<Person> personId, Id<Vehicle> vehicleId) {
		super(time);

		this.personId = personId;
		this.vehicleId = vehicleId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Id<Person> getPersonId() {
		return personId;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}
}
