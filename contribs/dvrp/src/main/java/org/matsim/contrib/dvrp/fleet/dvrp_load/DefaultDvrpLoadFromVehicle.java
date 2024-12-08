package org.matsim.contrib.dvrp.fleet.dvrp_load;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

/**
 * This default implementation of {@link DvrpLoadFromVehicle} allows to specify the capacities of {@link DvrpVehicle} in the {@link Vehicle} attributes and falls back to an {@link IntegerLoadType} if not found.
 * These attributes are {@value VEHICLES_ATTR_CAPACITY_TYPE} and {@value VEHICLES_ATTR_CAPACITY_VALUE}. A {@link DvrpLoadSerializer} is used to interpret them.
 * It can be used without a fallback IntegerLoadType, in this case an exception is fired when the necessary attributes are not found.
 * @author Tarek Chouaki (tkchouaki)
 */
public class DefaultDvrpLoadFromVehicle implements DvrpLoadFromVehicle {

	public static final String VEHICLES_ATTR_CAPACITY_TYPE = "dvrp:capacityType";
	public static final String VEHICLES_ATTR_CAPACITY_VALUE = "dvrp:capacityValue";

	private final DvrpLoadSerializer dvrpLoadSerializer;
	private final IntegerLoadType fallBackIntegerLoadType;

	@SuppressWarnings("unused")
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
