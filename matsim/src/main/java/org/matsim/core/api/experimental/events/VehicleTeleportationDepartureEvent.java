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
 * Signals that a vehicular trip is being teleported rather than physically simulated on the network.
 * <p>
 * This is the departure-side counterpart to {@link TeleportationArrivalEvent}. It is fired when the teleportation
 * actually starts — i.e. when the vehicle is confirmed available and removed from the departure link.
 * <p>
 * For immediate departures the event time equals the preceding
 * {@link org.matsim.api.core.v01.events.PersonDepartureEvent} time. For <em>deferred</em> departures (e.g. when FISS
 * defers because the vehicle is still in transit) the event time is later — it is the moment the vehicle arrives and the
 * agent can finally depart. In that case {@link #getDesiredDepartureTime()} returns the original
 * {@code PersonDepartureEvent} time, and the difference {@code getTime() - getDesiredDepartureTime()} is the vehicle
 * waiting time.
 * <p>
 * Use case: downstream handlers that need to know when a vehicle-based teleported trip starts (e.g. for rental
 * tracking, energy consumption), since {@link org.matsim.api.core.v01.events.PersonEntersVehicleEvent} is not fired for
 * teleported trips.
 */
public final class VehicleTeleportationDepartureEvent extends Event implements HasPersonId, HasVehicleId, HasLinkId {

	public static final String EVENT_TYPE = "vehicle teleport departure";

	public static final String ATTRIBUTE_MODE = "mode";
	public static final String ATTRIBUTE_DESIRED_DEPARTURE_TIME = "desiredDepartureTime";

	private final Id<Person> personId;
	private final Id<Vehicle> vehicleId;
	private final Id<Link> linkId;
	private final String mode;
	private final double desiredDepartureTime;

	/**
	 * Creates an event for a teleported departure where the agent may have waited for the vehicle.
	 *
	 * @param time                 the actual departure time (when the vehicle became available)
	 * @param personId             the departing person
	 * @param vehicleId            the vehicle being teleported
	 * @param linkId               the departure link
	 * @param mode                 the transport mode
	 * @param desiredDepartureTime when the agent originally wanted to depart (from {@code PersonDepartureEvent});
	 *                             equals {@code time} when no waiting occurred
	 */
	public VehicleTeleportationDepartureEvent(double time, Id<Person> personId, Id<Vehicle> vehicleId,
			Id<Link> linkId, String mode, double desiredDepartureTime) {
		super(time);
		this.personId = personId;
		this.vehicleId = vehicleId;
		this.linkId = linkId;
		this.mode = mode;
		this.desiredDepartureTime = desiredDepartureTime;
	}

	/**
	 * Backward-compatible constructor for immediate departures (no vehicle waiting).
	 */
	public VehicleTeleportationDepartureEvent(double time, Id<Person> personId, Id<Vehicle> vehicleId,
			Id<Link> linkId, String mode) {
		this(time, personId, vehicleId, linkId, mode, time);
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

	/**
	 * Returns the time when the agent originally wanted to depart. Equals {@link #getTime()} when the vehicle was
	 * immediately available. Less than {@link #getTime()} when the agent had to wait for the vehicle (deferred
	 * departure).
	 */
	public double getDesiredDepartureTime() {
		return desiredDepartureTime;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();
		attributes.put(ATTRIBUTE_MODE, mode);
		if (desiredDepartureTime != getTime()) {
			attributes.put(ATTRIBUTE_DESIRED_DEPARTURE_TIME, Double.toString(desiredDepartureTime));
		}
		return attributes;
	}
}
