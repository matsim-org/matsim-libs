package org.matsim.contrib.ev.withinday.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasPersonId;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Generated when a charging attempt is finished succesfully, i.e., the agent
 * managed to plug the vehicle and let it charge.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class FinishChargingAttemptEvent extends Event implements HasPersonId, HasVehicleId {
	static public final String EVENT_TYPE = "finish charging attempt";

	private final Id<Person> personId;
	private final Id<Vehicle> vehicleId;

	public FinishChargingAttemptEvent(double time, Id<Person> personId, Id<Vehicle> vehicleId) {
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
