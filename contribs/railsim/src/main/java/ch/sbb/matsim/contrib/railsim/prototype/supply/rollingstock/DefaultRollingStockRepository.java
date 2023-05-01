package ch.sbb.matsim.contrib.railsim.prototype.supply.rollingstock;

import ch.sbb.matsim.contrib.railsim.prototype.supply.RailsimSupplyConfigGroup;
import ch.sbb.matsim.contrib.railsim.prototype.supply.RollingStockRepository;
import ch.sbb.matsim.contrib.railsim.prototype.supply.VehicleTypeInfo;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;

/**
 * Default implementation of the rolling stock repository
 *
 * @author Merlin Unterfinger
 */
public class DefaultRollingStockRepository implements RollingStockRepository {

	private final int passengerCapacity;
	private final double length;
	private final double maxVelocity;
	private final double maxAcceleration;
	private final double maxDeceleration;
	private final double turnaroundTime;

	public DefaultRollingStockRepository(Scenario scenario) {
		var config = ConfigUtils.addOrGetModule(scenario.getConfig(), RailsimSupplyConfigGroup.class);
		passengerCapacity = config.getVehiclePassengerCapacity();
		length = config.getVehicleLength();
		maxVelocity = config.getVehicleMaxVelocity();
		maxAcceleration = config.getVehicleMaxAcceleration();
		maxDeceleration = config.getVehicleMaxDeceleration();
		turnaroundTime = config.getVehicleTurnaroundTime();
	}

	@Override
	public VehicleTypeInfo getVehicleType(String vehicleTypeId) {
		var vehicleTypeInfo = new VehicleTypeInfo(vehicleTypeId, passengerCapacity, length, maxVelocity, turnaroundTime);
		RollingStockRepository.addRailsimAttributes(vehicleTypeInfo, maxAcceleration, maxDeceleration);
		return vehicleTypeInfo;
	}
}
