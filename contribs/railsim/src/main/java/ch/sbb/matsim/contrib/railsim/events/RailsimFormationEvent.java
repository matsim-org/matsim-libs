package ch.sbb.matsim.contrib.railsim.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.vehicles.Vehicle;

import java.util.List;
import java.util.Map;

/**
 *  Inform about a vehicle and its contained units.
 */
public class RailsimFormationEvent extends Event implements HasVehicleId {

	public static final String EVENT_TYPE = "railsimFormationEvent";

	private final Id<Vehicle> vehicleId;
	private final List<String> units;

	public RailsimFormationEvent(double time, Id<Vehicle> vehicleId, List<String> units) {
		super(time);
		this.vehicleId = vehicleId;
		this.units = units;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	public List<String> getUnits() {
		return units;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_VEHICLE, vehicleId.toString());
		attr.put("units", String.join(",", units));
		return attr;
	}

}
