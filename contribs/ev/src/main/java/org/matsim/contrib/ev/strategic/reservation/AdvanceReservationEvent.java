package org.matsim.contrib.ev.strategic.reservation;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasPersonId;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.vehicles.Vehicle;

public class AdvanceReservationEvent extends Event implements HasPersonId, HasVehicleId {
	static public final String EVENT_TYPE = "advance charger reservation";

	private final Id<Person> personId;
	private final Id<Vehicle> vehicleId;
	private final Id<Charger> chargerId;

	private double startTime;
	private double endTime;

	private boolean successful;

	public AdvanceReservationEvent(double time, Id<Person> personId, Id<Vehicle> vehicleId, Id<Charger> chargerId,
			double startTime, double endTime, boolean successful) {
		super(time);

		this.personId = personId;
		this.vehicleId = vehicleId;
		this.chargerId = chargerId;
		this.startTime = startTime;
		this.endTime = endTime;
		this.successful = successful;
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

	public double getStartTime() {
		return startTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public boolean getSuccessful() {
		return successful;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();

		attributes.put("charger", chargerId.toString());
		attributes.put("startTime", String.valueOf(startTime));
		attributes.put("endTime", String.valueOf(endTime));
		attributes.put("successful", String.valueOf(successful));

		return attributes;
	}
}
