package org.matsim.contrib.drt.optimizer.insertion;

import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * Assumes a fixed stop duration for each pickup/dropoff.
 * @author nkuehnel / MOIA
 */
public class DefaultIncrementalStopDurationEstimator implements IncrementalStopDurationEstimator {

	private final double staticStopDuration;

	public DefaultIncrementalStopDurationEstimator(double staticStopDuration) {
		this.staticStopDuration = staticStopDuration;
	}

	@Override
	public double calcForPickup(DvrpVehicle vehicle, DrtStopTask stopTask, DrtRequest pickupRequest) {
		if(stopTask != null) {
			return 0;
		} else {
			return staticStopDuration;
		}
	}

	@Override
	public double calcForDropoff(DvrpVehicle vehicle, DrtStopTask stopTask, DrtRequest dropoffRequest) {
		if(stopTask != null) {
			return 0;
		} else {
			return staticStopDuration;
		}
	}
}
