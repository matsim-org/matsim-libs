package org.matsim.contrib.dvrp.fleet;

import org.matsim.contrib.dvrp.fleet.dvrp_load.DefaultIntegerLoadType;
import org.matsim.contrib.dvrp.fleet.dvrp_load.IntegerLoadType;
import org.matsim.vehicles.Vehicle;

public class DefaultDvrpLoadFromVehicle implements DvrpLoadFromVehicle {

	private final IntegerLoadType integerLoadType;

	public DefaultDvrpLoadFromVehicle() {
		this(new DefaultIntegerLoadType());
	}

	public DefaultDvrpLoadFromVehicle(IntegerLoadType integerLoadType) {
		this.integerLoadType = integerLoadType;
	}

	@Override
	public DvrpLoad getLoad(Vehicle vehicle) {
		return integerLoadType.fromInt(vehicle.getType().getCapacity().getSeats());
	}
}
