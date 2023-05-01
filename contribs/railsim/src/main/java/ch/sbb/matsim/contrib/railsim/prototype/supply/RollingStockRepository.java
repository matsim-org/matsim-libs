package ch.sbb.matsim.contrib.railsim.prototype.supply;

import ch.sbb.matsim.contrib.railsim.prototype.RailsimUtils;

/**
 * Rolling stock repository
 * <p>
 * Implement a repository to provide attributes (maximum velocity, passenger capacity, acceleration and deceleration) for the vehicle types.
 *
 * @author Merlin Unterfinger
 */
public interface RollingStockRepository {
	VehicleTypeInfo getVehicleType(String vehicleTypeId);

	static void addRailsimAttributes(VehicleTypeInfo vehicleTypeInfo, double maxAcceleration, double maxDeceleration) {
		var attributes = vehicleTypeInfo.getAttributes();
		attributes.put(RailsimUtils.VEHICLE_ATTRIBUTE_MAX_ACCELERATION, maxAcceleration);
		attributes.put(RailsimUtils.VEHICLE_ATTRIBUTE_MAX_DECELERATION, maxDeceleration);
	}
}
