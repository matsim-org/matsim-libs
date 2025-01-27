package org.matsim.contrib.dvrp.vrpagent;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;

/**
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 */
public class VehicleCapacityChangedEvent extends Event {

	public static final String EVENT_TYPE = "DvrpVehicleCapacityChanged";
	public static final String ATTRIBUTE_DVRP_MODE = "dvrpMode";
	public static final String ATTRIBUTE_VEHICLE_ID = "vehicleId";
	public static final String ATTRIBUTE_NEW_CAPACITY = "newCapacity";
	private final String dvrpMode;

	private final Id<DvrpVehicle> vehicleId;
	private final DvrpLoad newVehicleCapacity;
	private final String serializedNewCapacity;

	public VehicleCapacityChangedEvent(double time, String dvrpMode, Id<DvrpVehicle> vehicleId, DvrpLoad newVehicleCapacity, String serializedNewCapacity) {
		super(time);
		this.vehicleId = vehicleId;
		this.newVehicleCapacity = newVehicleCapacity;
		this.serializedNewCapacity = serializedNewCapacity;
		this.dvrpMode = dvrpMode;
	}

	public Id<DvrpVehicle> getVehicleId() {
		return vehicleId;
	}

	public DvrpLoad getNewVehicleCapacity() {
		return newVehicleCapacity;
	}

	public String getSerializedNewCapacity() {
		return serializedNewCapacity;
	}


	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> atts = super.getAttributes();
		atts.put(ATTRIBUTE_DVRP_MODE, this.dvrpMode);
		atts.put(ATTRIBUTE_VEHICLE_ID, this.vehicleId.toString());
		atts.put(ATTRIBUTE_NEW_CAPACITY, this.serializedNewCapacity);
		return atts;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public static VehicleCapacityChangedEvent convert(GenericEvent event) {
		return convert(event, null);
	}

	public static VehicleCapacityChangedEvent convert(GenericEvent event, DvrpLoadType dvrpLoadType) {
		Map<String, String> attributes = event.getAttributes();
		String dvrpMode = attributes.get(ATTRIBUTE_DVRP_MODE);
		Id<DvrpVehicle> vehicleId = Id.create(attributes.get(ATTRIBUTE_VEHICLE_ID), DvrpVehicle.class);
		String serializedCapacity = attributes.get(ATTRIBUTE_NEW_CAPACITY);
		DvrpLoad dvrpLoad = null;
		if(dvrpLoadType != null) {
			dvrpLoad = dvrpLoadType.deserialize(serializedCapacity);
		}
		return new VehicleCapacityChangedEvent(event.getTime(), dvrpMode, vehicleId, dvrpLoad, serializedCapacity);
	}
}
