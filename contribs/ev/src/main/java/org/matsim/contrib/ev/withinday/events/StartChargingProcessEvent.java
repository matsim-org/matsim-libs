package org.matsim.contrib.ev.withinday.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasPersonId;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Generated when an agent starts a charging process, i.e., he makes the
 * decision at which charger to perform a charging attempt.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class StartChargingProcessEvent extends Event implements HasPersonId, HasVehicleId {
	static public final String EVENT_TYPE = "start charging process";

	private final Id<Person> personId;
	private final Id<Vehicle> vehicleId;
	private final int processIndex;

	public StartChargingProcessEvent(double time, Id<Person> personId, Id<Vehicle> vehicleId, int processIndex) {
		super(time);

		this.personId = personId;
		this.vehicleId = vehicleId;
		this.processIndex = processIndex;
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

	public int getProcessIndex() {
		return processIndex;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();
		attributes.put("processIndex", String.valueOf(processIndex));
		return attributes;
	}
}
