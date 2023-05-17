package ch.sbb.matsim.contrib.railsim.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

/**
 * Event thrown when the very end of a train left a link.
 */
public class RailsimTrainLeavesLinkEvent extends Event implements HasLinkId, HasVehicleId {

	public static final String EVENT_TYPE = "railsimTrainLeavesLinkEvent";

	private final Id<Link> linkId;
	private final Id<Vehicle> vehicleId;

	public RailsimTrainLeavesLinkEvent(double time,Id<Vehicle> vehicleId, Id<Link> linkId) {
		super(time);
		this.vehicleId = vehicleId;
		this.linkId = linkId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Id<Link> getLinkId() {
		return linkId;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		return attr;
	}
}
