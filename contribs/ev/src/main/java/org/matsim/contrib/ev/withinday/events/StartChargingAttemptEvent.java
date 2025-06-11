package org.matsim.contrib.ev.withinday.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasPersonId;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.vehicles.Vehicle;

/**
 * Generated when an agent starts a charging attempt, i.e., he has arrived at
 * the charger and now starts to queue / plug the vehicle.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class StartChargingAttemptEvent extends Event implements HasPersonId, HasVehicleId {
	static public final String EVENT_TYPE = "start charging attempt";

	private final Id<Person> personId;
	private final Id<Vehicle> vehicleId;
	private final Id<Charger> chargerId;

	private final boolean isEnroute;
	private final boolean isSpontaneous;

	private final int attemptIndex;
	private final int processIndex;

	private final double duration;

	public StartChargingAttemptEvent(double time, Id<Person> personId, Id<Vehicle> vehicleId, Id<Charger> chargerId,
			int attemptIndex, int processIndex, boolean isEnroute, boolean isSpontaneous, double duration) {
		super(time);

		this.personId = personId;
		this.vehicleId = vehicleId;
		this.chargerId = chargerId;
		this.isEnroute = isEnroute;
		this.isSpontaneous = isSpontaneous;
		this.attemptIndex = attemptIndex;
		this.processIndex = processIndex;
		this.duration = duration;
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

	public Id<Charger> getChargerId() {
		return chargerId;
	}

	public boolean isEnroute() {
		return isEnroute;
	}

	public boolean isSpontaneous() {
		return isSpontaneous;
	}

	public int getAttemptIndex() {
		return attemptIndex;
	}

	public int getProcessIndex() {
		return processIndex;
	}

	public double getDuration() {
		return duration;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();
		attributes.put("charger", chargerId.toString());
		attributes.put("enroute", String.valueOf(isEnroute));
		attributes.put("spontaneous", String.valueOf(isSpontaneous));
		attributes.put("attemptIndex", String.valueOf(attemptIndex));
		attributes.put("processIndex", String.valueOf(processIndex));

		if (duration > 0.0) {
			attributes.put("duration", String.valueOf(duration));
		}

		return attributes;
	}
}
