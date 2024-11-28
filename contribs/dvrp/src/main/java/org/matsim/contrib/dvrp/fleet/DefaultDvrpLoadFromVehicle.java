package org.matsim.contrib.dvrp.fleet;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.dvrp_load.IntegerLoadType;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

public class DefaultDvrpLoadFromVehicle implements DvrpLoadFromVehicle {

	public static final String VEHICLES_ATTR_CAPACITY_TYPE = "dvrp:capacityType";
	public static final String VEHICLES_ATTR_CAPACITY_VALUE = "dvrp:capacityValue";

	private final DvrpLoadSerializer dvrpLoadSerializer;
	private final IntegerLoadType fallBackIntegerLoadType;

	public DefaultDvrpLoadFromVehicle(DvrpLoadSerializer dvrpLoadSerializer) {
		this(dvrpLoadSerializer, null);
	}

	public DefaultDvrpLoadFromVehicle(DvrpLoadSerializer dvrpLoadSerializer, IntegerLoadType fallBackIntegerLoadType) {
		this.dvrpLoadSerializer = dvrpLoadSerializer;
		this.fallBackIntegerLoadType = fallBackIntegerLoadType;
	}

	@Override
	public DvrpLoad getLoad(Vehicle vehicle) {
		Map<String, Object> attributes = new HashMap<>(vehicle.getAttributes().getAsMap());
		attributes.putAll(vehicle.getAttributes().getAsMap());
		if(attributes.containsKey(VEHICLES_ATTR_CAPACITY_TYPE) && attributes.containsKey(VEHICLES_ATTR_CAPACITY_VALUE)) {
			Id<DvrpLoadType> dvrpLoadTypeId = Id.create((String) attributes.get(VEHICLES_ATTR_CAPACITY_TYPE), DvrpLoadType.class);
			return this.dvrpLoadSerializer.deSerialize((String) attributes.get(VEHICLES_ATTR_CAPACITY_VALUE), dvrpLoadTypeId);
		} else {
			if(this.fallBackIntegerLoadType != null) {
				return this.fallBackIntegerLoadType.fromInt(vehicle.getType().getCapacity().getSeats());
			}
			throw new IllegalStateException(String.format("Attributes %s and %s not present in vehicle %s or its type %s and no fallback interpretation for the number of seats", VEHICLES_ATTR_CAPACITY_TYPE, VEHICLES_ATTR_CAPACITY_VALUE, vehicle.getId().toString(), vehicle.getType().getId().toString()));
		}
	}
}
