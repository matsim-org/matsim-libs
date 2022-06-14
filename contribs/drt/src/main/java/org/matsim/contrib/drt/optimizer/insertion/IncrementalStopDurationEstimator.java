package org.matsim.contrib.drt.optimizer.insertion;

import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

public interface IncrementalStopDurationEstimator {

	//if stopTask is null - it's a new (potential) stop
	double calcForPickup(DvrpVehicle vehicle, DrtStopTask stopTask, DrtRequest pickupRequest);

	//if stopTask is null - it's a new (potential) stop
	double calcForDropoff(DvrpVehicle vehicle, DrtStopTask stopTask, DrtRequest dropoffRequest);
}
