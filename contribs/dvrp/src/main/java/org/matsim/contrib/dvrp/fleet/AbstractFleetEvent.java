package org.matsim.contrib.dvrp.fleet;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;

/**
 * Base class for fleet-related events in DVRP
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public abstract class AbstractFleetEvent extends Event {
	protected static final String MODE_ATTRIBUTE = "mode";
	protected static final String VEHICLE_ATTRIBUTE = "vehicle";

	private final String mode;
	private final Id<DvrpVehicle> vehicleId;

	protected AbstractFleetEvent(double time, String mode, Id<DvrpVehicle> vehicleId) {
		super(time);
		this.mode = mode;
		this.vehicleId = vehicleId;
	}

	public String getDvrpMode() {
		return mode;
	}

	public Id<DvrpVehicle> getDvrpVehicleId() {
		return vehicleId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();
		attributes.put(MODE_ATTRIBUTE, mode);
		attributes.put(VEHICLE_ATTRIBUTE, vehicleId.toString());
		return attributes;
	}
}
