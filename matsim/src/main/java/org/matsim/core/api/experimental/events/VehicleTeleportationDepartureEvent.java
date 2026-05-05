package org.matsim.core.api.experimental.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.events.HasPersonId;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Signals that a vehicular trip is being teleported rather than physically simulated on the network. The vehicle has
 * been confirmed available on the departure link and removed from it.
 * <p>
 * This is the departure-side counterpart to {@link TeleportationArrivalEvent}. It is fired at departure time
 * ({@code now}) — the same timestamp as the preceding {@link org.matsim.api.core.v01.events.PersonDepartureEvent}.
 * <p>
 * Use case: downstream handlers that need to know when a vehicle-based teleported trip starts (e.g. for rental
 * tracking, energy consumption), since {@link org.matsim.api.core.v01.events.PersonEntersVehicleEvent} is not fired for
 * teleported trips.
 */
public final class VehicleTeleportationDepartureEvent extends Event implements HasPersonId, HasVehicleId, HasLinkId {

	public static final String EVENT_TYPE = "vehicle teleport departure";

	public static final String ATTRIBUTE_MODE = "mode";

	private final Id<Person> personId;
	private final Id<Vehicle> vehicleId;
	private final Id<Link> linkId;
	private final String mode;

	public VehicleTeleportationDepartureEvent(double time, Id<Person> personId, Id<Vehicle> vehicleId,
			Id<Link> linkId, String mode) {
		super(time);
		this.personId = personId;
		this.vehicleId = vehicleId;
		this.linkId = linkId;
		this.mode = mode;
	}

	@Override
	public Id<Person> getPersonId() {
		return personId;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

	public String getMode() {
		return mode;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();
		attributes.put(ATTRIBUTE_MODE, mode);
		return attributes;
	}
}
