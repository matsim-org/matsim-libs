package org.matsim.contrib.drt.prebooking.unscheduler;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;

public interface RequestUnscheduler {
	void unscheduleRequest(double now, Id<DvrpVehicle> vehicleId, Id<Request> requestId);
}
