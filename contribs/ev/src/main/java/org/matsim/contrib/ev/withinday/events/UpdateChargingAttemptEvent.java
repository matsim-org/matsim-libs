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
 * Generated when an agent updates a charging attempt at the beginning of the
 * charging process. This means that even before starting the first attempt, the
 * selected charger or other characteristics are updated on-the-fly compared to
 * the initially planned configuration.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class UpdateChargingAttemptEvent extends Event implements HasPersonId, HasVehicleId {
	static public final String EVENT_TYPE = "update charging attempt";

	private final Id<Person> personId;
	private final Id<Vehicle> vehicleId;
	private final Id<Charger> chargerId;

	private boolean isEnroute;
	private double duration;

	public UpdateChargingAttemptEvent(double time, Id<Person> personId, Id<Vehicle> vehicleId, Id<Charger> chargerId,
			boolean isEnroute, double duration) {
		super(time);

		this.personId = personId;
		this.vehicleId = vehicleId;
		this.chargerId = chargerId;
		this.isEnroute = isEnroute;
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

	public double getDuration() {
		return duration;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();
		attributes.put("charger", chargerId.toString());
		attributes.put("enroute", String.valueOf(isEnroute));

		if (duration > 0.0) {
			attributes.put("duration", String.valueOf(duration));
		}

		return attributes;
	}
}
