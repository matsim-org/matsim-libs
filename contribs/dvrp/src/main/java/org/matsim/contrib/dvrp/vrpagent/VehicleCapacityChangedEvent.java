package org.matsim.contrib.dvrp.vrpagent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.contrib.dvrp.fleet.DvrpLoad;
import org.matsim.contrib.dvrp.fleet.DvrpLoadSerializer;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Map;

/**
 * @author Tarek Chouaki (tkchouaki)
 */
public class VehicleCapacityChangedEvent extends Event {

	public static final String EVENT_TYPE = "DvrpVehicleCapacityChanged";
	public static final String ATTRIBUTE_VEHICLE_ID = "vehicleId";
	public static final String ATTRIBUTE_NEW_CAPACITY = "newCapacity";
	public static final String ATTRIBUTE_CAPACITY_TYPE = "capacityType";

	private final Id<DvrpVehicle> vehicleId;
	private final DvrpLoad newVehicleCapacity;
	private final String serializedNewCapacity;
	private final String capacityTypeName;

	public VehicleCapacityChangedEvent(double time, Id<DvrpVehicle> vehicleId, DvrpLoad newVehicleCapacity, String serializedNewCapacity) {
		this(time, vehicleId, newVehicleCapacity, serializedNewCapacity, newVehicleCapacity.getType().getName());
	}

	public VehicleCapacityChangedEvent(double time, Id<DvrpVehicle> vehicleId, DvrpLoad newVehicleCapacity, String serializedNewCapacity, String capacityTypeName) {
		super(time);
		this.vehicleId = vehicleId;
		this.newVehicleCapacity = newVehicleCapacity;
		this.serializedNewCapacity = serializedNewCapacity;
		this.capacityTypeName = capacityTypeName;
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

	public String getCapacityTypeName() {
		return capacityTypeName;
	}


	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> atts = super.getAttributes();
		atts.put(ATTRIBUTE_VEHICLE_ID, this.vehicleId.toString());
		atts.put(ATTRIBUTE_NEW_CAPACITY, this.serializedNewCapacity);
		atts.put(ATTRIBUTE_CAPACITY_TYPE, this.capacityTypeName);
		return atts;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public static VehicleCapacityChangedEvent convert(GenericEvent event) {
		return convert(event, null);
	}

	public static VehicleCapacityChangedEvent convert(GenericEvent event, DvrpLoadSerializer dvrpLoadSerializer) {
		Map<String, String> attributes = event.getAttributes();
		Id<DvrpVehicle> vehicleId = Id.create(attributes.get(ATTRIBUTE_VEHICLE_ID), DvrpVehicle.class);
		String capacityTypeName = attributes.get(ATTRIBUTE_CAPACITY_TYPE);
		String serializedCapacity = attributes.get(ATTRIBUTE_NEW_CAPACITY);
		DvrpLoad dvrpLoad = null;
		if(dvrpLoadSerializer != null) {
			dvrpLoad = dvrpLoadSerializer.deSerialize(serializedCapacity, capacityTypeName);
		}
		return new VehicleCapacityChangedEvent(event.getTime(), vehicleId, dvrpLoad, serializedCapacity, capacityTypeName);
	}
}
