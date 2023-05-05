package ch.sbb.matsim.contrib.railsim.qsimengine;


import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import org.matsim.vehicles.VehicleType;

/**
 * Non-mutable static information for a single train.
 */
record TrainInfo(
	double length,
	double maxVelocity,
	double acceleration,
	double deceleration,
	double maxDeceleration
	) {

	public TrainInfo(VehicleType vehicle, RailsimConfigGroup config) {
		// TODO:
		this(
			vehicle.getLength(),
			vehicle.getMaximumVelocity(),
			RailsimUtils.getTrainAcceleration(vehicle, config),
			RailsimUtils.getTrainDeceleration(vehicle, config),
			RailsimUtils.getTrainDeceleration(vehicle, config)
		);
	}

}
