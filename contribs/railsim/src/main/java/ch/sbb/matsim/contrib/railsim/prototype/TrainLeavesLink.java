package ch.sbb.matsim.contrib.railsim.prototype;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

/**
 * @author Ihab Kaddoura
 */
public class TrainLeavesLink extends Event implements HasLinkId, HasVehicleId {

	public static final String EVENT_TYPE = "train left link";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_LINK = "link";

	private final Id<Link> linkId;
	private final Id<Vehicle> vehicleId;

	public TrainLeavesLink(double time, Id<Link> linkId, Id<Vehicle> vehicleId) {
		super(time);
		this.linkId = linkId;
		this.vehicleId = vehicleId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Id<Link> getLinkId() {
		return linkId;
	}

	/**
	 * @return the vehicleId
	 */
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
