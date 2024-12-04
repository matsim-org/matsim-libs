package org.matsim.contrib.dvrp.vrpagent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.contrib.dvrp.fleet.dvrp_load.DvrpLoad;
import org.matsim.contrib.dvrp.fleet.dvrp_load.DvrpLoadSerializer;
import org.matsim.contrib.dvrp.fleet.dvrp_load.DvrpLoadType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Map;

/**
 * @author Tarek Chouaki (tkchouaki)
 */
public class VehicleCapacityChangedEvent extends Event {

	public static final String EVENT_TYPE = "DvrpVehicleCapacityChanged";
	public static final String ATTRIBUTE_DVRP_MODE = "dvrpMode";
	public static final String ATTRIBUTE_VEHICLE_ID = "vehicleId";
	public static final String ATTRIBUTE_NEW_CAPACITY = "newCapacity";
	public static final String ATTRIBUTE_CAPACITY_TYPE = "capacityType";
	private final String dvrpMode;

	private final Id<DvrpVehicle> vehicleId;
	private final DvrpLoad newVehicleCapacity;
	private final String serializedNewCapacity;
	private final Id<DvrpLoadType> capacityTypeId;

	public VehicleCapacityChangedEvent(double time, String dvrpMode, Id<DvrpVehicle> vehicleId, DvrpLoad newVehicleCapacity, String serializedNewCapacity) {
		this(time, dvrpMode, vehicleId, newVehicleCapacity, serializedNewCapacity, newVehicleCapacity.getType().getId());
	}

	public VehicleCapacityChangedEvent(double time, String dvrpMode, Id<DvrpVehicle> vehicleId, DvrpLoad newVehicleCapacity, String serializedNewCapacity, Id<DvrpLoadType> capacityTypeId) {
		super(time);
		this.vehicleId = vehicleId;
		this.newVehicleCapacity = newVehicleCapacity;
		this.serializedNewCapacity = serializedNewCapacity;
		this.capacityTypeId = capacityTypeId;
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

	public Id<DvrpLoadType> getCapacityTypeId() {
		return capacityTypeId;
	}


	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> atts = super.getAttributes();
		atts.put(ATTRIBUTE_DVRP_MODE, this.dvrpMode);
		atts.put(ATTRIBUTE_VEHICLE_ID, this.vehicleId.toString());
		atts.put(ATTRIBUTE_NEW_CAPACITY, this.serializedNewCapacity);
		atts.put(ATTRIBUTE_CAPACITY_TYPE, this.capacityTypeId.toString());
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
		String dvrpMode = attributes.get(ATTRIBUTE_DVRP_MODE);
		Id<DvrpVehicle> vehicleId = Id.create(attributes.get(ATTRIBUTE_VEHICLE_ID), DvrpVehicle.class);
		Id<DvrpLoadType> capacityTypeId = Id.create(attributes.get(ATTRIBUTE_CAPACITY_TYPE), DvrpLoadType.class);
		String serializedCapacity = attributes.get(ATTRIBUTE_NEW_CAPACITY);
		DvrpLoad dvrpLoad = null;
		if(dvrpLoadSerializer != null) {
			dvrpLoad = dvrpLoadSerializer.deSerialize(serializedCapacity, capacityTypeId);
		}
		return new VehicleCapacityChangedEvent(event.getTime(), dvrpMode, vehicleId, dvrpLoad, serializedCapacity, capacityTypeId);
	}
}
