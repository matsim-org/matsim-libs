package ch.sbb.matsim.contrib.railsim.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Event that occurs when a train was re-routed.
 */
public class RailsimDetourEvent extends Event implements HasVehicleId {

	public static final String EVENT_TYPE = "railsimDetourEvent";

	private final Id<Vehicle> vehicleId;
	private final Id<Link> entry;
	private final Id<Link> exit;
	private final List<Id<Link>> detour;

	public RailsimDetourEvent(double time, Id<Vehicle> vehicleId, Id<Link> entry, Id<Link> exit, List<Id<Link>> detour) {
		super(time);
		this.vehicleId = vehicleId;
		this.entry = entry;
		this.exit = exit;
		this.detour = detour;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}


	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();

		attributes.put(HasVehicleId.ATTRIBUTE_VEHICLE, vehicleId.toString());
		attributes.put("entry", entry.toString());
		attributes.put("exit", exit.toString());
		attributes.put("detour", detour.stream().map(Object::toString).collect(Collectors.joining(",")));

		return attributes;
	}
}
