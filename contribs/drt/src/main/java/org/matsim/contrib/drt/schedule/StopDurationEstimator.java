package org.matsim.contrib.drt.schedule;

import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Collection;

public interface StopDurationEstimator {

	double calcDuration(DvrpVehicle vehicle, Collection<AcceptedDrtRequest> dropoffRequests,
						Collection<AcceptedDrtRequest> pickupRequests);
}
