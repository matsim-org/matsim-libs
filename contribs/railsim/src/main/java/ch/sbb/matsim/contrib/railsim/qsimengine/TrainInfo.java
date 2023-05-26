package ch.sbb.matsim.contrib.railsim.qsimengine;


import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;

/**
 * Non-mutable static information for a single train.
 */
record TrainInfo(
	Id<VehicleType> id,
	double length,
	double maxVelocity,
	double acceleration,
	double deceleration,
	double maxDeceleration
) {

	public TrainInfo(VehicleType vehicle, RailsimConfigGroup config) {
		// TODO:
		this(
			vehicle.getId(),
			vehicle.getLength(),
			vehicle.getMaximumVelocity(),
			RailsimUtils.getTrainAcceleration(vehicle, config),
			RailsimUtils.getTrainDeceleration(vehicle, config),
			RailsimUtils.getTrainDeceleration(vehicle, config)
		);
	}

	public void checkConsistency() {
		if (!Double.isFinite(maxVelocity) || maxVelocity <= 0)
			throw new IllegalArgumentException("Train of type " + id + " does not have a finite maximumVelocity.");

		if (!Double.isFinite(acceleration) || acceleration <= 0)
			throw new IllegalArgumentException("Train of type " + id + " does not have a finite and positive acceleration.");

		if (!Double.isFinite(deceleration) || deceleration <= 0)
			throw new IllegalArgumentException("Train of type " + id + " does not have a finite and positive deceleration.");

	}
}
