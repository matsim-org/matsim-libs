package org.matsim.contrib.dvrp.vrpagent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.contrib.dvrp.fleet.DvrpLoad;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Map;

/**
 * @author Tarek Chouaki (tkchouaki)
 */
public class VehicleCapacityChangedEvent extends Event {

	public static final String EVENT_TYPE = "DvrpVehicleCapacityChange";
	public static final String ATTRIBUTE_VEHICLE_ID = "vehicleId";
	public static final String ATTRIBUTE_NEW_CAPACITY = "newCapacity";

	private final Id<DvrpVehicle> vehicleId;
	private final DvrpLoad newVehicleCapacity;

	public VehicleCapacityChangedEvent(double time, Id<DvrpVehicle> vehicleId, DvrpLoad newVehicleCapacity) {
		super(time);
		this.vehicleId = vehicleId;
		this.newVehicleCapacity = newVehicleCapacity;
	}

	public Id<DvrpVehicle> getVehicleId() {
		return vehicleId;
	}

	public DvrpLoad getNewVehicleCapacity() {
		return newVehicleCapacity;
	}


	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> atts = super.getAttributes();
		atts.put(ATTRIBUTE_VEHICLE_ID, this.vehicleId.toString());
		atts.put(ATTRIBUTE_NEW_CAPACITY, this.newVehicleCapacity.toString());
		return atts;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public static VehicleCapacityChangedEvent convert(GenericEvent event) {
		// TODO we need a serializer here to be able to build the newVehicleCapacity object
		return null;
	}
}
