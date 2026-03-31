package org.matsim.api.core.v01.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

public class VehicleEndsParkingSearch extends Event implements HasPersonId, HasLinkId, HasVehicleId {
	public static final String EVENT_TYPE = "vehicle ends parking search";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";

	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_NETWORKMODE = "networkMode";
	public static final String ATTRIBUTE_DRIVER = "person";

	private final Id<Person> driverId;
	private final Id<Link> linkId;
	private final Id<Vehicle> vehicleId;
	private final String networkMode;

	public VehicleEndsParkingSearch(double time, Id<Person> driverId, Id<Link> linkId, Id<Vehicle> vehicleId, String networkMode) {
		super(time);
		this.driverId = driverId;
		this.linkId = linkId;
		this.vehicleId = vehicleId;
		this.networkMode = networkMode;
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
	public Id<Person> getPersonId() {
		return driverId;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		// personId, linkId, vehicleId handled by superclass
		if (this.networkMode != null) {
			attr.put(ATTRIBUTE_NETWORKMODE, networkMode);
		}
		return attr;
	}
}
